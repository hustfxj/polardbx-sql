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

package com.alibaba.polardbx.executor.ddl.job.task.basic;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.polardbx.common.logger.LoggerInit;
import com.alibaba.polardbx.executor.ddl.job.task.BaseSyncTask;
import com.alibaba.polardbx.executor.ddl.job.task.util.TaskName;
import com.alibaba.polardbx.executor.sync.SequenceSyncAction;
import com.alibaba.polardbx.executor.sync.SyncManagerHelper;
import com.alibaba.polardbx.gms.sync.SyncScope;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import lombok.Getter;
import org.apache.calcite.sql.SqlKind;

/**
 * Created by zhuqiwei.
 *
 * @author zhuqiwei
 */

@TaskName(name = "SequenceSyncTask")
@Getter
public class SequenceSyncTask extends BaseSyncTask {
    final private String seqName;
    final private SqlKind sqlKind;

    @JSONCreator
    public SequenceSyncTask(String schemaName,
                            String seqName,
                            SqlKind sqlKind) {
        super(schemaName);
        this.seqName = seqName;
        this.sqlKind = sqlKind;
    }

    @Override
    protected void executeImpl(ExecutionContext executionContext) {
        SyncManagerHelper.sync(new SequenceSyncAction(schemaName, seqName), schemaName, SyncScope.CURRENT_ONLY);
    }

    @Override
    protected void onExecutionSuccess(ExecutionContext executionContext) {
        LoggerInit.TDDL_SEQUENCE_LOG.info(String.format("Sequence operation %s for %s was successful in %s",
            sqlKind, seqName, schemaName));
    }

}
