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

package com.alibaba.polardbx.executor.vectorized;

import com.alibaba.polardbx.common.type.MySQLStandardFieldType;
import com.alibaba.polardbx.executor.chunk.Block;
import com.alibaba.polardbx.executor.chunk.BlockUtils;
import com.alibaba.polardbx.executor.chunk.IntegerBlock;
import com.alibaba.polardbx.executor.chunk.LongBlock;
import com.alibaba.polardbx.executor.chunk.MutableChunk;
import com.alibaba.polardbx.executor.chunk.RandomAccessBlock;
import com.alibaba.polardbx.optimizer.config.table.Field;
import com.alibaba.polardbx.executor.vectorized.EvaluationContext;
import com.alibaba.polardbx.optimizer.core.datatype.DataType;
import org.apache.calcite.rex.RexLiteral;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Representation of vectorized literal expression / value
 */
public class LiteralVectorizedExpression extends AbstractVectorizedExpression implements Supplier<Object> {
    private final Object value;
    private final Object convertedValue;

    // for constant fold
    private VectorizedExpression folded;

    public LiteralVectorizedExpression(DataType<?> dataType, Object value, int outputIndex) {
        super(dataType, outputIndex, new VectorizedExpression[0]);
        this.value = value;
        this.convertedValue = dataType.convertFrom(value);
    }

    public static LiteralVectorizedExpression from(RexLiteral rexLiteral, int outputIndex) {
        Field field = new Field(rexLiteral.getType());
        return new LiteralVectorizedExpression(field.getDataType(), rexLiteral.getValue3(), outputIndex);
    }

    @Override
    public void eval(EvaluationContext ctx) {
        MutableChunk chunk = ctx.getPreAllocatedChunk();
        RandomAccessBlock outputSlot = chunk.slotIn(outputIndex);
        int batchSize = chunk.batchSize();

        // lazy allocation
        if (outputSlot == null || ((Block) outputSlot).getPositionCount() == 0) {
            int positionCount = batchSize;
            if (chunk.isSelectionInUse()) {
                positionCount = Math.max(positionCount, chunk.selection().length);
            }
            outputSlot = BlockUtils.createBlock(outputDataType, positionCount);
            outputSlot.resize(positionCount);
            chunk.setSlotAt(outputSlot, outputIndex);
        }

        // if value is null, just update the valueIsNull array and hasNull field.
        if (value == null) {
            outputSlot.setHasNull(true);
            Arrays.fill(outputSlot.nulls(), true);
            return;
        }

        MySQLStandardFieldType fieldType = outputDataType.fieldType();
        switch (fieldType) {
        case MYSQL_TYPE_LONGLONG:
            if (convertedValue instanceof Long && outputSlot instanceof LongBlock) {
                long[] longArray = ((LongBlock) outputSlot).longArray();
                long longVal = ((Long) convertedValue).longValue();
                if (chunk.isSelectionInUse()) {
                    int[] selection = chunk.selection();
                    for (int i = 0; i < batchSize; i++) {
                        longArray[selection[i]] = longVal;
                    }
                } else {
                    Arrays.fill(longArray, longVal);
                }
                return;
            }
            break;
        case MYSQL_TYPE_LONG:
            if (convertedValue instanceof Integer && outputSlot instanceof IntegerBlock) {
                int[] intArray = ((IntegerBlock) outputSlot).intArray();
                int intVal = ((Integer) convertedValue).intValue();
                if (chunk.isSelectionInUse()) {
                    int[] selection = chunk.selection();
                    for (int i = 0; i < batchSize; i++) {
                        intArray[selection[i]] = intVal;
                    }
                } else {
                    Arrays.fill(intArray, intVal);
                }
                return;
            }
            break;
        default:
            break;
        }

        if (chunk.isSelectionInUse()) {
            int[] selection = chunk.selection();
            for (int i = 0; i < batchSize; i++) {
                int idx = selection[i];
                outputSlot.setElementAt(idx, convertedValue);
            }
        } else {
            for (int i = 0; i < batchSize; i++) {
                outputSlot.setElementAt(i, convertedValue);
            }
        }
    }

    public Object getValue() {
        return value;
    }

    public Object getConvertedValue() {
        return convertedValue;
    }

    @Override
    public Object get() {
        return convertedValue;
    }

    public VectorizedExpression getFolded() {
        return folded;
    }

    public void setFolded(VectorizedExpression folded) {
        this.folded = folded;
    }
}
