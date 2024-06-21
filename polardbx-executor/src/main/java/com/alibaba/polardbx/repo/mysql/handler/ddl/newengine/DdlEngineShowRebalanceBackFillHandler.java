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

package com.alibaba.polardbx.repo.mysql.handler.ddl.newengine;

import com.alibaba.polardbx.common.utils.GeneralUtil;
import com.alibaba.polardbx.executor.cursor.Cursor;
import com.alibaba.polardbx.executor.cursor.impl.ArrayResultCursor;
import com.alibaba.polardbx.executor.ddl.job.task.CostEstimableDdlTask;
import com.alibaba.polardbx.executor.ddl.newengine.meta.DdlPlanManager;
import com.alibaba.polardbx.executor.ddl.newengine.utils.DdlHelper;
import com.alibaba.polardbx.executor.ddl.newengine.utils.TaskHelper;
import com.alibaba.polardbx.executor.handler.subhandler.InformationSchemaRebalanceProgressHandler;
import com.alibaba.polardbx.executor.spi.IRepository;
import com.alibaba.polardbx.gms.scheduler.DdlPlanRecord;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.datatype.DataType;
import com.alibaba.polardbx.optimizer.core.datatype.DataTypeUtil;
import com.alibaba.polardbx.optimizer.core.datatype.DataTypes;
import com.alibaba.polardbx.optimizer.core.rel.dal.LogicalDal;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.commons.collections.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.polardbx.gms.topology.SystemDbHelper.DEFAULT_DB_NAME;
import static com.alibaba.polardbx.optimizer.view.InformationSchemaRebalanceBackFill.*;

public class DdlEngineShowRebalanceBackFillHandler extends DdlEngineJobsHandler {

    public DdlEngineShowRebalanceBackFillHandler(IRepository repo) {
        super(repo);
    }

    DdlPlanManager planManager = new DdlPlanManager();

    @Override
    protected Cursor doHandle(LogicalDal logicalPlan, ExecutionContext executionContext) {
        List<Map<String, Object>> logicBackfillResult = DdlHelper.getServerConfigManager().executeQuerySql(
            "SELECT " +
                "T1.DDL_JOB_ID, " +
                "SUM(T1.`CURRENT_SPEED(ROWS/SEC)`) AS `CURRENT_SPEED(ROWS/SEC)`," +
                "SUM(T1.FINISHED_ROWS) / (UNIX_TIMESTAMP(NOW())-T2.GMT_CREATED/1000) AS `AVERAGE_SPEED(ROWS/SEC)` ," +
                "SUM(T1.FINISHED_ROWS) AS FINISHED_ROWS, " +
                "T1.`APPROXIMATE_TOTAL_ROWS` " +
                "FROM " +
                "INFORMATION_SCHEMA.REBALANCE_BACKFILL T1 " +
                "JOIN METADB.DDL_ENGINE T2 " +
                "ON T1.DDL_JOB_ID=T2.JOB_ID",
            DEFAULT_DB_NAME,
            null
        );
        ArrayResultCursor logicalBackfillResultCursor = buildLogicalBackfillResultCursor();
        boolean logicalBackfillProgress = false;
        if (CollectionUtils.isNotEmpty(logicBackfillResult)) {
            for (Map<String, Object> map : logicBackfillResult) {
                if (map.get(DDL_JOB_ID) == null) {
                    continue;
                }
                logicalBackfillProgress = true;
                final long jobId = parseLong(map.get(DDL_JOB_ID));
                final Optional<DdlPlanRecord> ddlPlanRecordOptional = planManager.getDdlPlanByJobId(jobId);
                long previousFinishedRows = 0L;
                if (ddlPlanRecordOptional.isPresent()) {
                    CostEstimableDdlTask.CostInfo costInfo =
                        TaskHelper.decodeCostInfo(ddlPlanRecordOptional.get().getExtras());
                    if (costInfo != null) {
                        previousFinishedRows += costInfo.rows;
                    }
                }

                long finishedRows = parseLong(map.get(FINISHED_ROWS));
                long approximateTotalRows = parseLong(map.get(APPROXIMATE_TOTAL_ROWS));

                logicalBackfillResultCursor.addRow(
                    new Object[] {
                        map.get(DDL_JOB_ID),
                        map.get(CURRENT_SPEED),
                        map.get(AVERAGE_SPEED),
                        finishedRows + previousFinishedRows,
                        approximateTotalRows + previousFinishedRows
                    }
                );
            }
        }
        if (!logicalBackfillProgress) {
            ArrayResultCursor physicalBackfillResultCursor = buildPhysicalBackfillResultCursor();
            InformationSchemaRebalanceProgressHandler.buildRebalanceBackFillView(physicalBackfillResultCursor);
            if (GeneralUtil.isNotEmpty(physicalBackfillResultCursor.getRows())) {
                return physicalBackfillResultCursor;
            }
        }
        return logicalBackfillResultCursor;
    }

    private long parseLong(@NotNull Object val) {
        return Long.parseLong(String.valueOf(DataTypeUtil.toJavaObject(null, val)));
    }

    private String parseString(@NotNull Object val) {
        return String.valueOf(DataTypeUtil.toJavaObject(null, val));
    }

    private ArrayResultCursor buildLogicalBackfillResultCursor() {
        ArrayResultCursor resultCursor = new ArrayResultCursor("REBALANCE_BACKFILL");
        resultCursor.addColumn(DDL_JOB_ID, DataTypes.StringType);
        resultCursor.addColumn(CURRENT_SPEED, DataTypes.StringType);
        resultCursor.addColumn(AVERAGE_SPEED, DataTypes.StringType);
        resultCursor.addColumn(FINISHED_ROWS, DataTypes.StringType);
        resultCursor.addColumn(APPROXIMATE_TOTAL_ROWS, DataTypes.StringType);
        resultCursor.initMeta();
        return resultCursor;
    }

    private ArrayResultCursor buildPhysicalBackfillResultCursor() {
        ArrayResultCursor resultCursor = new ArrayResultCursor("REBALANCE_PROOGRESS");
        int i = 0;
        resultCursor.addColumn("JOB_ID", DataTypes.StringType);
        resultCursor.addColumn("TABLE_SCHEMA", DataTypes.StringType);
        resultCursor.addColumn("STAGE", DataTypes.StringType);
        resultCursor.addColumn("STATE", DataTypes.StringType);
        resultCursor.addColumn("PROGRESS", DataTypes.DoubleType);
        resultCursor.addColumn("TOTAL_TASK", DataTypes.IntegerType);
        resultCursor.addColumn("FINISHED_TASK", DataTypes.IntegerType);
        resultCursor.addColumn("RUNNING_TASK", DataTypes.IntegerType);
        resultCursor.addColumn("NOTSTARTED_TASK", DataTypes.IntegerType);
        resultCursor.addColumn("FAILED_TASK", DataTypes.IntegerType);
        resultCursor.addColumn("INFO", DataTypes.StringType);
        resultCursor.addColumn("START_TIME", DataTypes.TimestampType);
        resultCursor.addColumn("LAST_UPDATE_TIME", DataTypes.TimestampType);
        resultCursor.addColumn("DDL_STMT", DataTypes.StringType);

        resultCursor.initMeta();
        return resultCursor;
    }
}
