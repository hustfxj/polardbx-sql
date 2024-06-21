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

package org.apache.calcite.sql;

import org.apache.calcite.util.EqualsContext;
import org.apache.calcite.util.Litmus;

/**
 * Created by luoyanxin.
 *
 * @author luoyanxin
 */
public class SqlPartitionValueItem {
    private final SqlNode value;
    private boolean isNull = false;
    private boolean isMaxValue = false;
    private boolean isMinValue = false;

    public SqlNode getValue() {
        return value;
    }

    public boolean isNull() {
        return isNull;
    }

    public void setNull(boolean aNull) {
        isNull = aNull;
    }

    public boolean isMaxValue() {
        return isMaxValue;
    }

    public void setMaxValue(boolean maxValue) {
        isMaxValue = maxValue;
    }

    public boolean isMinValue() {
        return isMinValue;
    }

    public void setMinValue(boolean minValue) {
        isMinValue = minValue;
    }

    public SqlPartitionValueItem(SqlNode value) {
        this.value = value;
    }

    public boolean equalsDeep(Object obj, Litmus litmus, EqualsContext context) {
        if (this == obj) {
            return true;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        SqlPartitionValueItem sqlPartValueItem = (SqlPartitionValueItem) obj;

        if (isNull != sqlPartValueItem.isNull || isMaxValue != sqlPartValueItem.isMaxValue
            || isMinValue != sqlPartValueItem.isMinValue) {
            return false;
        }

        return value.equalsDeep(sqlPartValueItem.value, litmus, context);
    }

}
