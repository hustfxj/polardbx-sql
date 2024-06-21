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

package com.alibaba.polardbx.optimizer.partition.common;

import com.alibaba.polardbx.gms.partition.TablePartitionRecord;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * @author chenghui.lch
 */
public enum PartitionTableType {
    PARTITION_TABLE(TablePartitionRecord.PARTITION_TABLE_TYPE_PARTITION_TABLE),
    GSI_TABLE(TablePartitionRecord.PARTITION_TABLE_TYPE_GSI_TABLE),
    SINGLE_TABLE(TablePartitionRecord.PARTITION_TABLE_TYPE_SINGLE_TABLE),
    BROADCAST_TABLE(TablePartitionRecord.PARTITION_TABLE_TYPE_BROADCAST_TABLE),
    GSI_BROADCAST_TABLE(TablePartitionRecord.PARTITION_TABLE_TYPE_GSI_BROADCAST_TABLE),
    GSI_SINGLE_TABLE(TablePartitionRecord.PARTITION_TABLE_TYPE_GSI_SINGLE_TABLE),
    OSS_TABLE(TablePartitionRecord.PARTITION_TABLE_TYPE_OSS_TABLE),
    COLUMNAR_TABLE(TablePartitionRecord.PARTITION_TABLE_TYPE_COLUMNAR_TABLE);

    private int tblTypeVal;
    private String tableTypeName;

    PartitionTableType(int typeVal) {
        this.tblTypeVal = typeVal;
        initTableTypeName();
    }

    public static PartitionTableType getTypeByIntVal(int tblTypeVal) {
        if (tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_PARTITION_TABLE) {
            return PARTITION_TABLE;
        } else if (tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_GSI_TABLE) {
            return GSI_TABLE;
        } else if (tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_SINGLE_TABLE) {
            return SINGLE_TABLE;
        } else if (tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_BROADCAST_TABLE) {
            return BROADCAST_TABLE;
        } else if (tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_GSI_SINGLE_TABLE) {
            return GSI_SINGLE_TABLE;
        } else if (tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_GSI_BROADCAST_TABLE) {
            return GSI_BROADCAST_TABLE;
        } else if (tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_OSS_TABLE) {
            return OSS_TABLE;
        } else if (tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_COLUMNAR_TABLE) {
            return COLUMNAR_TABLE;
        }
        return null;
    }

    private void initTableTypeName() {
        if (this.tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_PARTITION_TABLE) {
            tableTypeName = "PARTITION_TABLE";
        } else if (this.tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_GSI_TABLE) {
            tableTypeName = "GSI_TABLE";
        } else if (this.tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_SINGLE_TABLE) {
            tableTypeName = "SINGLE_TABLE";
        } else if (this.tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_BROADCAST_TABLE) {
            tableTypeName = "BROADCAST_TABLE";
        } else if (this.tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_GSI_BROADCAST_TABLE) {
            tableTypeName = "GSI_BROADCAST_TABLE";
        } else if (this.tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_GSI_SINGLE_TABLE) {
            tableTypeName = "GSI_SINGLE_TABLE";
        } else if (this.tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_OSS_TABLE) {
            tableTypeName = "OSS_TABLE";
        } else if (this.tblTypeVal == TablePartitionRecord.PARTITION_TABLE_TYPE_COLUMNAR_TABLE) {
            tableTypeName = "COLUMNAR_TABLE";
        }
    }

    public int getTableTypeIntValue() {
        return this.tblTypeVal;
    }

    public String getTableTypeName() {
        return this.tableTypeName;
    }

    public boolean isGsiTableType() {
        if (this == PartitionTableType.GSI_TABLE || this == PartitionTableType.GSI_BROADCAST_TABLE
            || this == PartitionTableType.GSI_SINGLE_TABLE) {
            return true;
        }
        return false;
    }

    public static final EnumSet<PartitionTableType> PARTITIONED_TABLE = EnumSet.of(
        PARTITION_TABLE,
        GSI_TABLE,
        COLUMNAR_TABLE);

    public boolean isA(@NotNull EnumSet<PartitionTableType> set) {
        return set.contains(this);
    }
}
