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

package com.alibaba.polardbx.gms.metadb.table;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JavaFunctionRecord extends JavaFunctionMetaRecord {

    public String className;
    public String code;
    public String codeLanguage;

    @Override
    public JavaFunctionRecord fill(ResultSet rs) throws SQLException {
        this.funcName = rs.getString("function_name");
        this.className = rs.getString("class_name");
        this.code = rs.getString("code");
        this.codeLanguage = rs.getString("code_language");
        this.inputTypes = rs.getString("input_types");
        this.returnType = rs.getString("return_type");
        this.noState = Optional.of(rs.getBoolean("is_no_state"))
            .orElse(false);
        return this;
    }
}
