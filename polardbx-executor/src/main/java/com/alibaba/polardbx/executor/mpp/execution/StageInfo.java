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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.polardbx.executor.mpp.execution;

import com.alibaba.polardbx.executor.mpp.operator.DriverContext;
import com.alibaba.polardbx.executor.mpp.operator.DriverStats;
import com.alibaba.polardbx.executor.mpp.operator.TaskStats;
import com.alibaba.polardbx.optimizer.core.datatype.DataType;
import com.alibaba.polardbx.util.MoreObjects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Immutable
public class StageInfo {
    private final StageId stageId;
    private final StageState state;
    private final URI self;
    private final PlanInfo plan;
    private final List<DataType> types;
    private final StageStats stageStats;
    private final List<TaskInfo> tasks;
    private final List<StageInfo> subStages;
    private final ExecutionFailureInfo failureCause;

    @JsonCreator
    public StageInfo(
        @JsonProperty("stageId") StageId stageId,
        @JsonProperty("state") StageState state,
        @JsonProperty("self") URI self,
        @JsonProperty("plan") @Nullable PlanInfo plan,
        @JsonProperty("types") List<DataType> types,
        @JsonProperty("stageStats") StageStats stageStats,
        @JsonProperty("tasks") List<TaskInfo> tasks,
        @JsonProperty("subStages") List<StageInfo> subStages,
        @JsonProperty("failureCause") ExecutionFailureInfo failureCause) {
        requireNonNull(stageId, "stageId is null");
        requireNonNull(state, "state is null");
        requireNonNull(self, "self is null");
        requireNonNull(tasks, "tasks is null");
        requireNonNull(subStages, "subStages is null");

        this.stageId = stageId;
        this.state = state;
        this.self = self;
        this.plan = plan;
        this.types = types;
        this.stageStats = stageStats;
        this.tasks = ImmutableList.copyOf(tasks);
        this.subStages = subStages;
        this.failureCause = failureCause;
    }

    @JsonProperty
    public StageId getStageId() {
        return stageId;
    }

    @JsonProperty
    public StageState getState() {
        return state;
    }

    @JsonProperty
    public URI getSelf() {
        return self;
    }

    @JsonProperty
    @Nullable
    public PlanInfo getPlan() {
        return plan;
    }

    @JsonProperty
    public List<DataType> getTypes() {
        return types;
    }

    @JsonProperty
    public StageStats getStageStats() {
        return stageStats;
    }

    @JsonProperty
    public List<TaskInfo> getTasks() {
        return tasks;
    }

    @JsonProperty
    public List<StageInfo> getSubStages() {
        return subStages;
    }

    @JsonProperty
    public ExecutionFailureInfo getFailureCause() {
        return failureCause;
    }

    public boolean isFinalStageInfo() {
        return state.isDone() && tasks.stream().allMatch(taskInfo -> taskInfo.getTaskStatus().getState().isDone());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("stageId", stageId)
            .add("state", state)
            .toString();
    }

    public static List<StageInfo> getAllStages(Optional<StageInfo> stageInfo) {
        ImmutableList.Builder<StageInfo> collector = ImmutableList.builder();
        addAllStages(stageInfo, collector);
        return collector.build();
    }

    private static void addAllStages(Optional<StageInfo> stageInfo, ImmutableList.Builder<StageInfo> collector) {
        stageInfo.ifPresent(stage -> {
            collector.add(stage);
            stage.getSubStages().stream()
                .forEach(subStage -> addAllStages(Optional.ofNullable(subStage), collector));
        });
    }

    public boolean isCompleteInfo() {
        return state.isDone() && tasks.stream().allMatch(TaskInfo::isComplete);
    }

    public String toPlanString() {
        MoreObjects.ToStringHelper toString = MoreObjects.toStringHelper(this)
            .add("stageId", stageId);
        if (plan != null) {
            toString.add("plan", plan.toPlanString());
        }
        for (int i = 0; i < subStages.size(); i++) {
            toString.add("subStages" + i, subStages.get(i).toPlanString());
        }
        for (int i = 0; i < tasks.size(); i++) {
            toString.add("task" + i, tasks.get(i).toTaskString());
        }
        return toString.toString();
    }

    private static StageInfo summaryInternal(StageInfo parentStage) {
        if (parentStage.tasks.isEmpty()) {
            return parentStage;
        }

        List<StageInfo> subStages = new ArrayList<>(parentStage.subStages.size());
        for (StageInfo subStage : parentStage.subStages) {
            subStages.add(summaryInternal(subStage));
        }

        StageInfo newStage = new StageInfo(
            parentStage.stageId,
            parentStage.state,
            parentStage.self,
            parentStage.plan,
            parentStage.types,
            parentStage.stageStats,
            parentStage.tasks,
            subStages,
            parentStage.failureCause
        );

        return newStage;
    }

    public static void collectStats(StageInfo rootStage, Map<String, List<Object[]>> driverStatistics) {
        String stageId = rootStage.getStageId().toString();
        if (rootStage.isCompleteInfo() && !driverStatistics.containsKey(stageId)) {
            List<Object[]> driverInfoResult = new ArrayList<>();
            driverStatistics.put(stageId, driverInfoResult);

            // Receive DriverInfo for each driver in each task.
            List<TaskInfo> taskInfoList = rootStage.getTasks();
            for (TaskInfo taskInfo : taskInfoList) {
                TaskStats taskStats = taskInfo.getTaskStats();

                for (DriverStats driverStats : taskStats.getDriverStats()) {
                    DriverContext.DriverRuntimeStatistics driverRuntimeStatistics =
                        driverStats.getDriverRuntimeStatistics();

                    // Convert
                    // [traceId].[stageId].[nodeId].[pipelineId].[threadId]
                    // into
                    // | traceId | stageId-pipelineId | nodeId | threadId |
                    String[] splitDriverId = StringUtils.split(driverStats.getDriverId(), ".");
                    final String traceId = splitDriverId[0];
                    final String stageAndPipeline = splitDriverId[1] + '-' + splitDriverId[3];
                    final String nodeId = splitDriverId[2];
                    final String threadId = splitDriverId[4];

                    if (driverRuntimeStatistics == null) {
                        // Use -1 as the default value when there is no dump in TaskExecution.
                        driverInfoResult.add(new Object[] {
                            traceId, stageAndPipeline, nodeId, threadId,
                            -1, -1, -1, -1, -1, -1, -1, -1});
                    } else {
                        driverInfoResult.add(new Object[] {
                            traceId, stageAndPipeline, nodeId, threadId,
                            driverRuntimeStatistics.getRunningCost(),
                            driverRuntimeStatistics.getPendingCost(),
                            driverRuntimeStatistics.getBlockedCost(),
                            driverRuntimeStatistics.getOpenCost(),
                            driverRuntimeStatistics.getTotalCost(),
                            driverRuntimeStatistics.getRunningCount(),
                            driverRuntimeStatistics.getPendingCount(),
                            driverRuntimeStatistics.getBlockedCount()
                        });
                    }

                }
            }
        }

        // Traverse the children stages.
        if (rootStage.getSubStages() != null && !rootStage.getSubStages().isEmpty()) {
            for (int i = 0; i < rootStage.getSubStages().size(); i++) {
                collectStats(rootStage.getSubStages().get(i), driverStatistics);
            }
        }
    }

    public StageInfo summary() {
        return summaryInternal(this);
    }
}
