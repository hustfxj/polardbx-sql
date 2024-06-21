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

package org.apache.calcite.sql;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author wumu
 */
public class SqlSkipRebalanceSubjob extends SqlDal implements DdlJobUtility {

    private static final SqlSpecialOperator OPERATOR = new SqlSkipRebalanceSubJobOperator();
    private boolean all;
    private List<Long> jobIds = new ArrayList<>();

    public SqlSkipRebalanceSubjob(SqlParserPos pos, boolean all) {
        super(pos);
        this.operands = new ArrayList<>(0);
        this.all = all;
    }

    @Override
    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    @Override
    public List<Long> getJobIds() {
        return jobIds;
    }

    public void setJobIds(List<Long> jobIds) {
        this.jobIds.clear();
        this.jobIds.addAll(jobIds);
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        final SqlWriter.Frame selectFrame = writer.startList(SqlWriter.FrameTypeEnum.SELECT);
        writer.sep("SKIP REBALANCE SUBJOB");
        if (isAll()) {
            writer.print("ALL");
        } else {
            writer.print(StringUtils.join(jobIds, ","));
        }
        writer.endList(selectFrame);
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public SqlKind getKind() {
        return SqlKind.SKIP_REBALANCE_SUBJOB;
    }

    public static class SqlSkipRebalanceSubJobOperator extends SqlSpecialOperator {

        public SqlSkipRebalanceSubJobOperator() {
            super("SKIP_REBALANCE_SUBJOB", SqlKind.SKIP_REBALANCE_SUBJOB);
        }

        @Override
        public RelDataType deriveType(SqlValidator validator, SqlValidatorScope scope, SqlCall call) {
            final RelDataTypeFactory typeFactory = validator.getTypeFactory();
            List<RelDataTypeFieldImpl> columns = new LinkedList<>();

            columns.add(new RelDataTypeFieldImpl("JOB_ID", 0, typeFactory.createSqlType(SqlTypeName.INTEGER)));
            columns.add(new RelDataTypeFieldImpl("STATUS", 1, typeFactory.createSqlType(SqlTypeName.VARCHAR)));

            return typeFactory.createStructType(columns);
        }
    }
}
