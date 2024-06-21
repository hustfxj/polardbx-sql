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

package com.alibaba.polardbx.executor.operator;

import com.alibaba.polardbx.common.exception.TddlRuntimeException;
import com.alibaba.polardbx.common.utils.GeneralUtil;
import com.alibaba.polardbx.common.utils.bloomfilter.BloomFilterInfo;
import com.alibaba.polardbx.executor.archive.reader.OSSColumnTransformer;
import com.alibaba.polardbx.executor.archive.reader.SimpleOSSPhysicalTableReadResult;
import com.alibaba.polardbx.executor.chunk.Chunk;
import com.alibaba.polardbx.executor.chunk.MutableChunk;
import com.alibaba.polardbx.executor.mpp.metadata.Split;
import com.alibaba.polardbx.executor.mpp.split.OssSplit;
import com.alibaba.polardbx.executor.vectorized.VectorizedExpression;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.datatype.DataType;
import com.alibaba.polardbx.optimizer.core.rel.OSSTableScan;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.alibaba.polardbx.common.exception.code.ErrorCode.ERR_EXECUTE_ON_MYSQL;

public class AsyncOSSTableScanExec extends AbstractOSSTableScanExec {
    protected OSSTableScan ossTableScan;
    private List<DataType> dataTypeList;
    private List<DataType<?>> filterInputTypes;
    private List<DataType<?>> filterOutputTypes;

    protected AtomicLong fetchTimeCost = new AtomicLong();
    private AtomicBoolean noMoreSplit;

    private volatile boolean isFinished;

    // for data filter
    private VectorizedExpression condition;
    private MutableChunk preAllocatedChunk;
    private int[] filterBitmap;
    private int[] outProject;
    private List<DataType<?>> inProjectDataTypeList;
    protected volatile TddlRuntimeException exception = null;

    private OSSTableScanClient client;
    private SimpleOSSPhysicalTableReadResult resultSetHandler;

    public AsyncOSSTableScanExec(OSSTableScan ossTableScan, ExecutionContext context, List<DataType> dataTypeList) {
        super(context);

        this.ossTableScan = ossTableScan;
        this.dataTypeList = dataTypeList;
        this.noMoreSplit = new AtomicBoolean(false);
        this.isFinished = false;
        this.inProjectDataTypeList = ossTableScan.getOrcNode().getInProjectsDataType();

        this.client = new OSSTableScanClient(ossTableScan, context, dataTypeList);
        this.resultSetHandler = new SimpleOSSPhysicalTableReadResult(inProjectDataTypeList, context, ossTableScan);
    }

    @Override
    public synchronized void initWaitFuture(ListenableFuture<List<BloomFilterInfo>> listListenableFuture) {
        this.client.initWaitFuture(listListenableFuture);
    }

    @Override
    public void addSplit(Split split) {
        OssSplit ossSplit = (OssSplit) split.getConnectorSplit();
        this.client.addSplit(ossSplit);
    }

    @Override
    public void noMoreSplits() {
        this.noMoreSplit.set(true);
    }

    @Override
    public Integer getSourceId() {
        return ossTableScan.getRelatedId();
    }

    @Override
    void doOpen() {
        if (!noMoreSplit.get()) {
            throw new TddlRuntimeException(ERR_EXECUTE_ON_MYSQL, "input splits are not ready!");
        }

        // open prefetch threads
        client.executePrefetchThread();
        createBlockBuilders();
    }

    @Override
    Chunk doSourceNextChunk() {
        long fetchStartNano = System.nanoTime();
        try {
            Chunk chunk = fetchChunk();
            if (chunk == null || chunk.getPositionCount() == 0) {
                return null;
            }
            return chunk;
        } finally {
            fetchTimeCost.addAndGet(System.nanoTime() - fetchStartNano);
        }
    }

    @Nullable
    private Chunk fetchChunk() {
        if (this.isFinished) {
            return null;
        }

        OSSTableScanClient.ResultFromOSS resultFromOSS = null;
        try {
            // check client state, and then consume the production.
            while (!client.isFinished()) {
                resultFromOSS = client.popResult();
                if (resultFromOSS == null) {
                    return null;
                }

                // if there are results in a chunk(using statistics), return the result directly
                if (resultFromOSS.isChunk() && !resultFromOSS.isDelta()) {
                    return resultFromOSS.getChunk();
                }

                // read chunk result from delta cache
                if (resultFromOSS.isChunk() && resultFromOSS.isDelta()) {
                    Chunk result = resultFromOSS.getChunk();
                    if (result == null) {
                        // may impossible.
                        return null;
                    }

                    int selSize = resultFromOSS.getSelSize();
                    int[] selection = resultFromOSS.getSelection();
                    return doConsumeChunk(result, selSize, selection);
                }

                // fetch the IO results.
                VectorizedRowBatch batch = resultFromOSS.getBatch();
                if (batch == null) {
                    // IO producer has no data (waiting or finished).
                    // Driver call the is_blocked.
                    return null;
                }
                int selSize = resultFromOSS.getSelSize();
                int[] selection = resultFromOSS.getSelection();
                return doConsumeBatch(
                    batch, resultFromOSS.getOssColumnTransformer(), selSize, selection);
            }
        } finally {
            // restore the buffer to producer.
            if (resultFromOSS != null && resultFromOSS.shouldRecycle()) {
                try {
                    client.recycle(resultFromOSS);
                } catch (InterruptedException e) {
                    throw GeneralUtil.nestedException(e);
                }
            }
        }

        // client IO finished, try to pop all the result batches.
        while ((resultFromOSS = client.popResult()) != null) {
            try {
                // if there are results in a chunk(using statistics), return the result directly
                if (resultFromOSS.isChunk() && !resultFromOSS.isDelta()) {
                    return resultFromOSS.getChunk();
                }

                // read chunk result from delta cache
                if (resultFromOSS.isChunk() && resultFromOSS.isDelta()) {
                    Chunk result = resultFromOSS.getChunk();
                    if (result == null) {
                        // may impossible.
                        return null;
                    }

                    int selSize = resultFromOSS.getSelSize();
                    int[] selection = resultFromOSS.getSelection();
                    return doConsumeChunk(result, selSize, selection);
                }

                // fetch the IO results.
                VectorizedRowBatch batch = resultFromOSS.getBatch();
                if (batch == null) {
                    // impossible.
                    return null;
                }
                int selSize = resultFromOSS.getSelSize();
                int[] selection = resultFromOSS.getSelection();
                return doConsumeBatch(
                    batch, resultFromOSS.getOssColumnTransformer(), selSize, selection);
            } finally {
                // restore the buffer to producer.
                if (resultFromOSS != null && resultFromOSS.shouldRecycle()) {
                    try {
                        client.recycle(resultFromOSS);
                    } catch (InterruptedException e) {
                        throw GeneralUtil.nestedException(e);
                    }
                }
            }
        }

        // All IO data has been consumed.
        this.isFinished = true;

        return null;
    }

    @Nullable
    private Chunk doConsumeBatch(VectorizedRowBatch batch,
                                 OSSColumnTransformer ossColumnTransformer,
                                 int selSize, int[] selection) {
        Chunk chunk;
        if (condition == null) {
            // for unconditional table scan
            chunk = resultSetHandler.next(batch, ossColumnTransformer, inProjectDataTypeList, blockBuilders, context,
                selSize, selection);
        } else {
            // for conditional table scan
            chunk = resultSetHandler.next(batch, ossColumnTransformer, inProjectDataTypeList, blockBuilders, condition,
                preAllocatedChunk, filterBitmap, outProject, context, selSize, selection);
        }

        if (chunk != null) {
            // reset block builders.
            reset();
            return chunk;
        }
        return null;
    }

    private Chunk doConsumeChunk(Chunk inputChunk, int selSize, int[] selection) {
        Chunk chunk;
        if (condition == null) {
            // for unconditional table scan
            chunk = resultSetHandler.next(inputChunk, blockBuilders, context, selSize, selection);
        } else {
            // for conditional table scan
            chunk = resultSetHandler.next(inputChunk, inProjectDataTypeList, condition,
                preAllocatedChunk, filterBitmap, outProject, context, selSize, selection);
        }

        if (chunk != null) {
            // reset block builders.
            reset();
            return chunk;
        }
        return null;
    }

    @Override
    void doClose() {
        // close table client (orc task -> orc file included.)
        client.close();
        client.throwIfFailed();
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public List<DataType> getDataTypes() {
        return dataTypeList;
    }

    @Override
    public boolean produceIsFinished() {
        return isFinished;
    }

    @Override
    public ListenableFuture<?> produceIsBlocked() {
        // synchronized
        return client.isBlocked();
    }

    public VectorizedExpression getCondition() {
        return condition;
    }

    @Override
    public void setCondition(VectorizedExpression condition) {
        this.condition = condition;
    }

    public MutableChunk getPreAllocatedChunk() {
        return preAllocatedChunk;
    }

    @Override
    public void setPreAllocatedChunk(MutableChunk preAllocatedChunk) {
        this.preAllocatedChunk = preAllocatedChunk;
    }

    public int[] getFilterBitmap() {
        return filterBitmap;
    }

    @Override
    public void setFilterBitmap(int[] filterBitmap) {
        this.filterBitmap = filterBitmap;
    }

    public int[] getOutProject() {
        return outProject;
    }

    @Override
    public void setOutProject(int[] outProject) {
        this.outProject = outProject;
    }

    public List<DataType<?>> getFilterInputTypes() {
        return filterInputTypes;
    }

    @Override
    public void setFilterInputTypes(List<DataType<?>> filterInputTypes) {
        this.filterInputTypes = filterInputTypes;
    }

    public List<DataType<?>> getFilterOutputTypes() {
        return filterOutputTypes;
    }

    @Override
    public void setFilterOutputTypes(List<DataType<?>> filterOutputTypes) {
        this.filterOutputTypes = filterOutputTypes;
    }

    public synchronized void setException(TddlRuntimeException exception) {
        if (this.exception == null) {
            this.exception = exception;
        }
        isFinished = true;
    }
}
