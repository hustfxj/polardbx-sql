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

import java.util.LinkedList;
import java.util.List;

/**
 * @author chengjin
 * @since 2023/7/21 14:29
 **/
public class SqlFlushLogs extends SqlDal {

    private static final SqlSpecialOperator OPERATOR = new SqlFlushLogsStatementOperator();
    // group name of cdc
    private SqlNode with;

    public SqlFlushLogs(SqlParserPos pos) {
        super(pos);
    }

    public SqlFlushLogs(SqlParserPos pos, SqlNode with) {
        super(pos);
        this.with = with;
    }


    public SqlNode getWith() {
        return with;
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public SqlKind getKind() {
        return SqlKind.FLUSH_LOGS;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        final SqlWriter.Frame selectFrame = writer.startList(SqlWriter.FrameTypeEnum.SELECT);
        writer.sep("FLUSH LOGS");
        if (with != null) {
            writer.keyword("WITH");
            writer.print(with.toString());
        }

        writer.endList(selectFrame);
    }

    public static class SqlFlushLogsStatementOperator extends SqlSpecialOperator {
        public SqlFlushLogsStatementOperator() {
            super("SQL_FLUSH_LOGS", SqlKind.FLUSH_LOGS);
        }

        @Override
        public RelDataType deriveType(SqlValidator validator, SqlValidatorScope scope, SqlCall call) {
            final RelDataTypeFactory typeFactory = validator.getTypeFactory();
            List<RelDataTypeFieldImpl> columns = new LinkedList<>();
            columns.add(new RelDataTypeFieldImpl("RESULT", 0, typeFactory.createSqlType(SqlTypeName.INTEGER)));

            return typeFactory.createStructType(columns);
        }
    }
}
