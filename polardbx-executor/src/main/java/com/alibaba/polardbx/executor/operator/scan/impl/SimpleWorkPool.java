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

package com.alibaba.polardbx.executor.operator.scan.impl;

import com.alibaba.polardbx.executor.chunk.Chunk;
import com.alibaba.polardbx.executor.operator.scan.ColumnarSplit;
import com.alibaba.polardbx.executor.operator.scan.ScanWork;
import com.alibaba.polardbx.executor.operator.scan.WorkPool;
import com.google.common.base.Preconditions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class SimpleWorkPool implements WorkPool<ColumnarSplit, Chunk> {
    private Map<Integer, Queue<ColumnarSplit>> splitMap;
    private boolean noMoreSplits;

    public SimpleWorkPool() {
        this.splitMap = new LinkedHashMap<>();
        this.noMoreSplits = false;
    }

    @Override
    public void addSplit(int driverId, ColumnarSplit split) {
        Queue<ColumnarSplit> splitQueue = splitMap.computeIfAbsent(driverId,
            any -> new PriorityBlockingQueue<>());

        splitQueue.add(split);
    }

    @Override
    public void noMoreSplits(int driverId) {
        noMoreSplits = true;
    }

    @Override
    public ScanWork<ColumnarSplit, Chunk> pickUp(int driverId) {
        Preconditions.checkArgument(noMoreSplits);

        Queue<ColumnarSplit> splitQueue = splitMap.get(driverId);
        if (splitQueue == null || splitQueue.isEmpty()) {
            return null;
        }

        // Peek all head of queue until we find a split with effective scan-work.
        while (!splitQueue.isEmpty()) {
            ColumnarSplit columnarSplit = splitQueue.peek();
            ScanWork<ColumnarSplit, Chunk> scanWork = columnarSplit.nextWork();
            if (scanWork == null) {
                // Remove this split from queue because the scan-works is run out.
                splitQueue.poll();
            } else {
                return scanWork;
            }
        }

        return null;
    }
}
