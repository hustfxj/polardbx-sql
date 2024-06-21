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

import com.alibaba.polardbx.common.ddl.foreignkey.ForeignKeyData;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.ImmutableNullableList;

import java.util.List;

/**
 * @author chenmo.cm
 */
public class SqlReferenceOption extends SqlCall {

    private static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator("COLUMN_REFERENCE_OPTION",
        SqlKind.COLUMN_REFERENCE_OPTION);

    private final OnType onType;
    private final ReferenceOptionType referenceOptionType;

    public SqlReferenceOption(SqlParserPos pos, OnType onType, ReferenceOptionType referenceOptionType) {
        super(pos);
        this.onType = onType;
        this.referenceOptionType = referenceOptionType;
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
        return ImmutableNullableList.of((SqlNode) SqlUtil.wrapSqlLiteralSymbol(onType),
            SqlUtil.wrapSqlLiteralSymbol(referenceOptionType));
    }

    public OnType getOnType() {
        return onType;
    }

    public ReferenceOptionType getReferenceOptionType() {
        return referenceOptionType;
    }

    public ForeignKeyData.ReferenceOptionType convertReferenceOptionType(ReferenceOptionType referenceOptionType) {
        switch (referenceOptionType) {
        case RESTRICT:
            return ForeignKeyData.ReferenceOptionType.RESTRICT;
        case CASCADE:
            return ForeignKeyData.ReferenceOptionType.CASCADE;
        case SET_NULL:
            return ForeignKeyData.ReferenceOptionType.SET_NULL;
        case SET_DEFAULT:
            return ForeignKeyData.ReferenceOptionType.SET_DEFAULT;
        case NO_ACTION:
            return ForeignKeyData.ReferenceOptionType.NO_ACTION;
        }
        return ForeignKeyData.ReferenceOptionType.NO_ACTION;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        if (null != onType) {
            switch (onType) {
            case ON_DELETE:
                writer.keyword("ON DELETE");
                break;
            case ON_UPDATE:
                writer.keyword("ON UPDATE");
                break;
            default:
                break;
            }

            if (null != referenceOptionType) {
                switch (referenceOptionType) {
                case RESTRICT:
                    writer.keyword("RESTRICT");
                    break;
                case CASCADE:
                    writer.keyword("CASCADE");
                    break;
                case SET_NULL:
                    writer.keyword("SET NULL");
                    break;
                case NO_ACTION:
                    writer.keyword("NO ACTION");
                    break;
                case SET_DEFAULT:
                    writer.keyword("SET DEFAULT");
                    break;
                default:
                    break;
                }
            }
        }
    }

    public static enum OnType {
        ON_DELETE, ON_UPDATE
    }

    public static enum ReferenceOptionType {
        RESTRICT, CASCADE, SET_NULL, SET_DEFAULT, NO_ACTION
    }
}
