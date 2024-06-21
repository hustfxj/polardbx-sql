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

package com.alibaba.polardbx.druid.sql.ast.statement;

import com.alibaba.polardbx.druid.sql.ast.SQLExpr;
import com.alibaba.polardbx.druid.sql.ast.SQLStatementImpl;
import com.alibaba.polardbx.druid.sql.visitor.SQLASTVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chenghui.lch
 */
public class SQLAlterSystemRefreshStorageStatement extends SQLStatementImpl implements SQLAlterStatement {

    private SQLExpr targetStorage;
    private List<SQLAssignItem> assignItems = new ArrayList<SQLAssignItem>();

    public SQLAlterSystemRefreshStorageStatement() {
    }

    @Override
    protected void accept0(SQLASTVisitor visitor) {
        if (visitor.visit(this)) {
            acceptChild(visitor, targetStorage);
            acceptChild(visitor, assignItems);
        }
        visitor.endVisit(this);
    }

    public SQLExpr getTargetStorage() {
        return targetStorage;
    }

    public void setTargetStorage(SQLExpr targetStorage) {
        this.targetStorage = targetStorage;
    }

    public List<SQLAssignItem> getAssignItems() {
        return assignItems;
    }

    public void setAssignItems(List<SQLAssignItem> assignItems) {
        this.assignItems = assignItems;
    }
}
