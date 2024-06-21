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
import com.alibaba.polardbx.executor.ddl.job.task.BaseValidateTask;
import com.alibaba.polardbx.executor.ddl.job.task.util.TaskName;
import com.alibaba.polardbx.executor.ddl.job.validator.TableGroupValidator;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import lombok.Getter;

@Getter
@TaskName(name = "EmptyTableGroupValidateTask")
public class EmptyTableGroupValidateTask extends BaseValidateTask {

    private String tableGroupName;

    @JSONCreator
    public EmptyTableGroupValidateTask(String schemaName,
                                       String tableGroupName) {
        super(schemaName);
        this.tableGroupName = tableGroupName;
    }

    @Override
    public void executeImpl(ExecutionContext executionContext) {
        TableGroupValidator.validateTableGroupIsEmpty(schemaName, tableGroupName);
    }

    @Override
    protected String remark() {
        return "|tableGroupName: " + tableGroupName;
    }

}
