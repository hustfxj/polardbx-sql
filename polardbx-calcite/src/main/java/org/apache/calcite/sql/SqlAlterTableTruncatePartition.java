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

import java.util.List;

/**
 * Created by luoyanxin.
 *
 * @author luoyanxin
 */
public class SqlAlterTableTruncatePartition extends SqlAlterSpecification {

    private static final SqlOperator OPERATOR =
        new SqlSpecialOperator("TRUNCATE PARTITION", SqlKind.TRUNCATE_PARTITION);

    protected final List<SqlNode> partitionNames;
    protected final boolean isSubPartition;

    public SqlAlterTableTruncatePartition(SqlParserPos pos, List<SqlNode> partitionNames, boolean isSubPartition) {
        super(pos);
        this.partitionNames = partitionNames;
        this.isSubPartition = isSubPartition;
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
        return partitionNames;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        final SqlWriter.Frame frame = writer.startList(SqlWriter.FrameTypeEnum.SELECT, "TRUNCATE", "");

        writer.keyword(isSubPartition ? "SUBPARTITION" : "PARTITION");

        int i = 0;
        for (SqlNode sqlNode : partitionNames) {
            sqlNode.unparse(writer, leftPrec, rightPrec);
            i++;
            if (i < partitionNames.size()) {
                writer.sep(",");
            }
        }

        writer.endList(frame);
    }

    public boolean isSubPartition() {
        return isSubPartition;
    }

    public List<SqlNode> getPartitionNames() {
        return partitionNames;
    }
}

