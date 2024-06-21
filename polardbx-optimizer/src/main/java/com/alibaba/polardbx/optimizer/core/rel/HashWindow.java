/*
 * Copyright [2013-2021], Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.polardbx.optimizer.core.rel;

import com.alibaba.polardbx.optimizer.config.meta.CostModelWeight;
import com.alibaba.polardbx.optimizer.core.DrdsConvention;
import com.alibaba.polardbx.optimizer.core.MppConvention;
import com.alibaba.polardbx.optimizer.memory.MemoryEstimator;
import org.apache.calcite.linq4j.Ord;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelInput;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttle;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.core.Window;
import org.apache.calcite.rel.externalize.RelDrdsWriter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.util.ImmutableBitSet;

import java.util.List;
import java.util.stream.Collectors;

import static com.alibaba.polardbx.optimizer.config.meta.CostModelWeight.CPU_START_UP_COST;

public class HashWindow extends Window {
    public HashWindow(RelOptCluster cluster,
                      RelTraitSet traitSet,
                      RelNode child,
                      List<RexLiteral> constants,
                      List<Window.Group> groups,
                      RelDataType rowType) {
        super(cluster, traitSet, child, constants, rowType, groups);
        assert traitSet.containsIfApplicable(DrdsConvention.INSTANCE)
            || traitSet.containsIfApplicable(MppConvention.INSTANCE);
    }

    public HashWindow(RelInput relInput) {
        super(relInput.getCluster(),
            relInput.getTraitSet(),
            relInput.getInput(),
            relInput.getExpressionList("constants").stream().map(rexNode -> {
                RexLiteral t1 = (RexLiteral) rexNode;
                return t1;
            }).collect(Collectors.toList()),
            relInput.getRowType("rowType"),
            relInput.getWindowGroups());
        this.traitSet = this.traitSet.replace(DrdsConvention.INSTANCE).replace(relInput.getPartitionWise());
    }

    public static HashWindow create(RelTraitSet traitSet, final RelNode input, List<RexLiteral> constants,
                                    List<Window.Group> groups,
                                    RelDataType rowType) {
        final RelOptCluster cluster = input.getCluster();
        HashWindow overWindow = new HashWindow(cluster, traitSet, input, constants, groups, rowType);
        return overWindow;
    }

    //~ Methods ----------------------------------------------------------------
    @Override
    public HashWindow copy(RelTraitSet traitSet, List<RelNode> inputs) {
        HashWindow overWindow = new HashWindow(getCluster(),
            traitSet,
            inputs.get(0),
            constants,
            groups,
            rowType);
        return overWindow;
    }

    @Override
    public RelNode accept(RelShuttle shuttle) {
        return shuttle.visit(this);
    }

    @Override
    public RelWriter explainTermsForDisplay(RelWriter pw) {
        pw.item(RelDrdsWriter.REL_NAME, "HashWindow");
        int inputFieldCount = getInput().getRowType().getFieldCount();
        for (Ord<RelDataTypeField> field : Ord.zip(getInput().getRowType().getFieldList())) {
            String fieldName = getRowType().getFieldList().get(field.i).getName();
            if (fieldName == null) {
                fieldName = "field#" + field.i;
            }

            pw.item(fieldName, field.e.getName());
        }
        for (Ord<Group> window : Ord.zip(groups)) {
            for (int i = 0; i < window.getValue().aggCalls.size(); i++) {
                RexWinAggCall rexWinAggCall = window.getValue().aggCalls.get(i);
                String fieldName = "f" + (i + inputFieldCount) + "w" + window.i + "$o" + i;
                pw.item(fieldName, "window#" + window.i + rexWinAggCall.toString());
            }
        }
        StringBuffer windowInfo = new StringBuffer();
        for (Ord<Group> window : Ord.zip(groups)) {
            windowInfo.append("window#" + window.i).append("=").append(window.e.toString()).append(",");
        }
        pw.item("Reference Windows", windowInfo.toString().substring(0, windowInfo.length() - 1));
        pw.itemIf("constants", constants.toString(), constants != null && constants.size() > 0);
        pw.itemIf("partition", traitSet.getPartitionWise(), !traitSet.getPartitionWise().isTop());
        return pw;
    }

    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner,
                                      RelMetadataQuery mq) {
        double rowCount = mq.getRowCount(this.input);
        if (Double.isInfinite(rowCount)) {
            return planner.getCostFactory().makeHugeCost();
        }

        final double hashAggWeight = CostModelWeight.INSTANCE.getHashAggWeight();

        // Notice: distinct row count may be null
        // TODO may be we can use row count of agg to replace this
        Double distinctRowCount = mq.getDistinctRowCount(this, groups.get(0).keys, null);
        final double memory =
            distinctRowCount != null ?
                MemoryEstimator.estimateKeySizeInHashTable(getRowType(), groups.get(0).keys) * distinctRowCount
                : MemoryEstimator.estimateKeySizeInHashTable(getRowType(), groups.get(0).keys) * rowCount;

        final double useAggSize =
            groups.get(0).aggCalls.stream().filter(x -> x.op.kind != SqlKind.__FIRST_VALUE
                && x.op.getKind() != SqlKind.FIRST_VALUE).count();
        final double cpu =
            CPU_START_UP_COST + rowCount * hashAggWeight * (groups.get(0).keys.cardinality() + useAggSize);

        return planner.getCostFactory().makeCost(rowCount, cpu, memory, 0, 0);
    }

    @Override
    public RelWriter explainTerms(RelWriter pw) {
        RelWriter relWriter = super.explainTerms(pw)
            .item("keys", groups.get(0).keys)
            .item("constants", constants)
            .item("rowType", rowType)
            .item("groups", groups)
            .itemIf("partitionWise", this.traitSet.getPartitionWise(), !this.traitSet.getPartitionWise().isTop());
        return relWriter;
    }
}
