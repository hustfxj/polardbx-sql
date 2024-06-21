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

package com.alibaba.polardbx.druid.sql.dialect.mysql.ast.statement;

import com.alibaba.polardbx.druid.sql.ast.SqlType;
import com.alibaba.polardbx.druid.sql.dialect.mysql.ast.expr.MySqlUserName;
import com.alibaba.polardbx.druid.sql.dialect.mysql.visitor.MySqlASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * For statement like:
 * <p>
 * <code>
 * SET ROLE a, b;
 * </code>
 *
 * @author bairui.lrj
 * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/set-role.html>Set Role</a>
 * @since 2.1.12.9_drds_3
 */
public class MySqlSetRoleStatement extends MySqlStatementImpl {
    public enum RoleSpec {
        /**
         * <code>SET ROLE DEFAULT;</code>
         */
        DEFAULT,
        /**
         * <code>SET ROLE NONE;</code>
         */
        NONE,
        /**
         * <code>SET ROLE ALL;</code>
         */
        ALL,
        /**
         * <code>SET ROLE ALL EXCEPT a, b;</code>
         */
        ALL_EXCEPT,
        /**
         * <code>SET ROLE a, b;</code>
         */
        ROLES
    }

    private final List<MySqlUserName> roles = new ArrayList<MySqlUserName>();
    private RoleSpec roleSpec = RoleSpec.DEFAULT;

    public RoleSpec getRoleSpec() {
        return roleSpec;
    }

    public void setRoleSpec(RoleSpec roleSpec) {
        this.roleSpec = roleSpec;
    }

    public List<MySqlUserName> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public void addRole(MySqlUserName role) {
        role.verifyNoIdentify();
        roles.add(role);
    }

    @Override
    public void accept0(MySqlASTVisitor v) {
        if (v.visit(this)) {
            acceptChild(v, roles);
        }

        v.endVisit(this);
    }

    @Override
    public SqlType getSqlType() {
        return null;
    }
}
