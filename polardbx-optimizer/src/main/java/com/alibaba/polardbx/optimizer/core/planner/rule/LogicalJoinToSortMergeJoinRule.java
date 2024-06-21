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

package com.alibaba.polardbx.optimizer.core.planner.rule;

import com.alibaba.polardbx.common.properties.ConnectionParams;
import com.alibaba.polardbx.optimizer.PlannerContext;
import com.alibaba.polardbx.optimizer.core.DrdsConvention;
import com.alibaba.polardbx.optimizer.core.planner.rule.util.CBOUtil;
import com.alibaba.polardbx.optimizer.core.rel.SortMergeJoin;
import com.alibaba.polardbx.optimizer.hint.operator.HintType;
import com.alibaba.polardbx.optimizer.hint.util.CheckJoinHint;
import com.google.common.collect.ImmutableList;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rel.logical.LogicalSort;
import org.apache.calcite.rex.RexNode;

import java.util.ArrayList;
import java.util.List;

public class LogicalJoinToSortMergeJoinRule extends ConverterRule {

    public static final LogicalJoinToSortMergeJoinRule INSTANCE = new LogicalJoinToSortMergeJoinRule("INSTANCE");

    LogicalJoinToSortMergeJoinRule(String desc) {
        super(LogicalJoin.class, Convention.NONE, DrdsConvention.INSTANCE,
            "LogicalJoinToSortMergeJoinRule:" + desc);
    }

    @Override
    public Convention getOutConvention() {
        return DrdsConvention.INSTANCE;
    }

    private boolean enable(PlannerContext plannerContext) {
        return plannerContext.getParamManager().getBoolean(ConnectionParams.ENABLE_SORT_MERGE_JOIN);
    }

    @Override
    public boolean matches(RelOptRuleCall call) {
        if (!RelOptUtil.NO_COLLATION_AND_DISTRIBUTION.test(call.rel(0))) {
            return false;
        }
        return enable(PlannerContext.getPlannerContext(call));
    }

    @Override
    public RelNode convert(RelNode rel) {
        final LogicalJoin join = (LogicalJoin) rel;

        final RelNode left = join.getLeft();
        final RelNode right = join.getRight();

        RexNode newCondition =
            JoinConditionSimplifyRule.simplifyCondition(join.getCondition(), join.getCluster().getRexBuilder());

        List<Integer> leftColumns = new ArrayList<>();
        List<Integer> rightColumns = new ArrayList<>();
        CBOUtil.RexNodeHolder otherConditionHolder = new CBOUtil.RexNodeHolder();

        int leftFieldCount = join.getLeft().getRowType().getFieldCount();

        if (!CBOUtil.checkSortMergeCondition(join, newCondition, leftFieldCount, leftColumns, rightColumns,
            otherConditionHolder)) {
            return null;
        }

        RelCollation leftRelCollation = CBOUtil.createRelCollation(leftColumns);
        RelCollation rightRelCollation = CBOUtil.createRelCollation(rightColumns);

        RelNode newLeft;
        RelNode newRight;

        RelTraitSet inputTraitSet = rel.getCluster().getPlanner().emptyTraitSet().replace(DrdsConvention.INSTANCE);

        newLeft = convert(left, inputTraitSet);
        newRight = convert(right, inputTraitSet);

        LogicalSort leftSort =
            LogicalSort.create(rel.getCluster().getPlanner().emptyTraitSet().replace(leftRelCollation), newLeft,
                leftRelCollation, null, null);
        newLeft = convert(leftSort, leftSort.getTraitSet().replace(DrdsConvention.INSTANCE));

        LogicalSort rightSort =
            LogicalSort.create(rel.getCluster().getPlanner().emptyTraitSet().replace(rightRelCollation), newRight,
                rightRelCollation, null, null);
        newRight = convert(rightSort, rightSort.getTraitSet().replace(DrdsConvention.INSTANCE));

        SortMergeJoin sortMergeJoin = SortMergeJoin.create(
            join.getTraitSet().replace(DrdsConvention.INSTANCE).replace(leftRelCollation),
            newLeft,
            newRight,
            leftRelCollation,
            newCondition,
            join.getVariablesSet(),
            join.getJoinType(),
            join.isSemiJoinDone(),
            ImmutableList.copyOf(join.getSystemFieldList()),
            join.getHints(),
            leftColumns,
            rightColumns,
            otherConditionHolder.getRexNode());
        RelOptCost fixedCost = CheckJoinHint.check(join, HintType.CMD_SORT_MERGE_JOIN);
        if (fixedCost != null) {
            sortMergeJoin.setFixedCost(fixedCost);
        }
        return sortMergeJoin;
    }
}


