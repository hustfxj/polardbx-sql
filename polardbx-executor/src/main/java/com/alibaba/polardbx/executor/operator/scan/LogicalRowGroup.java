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

package com.alibaba.polardbx.executor.operator.scan;

import com.alibaba.polardbx.executor.chunk.Chunk;
import org.apache.hadoop.fs.Path;

/**
 * Logical information of an orc row group.
 *
 * @param <VECTOR> the class of a column in row group (arrow block, value vector, array...)
 * @param <STATISTICS> the class of column statistics
 */
public interface LogicalRowGroup<VECTOR, STATISTICS> {
    String BLOCK_LOAD_TIMER = "BlockLoadTimer";
    String BLOCK_MEMORY_COUNTER = "BlockMemoryCounter";

    Path path();

    int stripeId();

    int groupId();

    /**
     * The row count of the row group.
     */
    int rowCount();

    /**
     * The starting row id of the row group.
     */
    int startRowId();

    RowGroupReader<Chunk> getReader();
}
