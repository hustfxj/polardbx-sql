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

package com.alibaba.polardbx.optimizer.core.function.calc.scalar.datatime;

import com.alibaba.polardbx.common.utils.time.MySQLTimeTypeUtil;
import com.alibaba.polardbx.common.utils.time.calculator.MySQLTimeCalculator;
import com.alibaba.polardbx.common.utils.time.core.MysqlDateTime;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.datatype.DataType;
import com.alibaba.polardbx.optimizer.core.datatype.DataTypeUtil;
import com.alibaba.polardbx.optimizer.core.function.calc.AbstractScalarFunction;
import com.alibaba.polardbx.optimizer.utils.FunctionUtils;
import com.alibaba.polardbx.optimizer.utils.TimestampUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

/**
 * NOW([fsp])
 * Returns the current date and time as a value in 'YYYY-MM-DD hh:mm:ss' or YYYYMMDDhhmmss
 * format, depending on whether the function is used in string or numeric context. The value is
 * expressed in the session time zone.
 */
public class Now extends AbstractScalarFunction {
    public Now(List<DataType> operandTypes, DataType resultType) {
        super(operandTypes, resultType);
    }

    public static String NAME = "NOW";

    @Override
    public Object compute(Object[] args, ExecutionContext ec) {
        FunctionUtils.checkFsp(args);

        // get scale
        int scale = resultType.getScale();

        // get zoned now datetime
        ZonedDateTime zonedDateTime;
        if (ec.getTimeZone() != null) {
            ZoneId zoneId = ec.getTimeZone().getZoneId();
            zonedDateTime = ZonedDateTime.now(zoneId);
        } else {
            zonedDateTime = ZonedDateTime.now();
        }

        // round to scale.
        MysqlDateTime t = MySQLTimeTypeUtil.fromZonedDatetime(zonedDateTime);
        t = MySQLTimeCalculator.roundDatetime(t, scale);
        return DataTypeUtil.fromMySQLDatetime(resultType, t, TimestampUtils.getTimeZone(ec));
    }

    @Override
    public String[] getFunctionNames() {
        return new String[] {"NOW", "CURRENT_TIMESTAMP", "LOCALTIME", "LOCALTIMESTAMP", "SYSDATE"};
    }
}
