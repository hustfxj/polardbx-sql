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

package com.alibaba.polardbx.executor.sync;

import com.alibaba.polardbx.executor.cursor.ResultCursor;
import com.alibaba.polardbx.executor.gms.GmsTableMetaManager;
import com.alibaba.polardbx.executor.mdl.MdlContext;
import com.alibaba.polardbx.executor.mdl.MdlDuration;
import com.alibaba.polardbx.executor.mdl.MdlKey;
import com.alibaba.polardbx.executor.mdl.MdlManager;
import com.alibaba.polardbx.executor.mdl.MdlRequest;
import com.alibaba.polardbx.executor.mdl.MdlType;
import com.alibaba.polardbx.optimizer.OptimizerContext;
import com.alibaba.polardbx.optimizer.config.table.TableMeta;
import com.alibaba.polardbx.statistics.SQLRecorderLogger;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author wumu
 */
public class LockTablesSyncAction implements ISyncAction {
    protected String schemaName;
    protected List<String> tableNames;
    protected String traceId;
    protected Long initWait;
    protected Long interval;
    protected TimeUnit timeUnit;

    // This will not conflict, because it is unique in clusters(ClusterAcceptIdGenerator).
    protected long connId;

    public LockTablesSyncAction(String schemaName, List<String> tableNames, String traceId, long connId,
                                Long initWait, Long interval, TimeUnit timeUnit) {
        this.schemaName = schemaName;
        this.tableNames = tableNames;
        this.traceId = traceId;
        this.connId = connId;
        this.initWait = initWait;
        this.interval = interval;
        this.timeUnit = timeUnit;
    }

    @Override
    public ResultCursor sync() {
        syncForLockTable();
        return null;
    }

    protected void syncForLockTable() {
        synchronized (OptimizerContext.getContext(schemaName)) {
            GmsTableMetaManager schemaManager =
                (GmsTableMetaManager) OptimizerContext.getContext(schemaName).getLatestSchemaManager();

            final MdlContext context = MdlManager.addContext(connId, schemaName, initWait, interval, timeUnit);

            for (String primaryTableName : tableNames) {
                TableMeta currentMeta = schemaManager.getTableWithNull(primaryTableName);
                if (currentMeta == null) {
                    continue;
                }

                SQLRecorderLogger.ddlLogger.warn(MessageFormat.format(
                    "{0}  {1}.addContext({2})", Thread.currentThread().getName(),
                    this.hashCode(), connId));

                context.acquireLock(new MdlRequest(1L,
                    MdlKey.getTableKeyWithLowerTableName(schemaName, currentMeta.getDigest()),
                    MdlType.MDL_EXCLUSIVE,
                    MdlDuration.MDL_TRANSACTION));

                SQLRecorderLogger.ddlLogger.warn(MessageFormat.format(
                    "[Mdl write lock acquired table[{0}]]",
                    currentMeta.getDigest()));
            }
        }
    }

    public String getSchemaName() {
        return schemaName;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public void setTableNames(List<String> tableNames) {
        this.tableNames = tableNames;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public long getConnId() {
        return connId;
    }

    public void setConnId(long connId) {
        this.connId = connId;
    }

    public void setInitWait(Long initWait) {
        this.initWait = initWait;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public Long getInitWait() {
        return initWait;
    }

    public Long getInterval() {
        return interval;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
