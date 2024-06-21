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

import com.alibaba.polardbx.optimizer.config.table.TableMeta;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.utils.RelUtils;
import com.google.common.collect.ImmutableList;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlIndexHint;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlShuttle;
import org.apache.calcite.sql.validate.SqlValidatorImpl;

import java.util.List;

/**
 * Note, only consider case when only one single table in select sql.
 * That is, we do not consider such case: select count(0) from a join b.
 *
 * @author yaozhili
 */
public class ForceIndexSingleVisitor extends SqlShuttle {

    /**
     * Validator is used to find AGG in AST.
     */
    private final SqlValidatorImpl validator;

    /**
     * Whether add FORCE INDEX PRIMARY for this AST.
     * If true, a new AST with FORCE INDEX PRIMARY will be generated.
     */
    private final boolean addForcePrimary;

    /**
     * Whether add FORCE INDEX PRIMARY when FILTER is given.
     */
    private final ExecutionContext ec;

    /**
     * Whether we can optimize the AST by adding FORCE INDEX PRIMARY for count(*) under TSO.
     * If true, this AST can be optimized by adding FORCE INDEX PRIMARY, (but we do not optimize it.)
     */
    private boolean canOptByForcePrimary = false;

    private TableMeta tableMeta = null;

    public ForceIndexSingleVisitor(SqlValidatorImpl validator, boolean addForcePrimary, ExecutionContext ec) {
        this.validator = validator;
        this.addForcePrimary = addForcePrimary;
        this.ec = ec;
    }

    public boolean isCanOptByForcePrimary() {
        return canOptByForcePrimary;
    }

    @Override
    public SqlNode visit(SqlCall call) {
        final SqlKind kind = call.getKind();
        // Only check for select with agg.
        if (kind == SqlKind.SELECT) {
            final SqlSelect select = (SqlSelect) SqlNode.clone(call);

            if (null != select.getGroup()) {
                // Whether optimize when GROUP BY is given.
                if (null == this.ec || !this.ec.enableForcePrimaryForGroupBy()) {
                    return select;
                }
            }

            if (null != select.getWhere() || null != select.getHaving()) {
                // Whether optimize when WHERE (filter) is given.
                if (null == this.ec || !this.ec.enableForcePrimaryForFilter()) {
                    return select;
                }
            }

            // Only optimize for FROM.
            SqlNode from = select.getFrom();
            if (null == from) {
                return select;
            }

            final SqlKind fromKind = from.getKind();
            if (fromKind == SqlKind.IDENTIFIER) {
                // Case 1: FROM single-table.
                tryToAddForceIndex(select, from, from);
            } else if (fromKind == SqlKind.AS) {
                // Case 2: FROM Something AS alias.
                SqlNode leftNode = ((SqlBasicCall) from).getOperandList().get(0);
                if (leftNode.getKind() == SqlKind.SELECT) {
                    // Case 2.1: FROM Sub-query AS alias.
                    leftNode = visit((SqlSelect) leftNode);
                    ((SqlBasicCall) from).setOperand(0, leftNode);
                } else if (leftNode.getKind() == SqlKind.AS_OF) {
                    // Case 2.2: FROM single-table AS OF TIMESTAMP xxx AS alias.
                    leftNode = ((SqlBasicCall) leftNode).getOperandList().get(0);
                    SqlNode rightNode = ((SqlBasicCall) from).getOperandList().get(1);
                    tryToAddForceIndex(select, leftNode, rightNode);
                } else if (leftNode.getKind() == SqlKind.IDENTIFIER) {
                    // Case 2.3: FROM single-table AS alias.
                    SqlNode rightNode = ((SqlBasicCall) from).getOperandList().get(1);
                    tryToAddForceIndex(select, leftNode, rightNode);
                }
            } else if (fromKind == SqlKind.SELECT && from instanceof SqlSelect) {
                // Case 3: FROM Sub-query.
                select.setFrom(visit((SqlSelect) from));
            } else if (fromKind == SqlKind.AS_OF) {
                // Case 4: FROM single-table AS OF TIMESTAMP xxx.
                SqlNode leftNode = ((SqlBasicCall) from).getOperandList().get(0);
                if (leftNode.getKind() == SqlKind.IDENTIFIER) {
                    // Index node should be added to left node.
                    tryToAddForceIndex(select, leftNode, leftNode);
                }
            }

            return select;
        }
        return call;
    }

    private void tryToAddForceIndex(SqlSelect select, SqlNode tableNode, SqlNode addIndexNode) {
        if (!(tableNode instanceof SqlIdentifier) || !(addIndexNode instanceof SqlIdentifier)) {
            return;
        }
        // Try to get table meta, may be null.
        this.tableMeta = RelUtils.getTableMeta((SqlIdentifier) tableNode, this.ec);
        if (null == tableMeta || tableMeta.getSecondaryIndexes().isEmpty() || !tableMeta.isHasPrimaryKey()) {
            // No secondary/primary indexes, no need to add FORCE INDEX PRIMARY.
            return;
        }
        if (findTargetAgg(select) && addIndexNode.getKind() == SqlKind.IDENTIFIER) {
            this.canOptByForcePrimary = true;
            if (this.addForcePrimary) {
                addForceIndex(addIndexNode);
            }
        }
    }

    private boolean findTargetAgg(SqlSelect select) {
        if (!validator.isAggregate(select)) {
            return false;
        }
        // Find count/sum/avg/min/max in select list.
        for (SqlNode selectItem : select.getSelectList()) {
            final FindAggVisitor findAggVisitor = new FindAggVisitor();
            selectItem.accept(findAggVisitor);
            if (findAggVisitor.isFound()) {
                return true;
            }
        }
        return false;
    }

    private void addForceIndex(SqlNode table) {
        // If we should optimize this sql node,
        // and originally it dose not have any force index hint,
        // we add a FORCE INDEX(PRIMARY) to it.
        if (table instanceof SqlIdentifier
            && ((SqlIdentifier) table).indexNode instanceof SqlNodeList
            && 0 == ((SqlNodeList) ((SqlIdentifier) table).indexNode).size()) {
            final SqlCharStringLiteral indexKind = SqlLiteral.createCharString("FORCE INDEX", SqlParserPos.ZERO);
            final SqlNodeList indexList = new SqlNodeList(
                ImmutableList.of(SqlLiteral.createCharString("PRIMARY", SqlParserPos.ZERO)),
                SqlParserPos.ZERO);
            final SqlNode indexHint = new SqlIndexHint(indexKind, null, indexList, SqlParserPos.ZERO);
            ((SqlIdentifier) table).indexNode = new SqlNodeList(ImmutableList.of(indexHint), SqlParserPos.ZERO);
        }
    }

    private class FindAggVisitor extends SqlShuttle {

        private boolean found = false;

        @Override
        public SqlNode visit(SqlCall call) {
            // Early terminated.
            if (found) {
                return call;
            }

            final SqlKind kind = call.getKind();
            List<SqlNode> operandList = call.getOperandList();
            if (kind == SqlKind.AS && !operandList.isEmpty() && operandList.get(0) instanceof SqlCall) {
                // For case "count(0) AS count", we should visit "count(0)", i.e., the left operand.
                visit((SqlCall) call.getOperandList().get(0));
            } else if (kind == SqlKind.COUNT || kind == SqlKind.SUM || SqlKind.AVG_AGG_FUNCTIONS.contains(kind)) {
                found = true;
            } else if (SqlKind.MIN_MAX_AGG.contains(kind) && operandList.get(0) instanceof SqlIdentifier) {
                // For case "min/max(col)".
                final String columnName = ((SqlIdentifier) operandList.get(0)).getLastName();
                if (RelUtils.canOptMinMax(tableMeta, columnName)) {
                    found = true;
                }
            }
            return call;
        }

        public boolean isFound() {
            return found;
        }
    }
}
