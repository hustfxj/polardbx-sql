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

package com.alibaba.polardbx.optimizer.config.meta;

import com.alibaba.polardbx.optimizer.core.rel.BaseTableOperation;
import com.alibaba.polardbx.optimizer.core.rel.DirectTableOperation;
import com.alibaba.polardbx.optimizer.core.rel.LogicalView;
import com.alibaba.polardbx.optimizer.core.rel.MysqlTableScan;
import com.google.common.collect.ImmutableSet;
import com.alibaba.polardbx.optimizer.view.ViewPlan;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.hep.HepRelVertex;
import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.core.Correlate;
import org.apache.calcite.rel.core.Exchange;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.SetOp;
import org.apache.calcite.rel.core.Sort;
import org.apache.calcite.rel.core.TableFunctionScan;
import org.apache.calcite.rel.core.TableLookup;
import org.apache.calcite.rel.metadata.BuiltInMetadata;
import org.apache.calcite.rel.metadata.MetadataDef;
import org.apache.calcite.rel.metadata.MetadataHandler;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelColumnMapping;
import org.apache.calcite.rel.metadata.RelColumnOrigin;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexSubQuery;
import org.apache.calcite.rex.RexVisitor;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.util.BuiltInMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author chenmo.cm
 */
public class DrdsRelMdColumnOriginNames implements MetadataHandler<BuiltInMetadata.ColumnOriginName> {

    public static final RelMetadataProvider SOURCE =
        ReflectiveRelMetadataProvider.reflectiveSource(BuiltInMethod.COLUMN_ORIGIN_NAME.method,
            new DrdsRelMdColumnOriginNames());

    @Override
    public MetadataDef<BuiltInMetadata.ColumnOriginName> getDef() {
        return BuiltInMetadata.ColumnOriginName.DEF;
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(Aggregate rel, RelMetadataQuery mq) {
        final List<Set<RelColumnOrigin>> origins = mq.getColumnOriginNames(rel.getInput());

        if (null == origins) {
            return null;
        }

        List<Set<RelColumnOrigin>> result = new ArrayList<>();
        for (int iOutputColumn = 0; iOutputColumn < rel.getRowType().getFieldCount(); iOutputColumn++) {
            if (iOutputColumn < rel.getGroupCount()) {
                // Group columns pass through directly.
                result.add(origins.get(iOutputColumn));
                continue;
            }

            if (rel.indicator) {
                if (iOutputColumn < rel.getGroupCount() + rel.getIndicatorCount()) {
                    // The indicator column is originated here.
                    result.add(ImmutableSet.of());
                    continue;
                }
            }

            // Aggregate columns are derived from input columns
            AggregateCall call = rel.getAggCallList()
                .get(iOutputColumn - rel.getGroupCount() - rel.getIndicatorCount());

            final Set<RelColumnOrigin> set = new HashSet<>();
            for (Integer iInput : call.getArgList()) {
                Set<RelColumnOrigin> inputSet = origins.get(iInput);
                inputSet = createDerivedColumnOrigins(inputSet);
                if (inputSet != null) {
                    set.addAll(inputSet);
                }
            }
            result.add(set);
        }
        return result;
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(Join rel, RelMetadataQuery mq) {
        final int nLeftColumns = rel.getLeft().getRowType().getFieldList().size();

        final List<Set<RelColumnOrigin>> leftOrigins = mq.getColumnOriginNames(rel.getLeft());
        final List<Set<RelColumnOrigin>> rightOrigins = mq.getColumnOriginNames(rel.getRight());

        if (null == leftOrigins || null == rightOrigins) {
            return null;
        }

        List<Set<RelColumnOrigin>> result = new ArrayList<>();
        for (int ci = 0; ci < rel.getRowType().getFieldCount(); ci++) {
            Set<RelColumnOrigin> set;
            if (ci < nLeftColumns) {
                set = leftOrigins.get(ci);
                // null generation does not change column name
            } else {
                set = rightOrigins.get(ci - nLeftColumns);
                // null generation does not change column name
            }

            result.add(set);
        }
        return result;
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(Correlate rel, RelMetadataQuery mq) {
        final int nLeftColumns = rel.getLeft().getRowType().getFieldList().size();

        final List<Set<RelColumnOrigin>> leftOrigins = mq.getColumnOriginNames(rel.getLeft());
        final List<Set<RelColumnOrigin>> rightOrigins = mq.getColumnOriginNames(rel.getRight());

        if (null == leftOrigins || null == rightOrigins) {
            return null;
        }

        List<Set<RelColumnOrigin>> result = new ArrayList<>();
        for (int ci = 0; ci < rel.getRowType().getFieldCount(); ci++) {
            Set<RelColumnOrigin> set;
            if (ci < nLeftColumns) {
                set = leftOrigins.get(ci);
                // null generation does not change column name
            } else {
                if (rel.getJoinType().returnsJustFirstInput()) {
                    set = ImmutableSet.of();
                } else {
                    set = rightOrigins.get(ci - nLeftColumns);
                }
                // null generation does not change column name
            }

            result.add(set);
        }
        return result;
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(SetOp rel, RelMetadataQuery mq) {
        final List<Set<RelColumnOrigin>> set = new ArrayList<>();
        for (RelNode input : rel.getInputs()) {
            List<Set<RelColumnOrigin>> inputSet = mq.getColumnOriginNames(input);
            if (inputSet == null) {
                return null;
            }

            for (int ci = 0; ci < inputSet.size(); ci++) {
                if (set.size() <= ci) {
                    set.add(new HashSet<>());
                }

                set.get(ci).addAll(inputSet.get(ci));
            }
        }
        return set;
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(Project rel, final RelMetadataQuery mq) {
        final RelNode input = rel.getInput();

        final List<Set<RelColumnOrigin>> origins = mq.getColumnOriginNames(input);

        if (null == origins) {
            return null;
        }

        final List<Set<RelColumnOrigin>> result = new ArrayList<>();
        for (RexNode rexNode : rel.getProjects()) {
            Set<RelColumnOrigin> columnOrigins = null;
            if (rexNode instanceof RexInputRef) {
                // Direct reference: no derivation added.
                final RexInputRef inputRef = (RexInputRef) rexNode;
                columnOrigins = origins.get(inputRef.getIndex());
            } else if (rexNode instanceof RexDynamicParam) {
                columnOrigins = new HashSet<>();
                if (((RexDynamicParam) rexNode).getRel() != null) {
                    List<Set<RelColumnOrigin>> subOrigin =
                        mq.getColumnOriginNames(((RexDynamicParam) rexNode).getRel());
                    if (!subOrigin.isEmpty()) {
                        columnOrigins.addAll(subOrigin.get(0));
                    }
                }
            } else {
                // Anything else is a derivation, possibly from multiple
                // columns.
                final Set<RelColumnOrigin> set = new HashSet<>();
                final RexVisitor<Void> visitor = new RexVisitorImpl<Void>(true) {

                    @Override
                    public Void visitInputRef(RexInputRef inputRef) {
                        set.addAll(origins.get(inputRef.getIndex()));
                        return null;
                    }
                };
                rexNode.accept(visitor);

                columnOrigins = createDerivedColumnOrigins(set);
            }

            result.add(columnOrigins);
        }

        return result;
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(TableLookup rel, final RelMetadataQuery mq) {
        return mq.getColumnOriginNames(rel.getProject());
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(Filter rel, RelMetadataQuery mq) {
        return mq.getColumnOriginNames(rel.getInput());
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(Sort rel, RelMetadataQuery mq) {
        return mq.getColumnOriginNames(rel.getInput());
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(Exchange rel, RelMetadataQuery mq) {
        return mq.getColumnOriginNames(rel.getInput());
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(ViewPlan rel, RelMetadataQuery mq) {
        return mq.getColumnOriginNames(rel.getPlan());
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(MysqlTableScan rel, RelMetadataQuery mq) {
        return mq.getColumnOriginNames(rel.getNodeForMetaQuery());
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(TableFunctionScan rel, RelMetadataQuery mq) {
        final Set<RelColumnMapping> mappings = rel.getColumnMappings();
        if (mappings == null) {
            if (rel.getInputs().size() > 0) {
                // This is a non-leaf transformation: say we don't
                // know about origins, because there are probably
                // columns below.
                return null;
            } else {
                // This is a leaf transformation: say there are for sure no
                // column origins.
                return emptyColumnOrigin(rel);
            }
        }

        final List<Set<RelColumnOrigin>> result = new ArrayList<>();
        for (RelColumnMapping mapping : mappings) {
            final RelNode input = rel.getInputs().get(mapping.iInputRel);
            final int column = mapping.iInputColumn;

            final List<Set<RelColumnOrigin>> origins = mq.getColumnOriginNames(input);
            if (origins == null || origins.size() <= column) {
                return null;
            }

            Set<RelColumnOrigin> origin = origins.get(column);
            if (mapping.derived) {
                origin = createDerivedColumnOrigins(origin);
            }
            result.add(origin);
        }
        return result;
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(LogicalView rel, RelMetadataQuery mq) {
        return mq.getColumnOriginNames(rel.getPushedRelNode());
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(BaseTableOperation rel, RelMetadataQuery mq) {
        return mq.getColumnOriginNames(rel.getParent());
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(DirectTableOperation rel, RelMetadataQuery mq) {
        List<Set<RelColumnOrigin>> pushedOrigins = mq.getColumnOriginNames(rel.getParent());
        try {
            List<String> pushedColNames = rel.getParent().getRowType().getFieldNames();
            Map<String, Integer> colNameIndex = new HashMap<>(pushedColNames.size());
            for (int i = 0; i < pushedColNames.size(); i++) {
                colNameIndex.put(pushedColNames.get(i).toLowerCase(), i);
            }
            List<String> colNames = rel.getRowType().getFieldNames();
            List<Set<RelColumnOrigin>> origins = new ArrayList<>(pushedOrigins.size());
            for (String colName : colNames) {
                origins.add(pushedOrigins.get(colNameIndex.get(colName.toLowerCase())));
            }
            return origins;
        } catch (Throwable e) {
            return pushedOrigins;
        }
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(RelSubset rel, RelMetadataQuery mq) {
        return mq.getColumnOriginNames(rel.getOriginal());
    }

    public List<Set<RelColumnOrigin>> getColumnOriginNames(HepRelVertex rel, RelMetadataQuery mq) {
        return mq.getColumnOriginNames(rel.getCurrentRel());
    }

    // Catch-all rule when none of the others apply.
    public List<Set<RelColumnOrigin>> getColumnOriginNames(RelNode rel, RelMetadataQuery mq) {
        // NOTE jvs 28-Mar-2006: We may get this wrong for a physical table
        // expression which supports projections. In that case,
        // it's up to the plugin writer to override with the
        // correct information.

        if (rel.getInputs().size() > 0) {
            // No generic logic available for non-leaf rels.
            return null;
        }

        RelOptTable table = rel.getTable();
        if (table == null) {
            // Somebody is making column values up out of thin air, like a
            // VALUES clause, so we return empty set for each column in row
            // type.
            return emptyColumnOrigin(rel);

        }

        // Detect the case where a physical table expression is performing
        // projection, and say we don't know instead of making any assumptions.
        // (Theoretically we could try to map the projection using column
        // names.) This detection assumes the table expression doesn't handle
        // rename as well.
        if (table.getRowType() != rel.getRowType()) {
            return null;
        }

        return table.getRowType()
            .getFieldList()
            .stream()
            .map(field -> ImmutableSet.of(new RelColumnOrigin(table, field.getIndex(), false)))
            .collect(Collectors.toList());
    }

    public List<Set<RelColumnOrigin>> emptyColumnOrigin(RelNode rel) {
        return rel.getRowType()
            .getFieldList()
            .stream()
            .map(field -> ImmutableSet.<RelColumnOrigin>of())
            .collect(Collectors.toList());
    }

    private Set<RelColumnOrigin> createDerivedColumnOrigins(Set<RelColumnOrigin> inputSet) {
        if (inputSet == null) {
            return null;
        }
        final Set<RelColumnOrigin> set = new HashSet<>();
        for (RelColumnOrigin rco : inputSet) {
            RelColumnOrigin derived = new RelColumnOrigin(rco.getOriginTable(), rco.getOriginColumnOrdinal(), true);
            set.add(derived);
        }
        return set;
    }
}
