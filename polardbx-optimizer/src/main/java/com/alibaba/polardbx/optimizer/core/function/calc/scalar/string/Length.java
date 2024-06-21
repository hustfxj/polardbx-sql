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

package com.alibaba.polardbx.optimizer.core.function.calc.scalar.string;

import com.alibaba.polardbx.common.charset.CharsetName;
import com.alibaba.polardbx.common.utils.GeneralUtil;
import com.alibaba.polardbx.common.utils.TStringUtil;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.datatype.DataType;
import com.alibaba.polardbx.optimizer.core.datatype.DataTypeUtil;
import com.alibaba.polardbx.optimizer.core.datatype.DataTypes;
import com.alibaba.polardbx.optimizer.core.datatype.SliceType;
import com.alibaba.polardbx.optimizer.core.datatype.VarcharType;
import com.alibaba.polardbx.optimizer.core.function.calc.AbstractScalarFunction;
import com.alibaba.polardbx.optimizer.utils.FunctionUtils;
import io.airlift.slice.Slice;

import java.util.List;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Optional;

/**
 * <pre>
 * Returns the length of the string str, measured in bytes.
 * A multi-byte character counts as multiple bytes.
 * This means that for a string containing five 2-byte characters, LENGTH() returns 10, whereas CHAR_LENGTH() returns 5.
 * </pre>
 *
 * @author mengshi.sunmengshi 2014年4月11日 下午5:17:48
 * @since 5.1.0
 */
public class Length extends AbstractScalarFunction {
    public Length(List<DataType> operandTypes, DataType resultType) {
        super(operandTypes, resultType);
    }

    @Override
    public String[] getFunctionNames() {
        return new String[] {"LENGTH", "OCTET_LENGTH"};
    }

    @Override
    public Object compute(Object[] args, ExecutionContext ec) {
        Object arg = args[0];

        if (FunctionUtils.isNull(arg)) {
            return null;
        }
        DataType operandType = operandTypes.get(0);
        if (operandType instanceof VarcharType
            && operandType.getCharsetName() == CharsetName.BINARY) {
            byte[] bytes = ((Slice) args[0]).getBytes();
            return bytes.length;
        }
        if (DataTypeUtil.equalsSemantically(DataTypes.BlobType, operandType)) {
            Blob blob = DataTypes.BlobType.convertFrom(args[0]);
            try {
                byte[] bytes = blob.getBytes(1, (int) blob.length());
                return bytes.length;
            } catch (SQLException e) {
                throw GeneralUtil.nestedException(e);
            }
        }
        String str = DataTypeUtil.convert(operandType, DataTypes.StringType, arg);

        return Optional.ofNullable(operandTypes)
            .map(types -> types.get(0))
            .filter(SliceType.class::isInstance)
            .map(SliceType.class::cast)
            .map(SliceType::getCharsetHandler)
            .map(charsetHandler -> charsetHandler.encodeWithReplace(str))
            .map(bs -> bs.length())
            .orElseGet(
                () -> TStringUtil.isEmpty(str) ? 0 : str.getBytes().length
            );
    }
}
