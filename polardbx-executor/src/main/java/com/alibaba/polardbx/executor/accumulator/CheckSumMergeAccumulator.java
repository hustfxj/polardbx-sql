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

package com.alibaba.polardbx.executor.accumulator;

import com.alibaba.polardbx.common.IOrderInvariantHash;
import com.alibaba.polardbx.common.OrderInvariantHasher;
import com.alibaba.polardbx.executor.accumulator.state.NullableCheckSumGroupState;
import com.alibaba.polardbx.executor.chunk.Block;
import com.alibaba.polardbx.executor.chunk.BlockBuilder;
import com.alibaba.polardbx.executor.chunk.Chunk;
import com.alibaba.polardbx.optimizer.core.datatype.DataType;
import com.alibaba.polardbx.optimizer.core.datatype.DataTypes;

public class CheckSumMergeAccumulator implements Accumulator {

    private static final DataType[] INPUT_TYPES = new DataType[] {DataTypes.LongType};

    private final NullableCheckSumGroupState groupState;

    private final static byte SEPARATOR_TAG = (byte) 255;
    private final static byte NULL_TAG = (byte) 254;

    public CheckSumMergeAccumulator(int capacity) {
        this.groupState = new NullableCheckSumGroupState(capacity, OrderInvariantHasher.class);
    }

    @Override
    public DataType[] getInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public void appendInitValue() {
        this.groupState.appendNull();
    }

    @Override
    public void accumulate(int groupId, Chunk inputChunk, int position) {
        Block inputBlock = inputChunk.getBlock(0);
        if (inputBlock.isNull(position)) {
            return;
        }
        long toMerge = inputBlock.getLong(position);

        // write to group state
        if (groupState.isNull(groupId)) {
            OrderInvariantHasher orderInvariantHasher = new OrderInvariantHasher();
            orderInvariantHasher.add(toMerge);
            groupState.set(groupId, orderInvariantHasher);
        } else {
            IOrderInvariantHash orderInvariantHasher = groupState.getHasher(groupId);
            orderInvariantHasher.add(toMerge);
        }
    }

    @Override
    public void writeResultTo(int groupId, BlockBuilder bb) {
        if (groupState.isNull(groupId)) {
            bb.appendNull();
        } else {
            bb.writeLong(groupState.get(groupId));
        }
    }

    @Override
    public long estimateSize() {
        return groupState.estimateSize();
    }
}
