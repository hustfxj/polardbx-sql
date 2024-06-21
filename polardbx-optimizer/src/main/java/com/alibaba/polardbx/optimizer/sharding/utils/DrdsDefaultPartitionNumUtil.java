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

package com.alibaba.polardbx.optimizer.sharding.utils;

import com.alibaba.polardbx.common.exception.TddlNestableRuntimeException;
import com.alibaba.polardbx.gms.topology.ServerInstIdManager;
import com.alibaba.polardbx.gms.topology.StorageInfoAccessor;
import com.alibaba.polardbx.gms.topology.StorageInfoRecord;
import com.alibaba.polardbx.gms.util.MetaDbUtil;

import java.sql.Connection;

/**
 * Created by zhuqiwei.
 *
 * @author zhuqiwei
 */
public class DrdsDefaultPartitionNumUtil {
    private static Integer dbPartitionsDefault = null;
    final private static Integer tbPartitionsDefault = 1;

    private DrdsDefaultPartitionNumUtil() {
    }

    public static synchronized int getDrdsDefaultDbPartitionNum() {
        if (dbPartitionsDefault != null) {
            return dbPartitionsDefault;
        } else {
            try (Connection metaDbConn = MetaDbUtil.getConnection()) {
                StorageInfoAccessor storageInfoAccessor =
                    new StorageInfoAccessor();
                storageInfoAccessor.setConnection(metaDbConn);
                int rwDnNum = storageInfoAccessor.getStorageIdListByInstIdAndInstKind(
                    ServerInstIdManager.getInstance().getMasterInstId()
                    , StorageInfoRecord.INST_KIND_MASTER).size();
                dbPartitionsDefault = 8 * rwDnNum;
                return dbPartitionsDefault;
            } catch (Exception e) {
                throw new TddlNestableRuntimeException("failed to query metaDb", e);
            }
        }
    }

    public static synchronized int getDrdsDefaultTbPartitionNum() {
        return tbPartitionsDefault;
    }
}
