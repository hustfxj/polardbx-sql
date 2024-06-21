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

import com.alibaba.polardbx.common.jdbc.ParameterContext;
import com.alibaba.polardbx.common.jdbc.ParameterMethod;
import com.alibaba.polardbx.common.utils.GeneralUtil;
import com.alibaba.polardbx.gms.util.MetaDbUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

public class IndexesRecord extends IndexesInfoSchemaRecord {

    public static final long GLOBAL_INDEX = 1L;

    public static final long FLAG_CLUSTERED = 0x1;
    public static final long FLAG_COLUMNAR = 0x2;

    public long indexColumnType;
    public long indexLocation;
    public String indexTableName;
    public long indexStatus;
    public long version;
    public long flag;
    public long visible;
    public Long visitFrequency;
    public Date lastAccessTime;

    @Override
    public IndexesRecord fill(ResultSet rs) throws SQLException {
        super.fill(rs);
        this.indexColumnType = rs.getLong("index_column_type");
        this.indexLocation = rs.getLong("index_location");
        this.indexTableName = rs.getString("index_table_name");
        this.indexStatus = rs.getLong("index_status");
        this.version = rs.getLong("version");
        this.flag = rs.getLong("flag");
        this.visible = rs.getLong("visible");
        this.visitFrequency = rs.getLong("visit_frequency");
        this.lastAccessTime = rs.getTimestamp("last_access_time");
        return this;
    }

    @Override
    public Map<Integer, ParameterContext> buildInsertParams() {
        Map<Integer, ParameterContext> params = super.buildInsertParams();
        int index = params.size();
        MetaDbUtil.setParameter(++index, params, ParameterMethod.setLong, this.indexColumnType);
        MetaDbUtil.setParameter(++index, params, ParameterMethod.setLong, this.indexLocation);
        MetaDbUtil.setParameter(++index, params, ParameterMethod.setString, this.indexTableName);
        MetaDbUtil.setParameter(++index, params, ParameterMethod.setLong, this.indexStatus);
        MetaDbUtil.setParameter(++index, params, ParameterMethod.setLong, this.version);
        MetaDbUtil.setParameter(++index, params, ParameterMethod.setLong, this.flag);
        return params;
    }

    public boolean isClustered() {
        if ((flag & FLAG_CLUSTERED) != 0L) {
            if (indexLocation != IndexesRecord.GLOBAL_INDEX) {
                throw GeneralUtil.nestedException("Local index with clustered flag.");
            }
            return true;
        }
        return false;
    }

    public boolean isColumnar() {
        if ((flag & FLAG_COLUMNAR) != 0L) {
            if (indexLocation != IndexesRecord.GLOBAL_INDEX) {
                throw GeneralUtil.nestedException("Local index with columnar flag.");
            }
            return true;
        }
        return false;
    }

    public void setClustered() {
        flag |= FLAG_CLUSTERED;
    }

    public void setColumnar() {
        flag |= FLAG_COLUMNAR;
    }

    public void clearClustered() {
        flag &= ~FLAG_CLUSTERED;
    }

    public void clearColumnar() {
        flag &= ~FLAG_COLUMNAR;
    }
}
