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

package com.alibaba.polardbx.optimizer.core.rel.ddl.data.gsi;

import com.alibaba.polardbx.optimizer.core.rel.ddl.data.DdlPreparedData;
import com.alibaba.polardbx.optimizer.core.rel.ddl.data.DropLocalIndexPreparedData;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
public class DropIndexWithGsiPreparedData extends DdlPreparedData {

    private String schemaName;

    private DropGlobalIndexPreparedData globalIndexPreparedData;
    private List<DropLocalIndexPreparedData> localIndexPreparedDataList = new ArrayList<>();

    public void addLocalIndexPreparedData(DropLocalIndexPreparedData localIndexPreparedData) {
        if (this.localIndexPreparedDataList.stream().anyMatch(x -> x.equals(localIndexPreparedData))) {
            return;
        }
        this.localIndexPreparedDataList.add(localIndexPreparedData);
    }

    public boolean hasLocalIndexOnClustered(String clusteredIndexName) {
        return localIndexPreparedDataList.stream()
            .anyMatch(x -> x.isOnClustered() && x.getTableName().equalsIgnoreCase(clusteredIndexName));
    }

    public void setDdlVersionId(@NotNull Long versionId) {
        super.setDdlVersionId(versionId);
        if (null != globalIndexPreparedData) {
            globalIndexPreparedData.setDdlVersionId(versionId);
        }
        if (null != localIndexPreparedDataList) {
            localIndexPreparedDataList.forEach(p -> p.setDdlVersionId(versionId));
        }
    }
}
