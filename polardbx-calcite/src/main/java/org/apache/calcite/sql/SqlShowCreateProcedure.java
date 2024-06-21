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

import com.google.common.collect.ImmutableList;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import java.util.LinkedList;
import java.util.List;

public class SqlShowCreateProcedure extends SqlShow {
    private static final SqlSpecialOperator OPERATOR = new SqlShowCreateProcedure.SqlShowCreateProcedureOperator();

    private SqlNode procedureName;

    public SqlShowCreateProcedure(SqlParserPos pos, List<SqlSpecialIdentifier> specialIdentifiers,
                                  List<SqlNode> operands, SqlNode procedureName) {
        super(pos, specialIdentifiers, operands, null, null, null, null,
            specialIdentifiers.size() + operands.size() - 1);
        this.procedureName = procedureName;
    }

    public static SqlShowCreateProcedure create(SqlParserPos pos, SqlNode procedureName) {
        return new SqlShowCreateProcedure(pos,
            ImmutableList.of(SqlSpecialIdentifier.CREATE, SqlSpecialIdentifier.PROCEDURE),
            ImmutableList.of(procedureName), procedureName);
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public SqlKind getShowKind() {
        return SqlKind.SHOW_CREATE_PROCEDURE;
    }

    public SqlNode getProcedureName() {
        return procedureName;
    }

    public static class SqlShowCreateProcedureOperator extends SqlSpecialOperator {

        public SqlShowCreateProcedureOperator() {
            super("SHOW_CREATE_PROCEDURE", SqlKind.SHOW_CREATE_PROCEDURE);
        }

        @Override
        public RelDataType deriveType(SqlValidator validator, SqlValidatorScope scope, SqlCall call) {
            final RelDataTypeFactory typeFactory = validator.getTypeFactory();
            List<RelDataTypeFieldImpl> columns = new LinkedList<>();
            columns.add(new RelDataTypeFieldImpl("Procedure", 0, typeFactory.createSqlType(SqlTypeName.VARCHAR)));
            columns.add(
                new RelDataTypeFieldImpl("Create Procedure", 1, typeFactory.createSqlType(SqlTypeName.VARCHAR)));

            return typeFactory.createStructType(columns);
        }
    }
}
