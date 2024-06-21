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

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

/**
 * The abstract implement of column reader for management of reference.
 */
public abstract class AbstractColumnReader implements ColumnReader {
    protected static final Logger LOGGER = LoggerFactory.getLogger("oss");

    protected final int columnId;

    private final boolean isPrimaryKey;

    /**
     * The count of reference initialized by zero.
     */
    private final AtomicInteger refCount;

    /**
     * To ensure the idempotency of the close method
     */
    protected final AtomicBoolean isClosed;

    protected final AtomicBoolean hasNoMoreBlocks;

    protected final StampedLock stampedLock;

    protected AbstractColumnReader(int columnId, boolean isPrimaryKey) {
        this.columnId = columnId;
        this.isPrimaryKey = isPrimaryKey;
        this.refCount = new AtomicInteger(0);
        this.isClosed = new AtomicBoolean(false);
        this.hasNoMoreBlocks = new AtomicBoolean(false);
        this.stampedLock = new StampedLock();
    }

    @Override
    public StampedLock getLock() {
        return stampedLock;
    }

    @Override
    public void releaseRef(int decrement) {
        Preconditions.checkArgument(decrement > 0);
        refCount.getAndAdd(-decrement);
    }

    @Override
    public void retainRef(int increment) {
        Preconditions.checkArgument(increment > 0);
        refCount.getAndAdd(increment);
    }

    @Override
    public int refCount() {
        return refCount.get();
    }

    @Override
    public void setNoMoreBlocks() {
        hasNoMoreBlocks.set(true);
    }

    @Override
    public boolean hasNoMoreBlocks() {
        return hasNoMoreBlocks.get();
    }

    @Override
    public boolean needCache() {
        return isPrimaryKey;
    }

    /**
     * Seek to given row group.
     */
    abstract public void seek(int rowGroupId) throws IOException;

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }
}
