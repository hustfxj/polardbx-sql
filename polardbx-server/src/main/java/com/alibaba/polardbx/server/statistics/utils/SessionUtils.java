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

package com.alibaba.polardbx.server.statistics.utils;

import com.alibaba.polardbx.CobarServer;
import com.alibaba.polardbx.gms.topology.SystemDbHelper;
import com.alibaba.polardbx.net.FrontendConnection;
import com.alibaba.polardbx.net.NIOProcessor;
import com.alibaba.polardbx.server.ServerConnection;
import com.alibaba.polardbx.common.utils.TStringUtil;

public class SessionUtils {

    /**
     * 获取除了系统库的活跃连接数总和
     */
    public static long getAllActiveConnectionsNum() {
        long count = 0L;
        for (NIOProcessor p : CobarServer.getInstance().getProcessors()) {
            for (FrontendConnection fc : p.getFrontends().values()) {
                if (fc instanceof ServerConnection &&
                    !TStringUtil.equals(SystemDbHelper.DEFAULT_DB_NAME, fc.getSchema()) &&
                    !TStringUtil.equals(SystemDbHelper.CDC_DB_NAME, fc.getSchema())) {
                    ServerConnection sc = (ServerConnection) fc;
                    boolean isStmtExecuting = sc.isStatementExecuting().get();
                    if (isStmtExecuting) {
                        count++;
                    }
                }

            }
        }
        return count;
    }

    /**
     * 获取除了系统库的连接数总和
     */
    public static long getAllConnectionsNum() {
        long count = 0L;
        for (NIOProcessor p : CobarServer.getInstance().getProcessors()) {
            for (FrontendConnection fc : p.getFrontends().values()) {
                if (fc instanceof ServerConnection &&
                    !TStringUtil.equals(SystemDbHelper.DEFAULT_DB_NAME, fc.getSchema()) &&
                    !TStringUtil.equals(SystemDbHelper.CDC_DB_NAME, fc.getSchema())) {
                    count++;
                }

            }
        }
        return count;
    }

    public static long getActiveConnectionsNum(String schema) {
        long count = 0L;
        for (NIOProcessor p : CobarServer.getInstance().getProcessors()) {
            for (FrontendConnection fc : p.getFrontends().values()) {
                if (fc instanceof ServerConnection && TStringUtil.equals(schema, fc.getSchema())) {
                    ServerConnection sc = (ServerConnection) fc;
                    boolean isStmtExecuting = sc.isStatementExecuting().get();
                    if (isStmtExecuting) {
                        count++;
                    }
                }

            }
        }
        return count;
    }

    public static long getConnectionsNum(String schema) {
        long count = 0L;
        for (NIOProcessor p : CobarServer.getInstance().getProcessors()) {
            for (FrontendConnection fc : p.getFrontends().values()) {
                if (fc instanceof ServerConnection && TStringUtil.equals(schema, fc.getSchema())) {
                    count++;
                }

            }
        }
        return count;
    }
}
