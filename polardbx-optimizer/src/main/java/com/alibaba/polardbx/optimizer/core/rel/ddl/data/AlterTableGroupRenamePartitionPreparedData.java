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

package com.alibaba.polardbx.optimizer.core.rel.ddl.data;

import com.alibaba.polardbx.common.utils.GeneralUtil;
import com.alibaba.polardbx.common.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class AlterTableGroupRenamePartitionPreparedData extends AlterTableGroupBasePreparedData {

    private List<Pair<String, String>> changePartitionsPair;
    private boolean subPartitionRename;
    private boolean renameNothing = false;

    public AlterTableGroupRenamePartitionPreparedData() {
    }

    public List<Pair<String, String>> getChangePartitionsPair() {
        return changePartitionsPair;
    }

    public void setChangePartitionsPair(
        List<Pair<String, String>> changePartitionsPair) {
        this.changePartitionsPair = changePartitionsPair;
    }

    public boolean isSubPartitionRename() {
        return subPartitionRename;
    }

    public void setSubPartitionRename(boolean subPartitionRename) {
        this.subPartitionRename = subPartitionRename;
    }

    public List<String> getRelatedPartitions() {
        List<String> relatedParts = new ArrayList<>();
        for (Pair<String, String> changePair : changePartitionsPair) {
            relatedParts.add(changePair.getKey());
            relatedParts.add(changePair.getValue());
        }

        return relatedParts;
    }

    public boolean isRenameNothing() {
        return renameNothing;
    }

    public void setRenameNothing(boolean renameNothing) {
        this.renameNothing = renameNothing;
    }

    public boolean partitionNameNoChange() {
        if (GeneralUtil.isEmpty(changePartitionsPair)) {
            return true;
        }
        for (Pair<String, String> changePair : changePartitionsPair) {
            if (!changePair.getKey().equalsIgnoreCase(changePair.getValue())) {
                return false;
            }
        }
        return true;
    }
}
