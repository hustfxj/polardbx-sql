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

package com.alibaba.polardbx.optimizer.core.expression.calc.aggfunctions;

import com.alibaba.polardbx.common.datatype.Decimal;
import com.alibaba.polardbx.optimizer.core.datatype.DataType;
import com.alibaba.polardbx.optimizer.core.datatype.DataTypes;
import com.alibaba.polardbx.optimizer.core.expression.IFunction;
import com.alibaba.polardbx.optimizer.core.expression.calc.Aggregator;
import com.alibaba.polardbx.optimizer.core.row.Row;
import com.alibaba.polardbx.optimizer.memory.MemoryAllocatorCtx;
import org.apache.calcite.sql.SqlKind;

import java.math.BigDecimal;

import static org.apache.calcite.sql.SqlKind.SUM0;

public class Sum0 extends Aggregator {

    private Object sum = new Decimal(0, 0);

    public Sum0() {
    }

    public Sum0(int targetIndex, boolean isDistinct, MemoryAllocatorCtx allocator, int filterArg) {
        super(new int[] {targetIndex}, isDistinct, allocator, filterArg);
    }

    public void setSum(BigDecimal sum) {
        this.sum = sum;
    }

    @Override
    public SqlKind getSqlKind() {
        return SUM0;
    }

    @Override
    protected void conductAgg(Object value) {
        if (sum == null) {
            sum = new Decimal(0, 0);
        }
        sum = getReturnType().getCalculator().add(sum, value);
    }

    @Override
    public Aggregator getNew() {
        return new Sum0(aggTargetIndexes[0], isDistinct, memoryAllocator, filterArg);
    }

    @Override
    public Object eval(Row row) {
        return sum;
    }

    @Override
    public void setFunction(IFunction function) {

    }

    @Override
    public IFunction.FunctionType getFunctionType() {
        return IFunction.FunctionType.Aggregate;
    }

    @Override
    public Object compute() {
        return null;
    }

    @Override
    public DataType getReturnType() {
        return DataTypes.DecimalType;
    }

    @Override
    public String[] getFunctionNames() {
        return new String[] {"SUM0"};
    }

    @Override
    public void clear() {

    }

    @Override
    public int getScale() {
        return 0;
    }

    @Override
    public int getPrecision() {
        return 0;
    }
}
