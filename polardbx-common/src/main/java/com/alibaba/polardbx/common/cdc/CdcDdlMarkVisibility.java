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

package com.alibaba.polardbx.common.cdc;

public enum CdcDdlMarkVisibility {
    /**
     * 可见性为私有
     * PolarX专有的ddl类型，不写入逻辑binlog文件，即：不支持复制到原生MySQL，也不支持复制到PolarDB-X
     */
    Private(0),
    /**
     * 可见性为公开
     * CDC需要将该类型的ddl sql转换为单机形态，写入逻辑binlog文件，以支持复制到原生MySQL；以注释形态写入到逻辑binlog文件，以支持复制到PolarDB-X
     */
    Public(1),
    /**
     * PolarDB-X专有的ddl(类型专有，或者语法专有)
     * 1.支持复制到另一个PolarDB-X实例，以注释的形式写入逻辑binlog的Query event
     * 2.不支持复制到单机MySQL实例，在逻辑binlog的Query event中不予记录
     */
    Protected(2);

    private final int value;

    CdcDdlMarkVisibility(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
