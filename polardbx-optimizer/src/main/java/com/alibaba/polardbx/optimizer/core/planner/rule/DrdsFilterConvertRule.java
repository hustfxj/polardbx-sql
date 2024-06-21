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

import com.alibaba.polardbx.optimizer.core.DrdsConvention;
import com.alibaba.polardbx.optimizer.core.planner.rule.util.CBOUtil;
import com.alibaba.polardbx.optimizer.core.rel.PhysicalFilter;
import com.google.common.collect.ImmutableSet;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.logical.LogicalFilter;

public class DrdsFilterConvertRule extends ConverterRule {
    public static final DrdsFilterConvertRule SMP_INSTANCE = new DrdsFilterConvertRule(DrdsConvention.INSTANCE);

    public static final DrdsFilterConvertRule COL_INSTANCE = new DrdsFilterConvertRule(CBOUtil.getColConvention());

    private final Convention outConvention;

    DrdsFilterConvertRule(Convention outConvention) {
        super(LogicalFilter.class, RelOptUtil.FILTER_PREDICATE, Convention.NONE,
            outConvention, RelFactories.LOGICAL_BUILDER,
            "DrdsFilterConvertRule");
        this.outConvention = outConvention;
    }

    @Override
    public Convention getOutConvention() {
        return outConvention;
    }

    @Override
    public RelNode convert(RelNode rel) {
        final LogicalFilter filter = (LogicalFilter) rel;
        return new PhysicalFilter(
            filter.getCluster(),
            filter.getTraitSet().simplify().replace(outConvention),
            convert(filter.getInput(),
                filter.getInput().getTraitSet().simplify()
                    .replace(outConvention)),
            filter.getCondition(),
            ImmutableSet.copyOf(filter.getVariablesSet())
        );
    }
}
