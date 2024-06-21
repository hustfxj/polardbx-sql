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
public class SqlAlterTableDropPartition extends SqlAlterSpecification {

    private static final SqlOperator OPERATOR = new SqlSpecialOperator("DROP PARTITION", SqlKind.DROP_PARTITION);

    protected final List<SqlNode> partitions;

    private boolean isSubPartition;
    /**
     * partitionName is only for drop subpartition
     */
    private SqlNode partitionName;

    protected SqlNode parent;

    public SqlAlterTableDropPartition(SqlParserPos pos, List<SqlNode> partitions, boolean isSubPartition,
                                      SqlNode partitionName) {
        super(pos);
        this.partitions = partitions;
        this.isSubPartition = isSubPartition;
        this.partitionName = partitionName;
    }

    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }

    @Override
    public List<SqlNode> getOperandList() {
        return partitions;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        final SqlWriter.Frame frame = writer.startList(SqlWriter.FrameTypeEnum.SELECT, "DROP", "");

        writer.keyword(isSubPartition ? "SUBPARTITION" : "PARTITION");

        int i = 0;
        for (SqlNode sqlNode : partitions) {
            sqlNode.unparse(writer, leftPrec, rightPrec);
            i++;
            if (i < partitions.size()) {
                writer.sep(",");
            }
        }

        writer.endList(frame);
    }

    public List<SqlNode> getPartitionNames() {
        return partitions;
    }

    public boolean isSubPartition() {
        return isSubPartition;
    }

    public SqlNode getPartitionName() {
        return partitionName;
    }
}

