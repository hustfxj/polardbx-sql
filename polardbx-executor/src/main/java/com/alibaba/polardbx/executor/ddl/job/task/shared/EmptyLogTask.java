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

package com.alibaba.polardbx.executor.ddl.job.task.shared;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.polardbx.executor.ddl.job.task.BaseValidateTask;
import com.alibaba.polardbx.executor.ddl.job.task.util.TaskName;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.statistics.SQLRecorderLogger;
import lombok.Getter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author wumu
 */
@Getter
@TaskName(name = "EmptyLogTask")
public class EmptyLogTask extends EmptyTask {
    private String msg;

    @JSONCreator
    public EmptyLogTask(String schemaName, String msg) {
        super(schemaName);
        this.msg = msg;
    }

    @Override
    public void executeImpl(ExecutionContext executionContext) {
        Date currentTime = Calendar.getInstance().getTime();
        SQLRecorderLogger.scaleOutTaskLogger.info(String.format("jobId: %s; msg: %s; timestamp: %s; time: %s",
            this.getJobId(), msg, currentTime.getTime(),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentTime)));
    }
}
