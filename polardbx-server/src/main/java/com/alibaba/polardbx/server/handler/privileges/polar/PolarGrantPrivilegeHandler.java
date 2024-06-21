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

package com.alibaba.polardbx.server.handler.privileges.polar;

import com.alibaba.polardbx.server.ServerConnection;
import com.alibaba.polardbx.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.polardbx.druid.sql.ast.statement.SQLGrantStatement;
import com.alibaba.polardbx.druid.sql.parser.ByteString;
import com.alibaba.polardbx.common.audit.AuditAction;
import com.alibaba.polardbx.common.utils.GeneralUtil;
import com.alibaba.polardbx.common.utils.logger.Logger;
import com.alibaba.polardbx.common.utils.logger.LoggerFactory;
import com.alibaba.polardbx.gms.privilege.PolarAccountInfo;
import com.alibaba.polardbx.gms.privilege.PolarPrivManager;
import com.alibaba.polardbx.gms.privilege.PrivilegeKind;
import org.apache.calcite.sql.SqlKind;

import java.util.ArrayList;
import java.util.List;

import static com.alibaba.polardbx.server.handler.privileges.polar.PolarHandlerCommon.checkDrdsRoot;
import static com.alibaba.polardbx.gms.privilege.audit.AuditPrivilege.polarAudit;

/**
 * Handle grant privileges statement.
 *
 * @author bairui.lrj
 * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/grant.html">Grant Privileges</a>
 * @since 5.4.9
 */
public class PolarGrantPrivilegeHandler extends AbstractPrivilegeCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(PolarGrantPrivilegeHandler.class);

    private final SQLGrantStatement stmt;

    public PolarGrantPrivilegeHandler(ByteString sql,
                                      ServerConnection serverConn,
                                      PolarAccountInfo granter,
                                      PolarPrivManager polarPrivManager,
                                      SQLGrantStatement stmt) {
        super(sql, serverConn, granter, polarPrivManager);
        this.stmt = stmt;
    }

    private List<PolarAccountInfo> getGrantees(ServerConnection c) {
        List<PolarAccountInfo> grantees = new ArrayList<>();
        try {
            SQLExprTableSource sqlExprTableSource = (SQLExprTableSource) stmt.getResource();
            grantees = PolarHandlerCommon.getGrantees(sqlExprTableSource, stmt.getUsers(),
                stmt.getPrivileges(), c);
            if (stmt.getWithGrantOption()) {
                for (PolarAccountInfo grantee : grantees) {
                    if (grantee.getFirstDbPriv() != null) {
                        grantee.getFirstDbPriv().grantPrivilege(PrivilegeKind.GRANT_OPTION);
                    } else if (grantee.getFirstTbPriv() != null) {
                        grantee.getFirstTbPriv().grantPrivilege(PrivilegeKind.GRANT_OPTION);
                    } else {
                        grantee.getInstPriv().grantPrivilege(PrivilegeKind.GRANT_OPTION);
                    }
                }
            }
        } catch (Exception e) {
            GeneralUtil.nestedException(e.getMessage(), e);
        }

        return grantees;
    }

    private void checkGrantees(List<PolarAccountInfo> grantees) {
        checkDrdsRoot(grantees);
    }

    @Override
    protected void doHandle() {
        ByteString sql = getSql();
        ServerConnection c = getServerConn();
        List<PolarAccountInfo> grantees = getGrantees(getServerConn());
        checkGrantees(grantees);

        PolarAccountInfo granter = getGranter();

        PolarPrivManager.getInstance().grantPrivileges(granter, c.getActiveRoles(), grantees);
        polarAudit(getServerConn().getConnectionInfo(), getSql().toString(), AuditAction.GRANT);
    }

    @Override
    protected SqlKind getSqlKind() {
        return SqlKind.GRANT_PRIVILEGE;
    }
}
