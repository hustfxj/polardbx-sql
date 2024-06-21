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
import com.alibaba.polardbx.optimizer.core.rel.PhysicalProject;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.logical.LogicalProject;

public class DrdsProjectConvertRule extends ConverterRule {
    public static final DrdsProjectConvertRule SMP_INSTANCE = new DrdsProjectConvertRule(DrdsConvention.INSTANCE);

    public static final DrdsProjectConvertRule COL_INSTANCE = new DrdsProjectConvertRule(CBOUtil.getColConvention());

    private final Convention outConvention;

    DrdsProjectConvertRule(Convention outConvention) {
        super(LogicalProject.class, RelOptUtil.PROJECT_PREDICATE, Convention.NONE,
            outConvention, RelFactories.LOGICAL_BUILDER,
            "DrdsProjectConvertRule");
        this.outConvention = outConvention;
    }

    @Override
    public Convention getOutConvention() {
        return outConvention;
    }

    @Override
    public RelNode convert(RelNode rel) {
        final LogicalProject project = (LogicalProject) rel;
        return new PhysicalProject(
            rel.getCluster(),
            project.getTraitSet().simplify().replace(outConvention),
            convert(project.getInput(), project.getInput().getTraitSet().simplify().replace(outConvention)),
            project.getProjects(),
            project.getRowType(),
            project.getOriginalRowType(),
            project.getVariablesSet()
        );
    }
}
