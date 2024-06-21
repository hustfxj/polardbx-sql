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
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;

/**
 * @author pangzhaoxing
 */
public class SqlRevokeSecurityLabel extends SqlDal{

    private static final SqlOperator OPERATOR = new SqlRevokeSecurityLabel.SqlRevokeSecurityLabelOperator();

    private SqlIdentifier accessType;

    private SqlIdentifier policyName;

    private SqlUserName userName;

    protected SqlRevokeSecurityLabel(SqlParserPos pos) {
        super(pos);
    }

    public SqlRevokeSecurityLabel(SqlParserPos pos, SqlIdentifier accessType, SqlIdentifier policyName,
                                  SqlUserName userName) {
        super(pos);
        this.accessType = accessType;
        this.policyName = policyName;
        this.userName = userName;
    }

    public SqlIdentifier getAccessType() {
        return accessType;
    }

    public SqlIdentifier getPolicyName() {
        return policyName;
    }

    public SqlUserName getUserName() {
        return userName;
    }

    @Override
    public SqlKind getKind() {
        return SqlKind.REVOKE_SECURITY_LABEL;
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        writer.keyword("REVOKE SECURITY LABEL");
        accessType.unparse(writer, leftPrec, rightPrec);
        writer.keyword("ACCESS");
        writer.keyword("ON");
        policyName.unparse(writer, leftPrec, rightPrec);
        writer.keyword("FROM");
        writer.keyword("USER");
        userName.unparse(writer, leftPrec, rightPrec);
    }

    public static class SqlRevokeSecurityLabelOperator extends SqlSpecialOperator {

        public SqlRevokeSecurityLabelOperator() {
            super("REVOKE_SECURITY_LABEL", SqlKind.REVOKE_SECURITY_LABEL);
        }

        @Override
        public RelDataType deriveType(final SqlValidator validator, final SqlValidatorScope scope, final SqlCall call) {
            RelDataTypeFactory typeFactory = validator.getTypeFactory();
            RelDataType columnType = typeFactory.createSqlType(SqlTypeName.CHAR);

            return typeFactory.createStructType(
                ImmutableList.of((RelDataTypeField) new RelDataTypeFieldImpl("REVOKE_SECURITY_LABEL",
                    0,
                    columnType)));
        }
    }
}
