/*
 * Copyright 1999-2017 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.polardbx.druid.sql.ast.statement;

import com.alibaba.polardbx.druid.sql.ast.SQLExpr;
import com.alibaba.polardbx.druid.sql.ast.SQLName;
import com.alibaba.polardbx.druid.sql.ast.SQLObjectImpl;
import com.alibaba.polardbx.druid.sql.visitor.SQLASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class SQLAlterTableDropPartition extends SQLObjectImpl implements SQLAlterTableItem, SQLAlterTableGroupItem {
    private boolean ifExists = false;
    private boolean purge;
    private final List<SQLExpr> partitions = new ArrayList<SQLExpr>(4);

    private boolean isSubPartition = false;
    /**
     * partitionName is only for drop subpartition
     */
    private SQLName partitionName;

    public SQLAlterTableDropPartition() {

    }

    public List<SQLExpr> getPartitions() {
        return partitions;
    }

    public void addPartition(SQLExpr partition) {
        if (partition != null) {
            partition.setParent(this);
        }
        this.partitions.add(partition);
    }

    public boolean isIfExists() {
        return ifExists;
    }

    public void setIfExists(boolean ifExists) {
        this.ifExists = ifExists;
    }

    public boolean isPurge() {
        return purge;
    }

    public void setPurge(boolean purge) {
        this.purge = purge;
    }

    public boolean isSubPartition() {
        return isSubPartition;
    }

    public void setSubPartition(boolean subPartition) {
        isSubPartition = subPartition;
    }

    public SQLName getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(SQLName partitionName) {
        this.partitionName = partitionName;
    }

    @Override
    protected void accept0(SQLASTVisitor visitor) {
        if (visitor.visit(this)) {
            acceptChild(visitor, partitions);
        }
        visitor.endVisit(this);
    }
}
