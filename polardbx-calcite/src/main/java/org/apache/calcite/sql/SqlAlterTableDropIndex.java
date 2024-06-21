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

import org.apache.calcite.sql.parser.SqlParserPos;

import java.util.Arrays;
import java.util.List;

/**
 * @author chenmo.cm
 */
public class SqlAlterTableDropIndex extends SqlAlterSpecification {
    private static final SqlOperator OPERATOR =
        new SqlSpecialOperator("DROP INDEX", SqlKind.DROP_INDEX);

    /**
     * Creates a SqlAlterTableDropIndex.
     */
    public SqlAlterTableDropIndex(SqlIdentifier tableName, SqlIdentifier indexName, SqlIdentifier originIndexName,
                                  String sql, SqlParserPos pos) {
        super(pos);
        this.tableName = tableName;
        this.originTableName = tableName;
        this.originIndexName = originIndexName;
        this.indexName = indexName;
        this.sourceSql = sql;
    }

    final private SqlIdentifier tableName;
    final private SqlIdentifier originTableName;
    final private SqlIdentifier indexName;
    final private SqlIdentifier originIndexName;
    final private String sourceSql;

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
        return Arrays.asList(tableName, indexName);
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        final SqlWriter.Frame frame = writer.startList(SqlWriter.FrameTypeEnum.SELECT, "DROP INDEX", "");

        indexName.unparse(writer, leftPrec, rightPrec);

        writer.endList(frame);
    }

    public SqlNode getTableName() {
        return tableName;
    }

    public SqlIdentifier getIndexName() {
        return indexName;
    }

    public SqlIdentifier getOriginIndexName() {
        return originIndexName;
    }

    public String getSourceSql() {
        return sourceSql;
    }

    public SqlIdentifier getOriginTableName() {
        return originTableName;
    }

    @Override
    public boolean supportFileStorage() {
        return true;
    }

    public SqlAlterTableDropIndex replaceIndexName(SqlIdentifier newIndexName) {
        return new SqlAlterTableDropIndex(tableName, newIndexName, originIndexName, sourceSql, pos);
    }
}
