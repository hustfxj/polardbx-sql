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

package com.alibaba.polardbx.executor.sync;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.polardbx.common.DefaultSchema;
import com.alibaba.polardbx.common.utils.extension.ExtensionLoader;
import com.alibaba.polardbx.gms.sync.IGmsSyncAction;
import com.alibaba.polardbx.gms.sync.ISyncResultHandler;
import com.alibaba.polardbx.gms.sync.SyncScope;
import com.alibaba.polardbx.gms.topology.SystemDbHelper;
import com.alibaba.polardbx.gms.util.DdlMetaLogUtil;

import java.util.List;
import java.util.Map;

/**
 * sync的代理类,基于extenstion查找具体实现
 *
 * @author agapple 2015年3月26日 下午5:51:31
 * @since 5.1.19
 */
public class SyncManagerHelper {

    private static ISyncManager instance;

    static {
        instance = ExtensionLoader.load(ISyncManager.class);
    }

    public static List<List<Map<String, Object>>> sync(IGmsSyncAction action, SyncScope scope) {
        return sync(action, scope, false);
    }

    public static List<List<Map<String, Object>>> syncWithDefaultDB(IGmsSyncAction action, SyncScope scope) {
        return sync(action, SystemDbHelper.DEFAULT_DB_NAME, scope, false);
    }

    public static List<List<Map<String, Object>>> sync(IGmsSyncAction action, SyncScope scope,
                                                       boolean throwExceptions) {
        DdlMetaLogUtil.DDL_META_LOG.info("sync. action:" + JSONObject.toJSONString(action));
        return sync(action, DefaultSchema.getSchemaName(), scope, throwExceptions);
    }

    public static List<List<Map<String, Object>>> sync(IGmsSyncAction action, String schema, SyncScope scope) {
        return sync(action, schema, scope, false);
    }

    public static List<List<Map<String, Object>>> sync(IGmsSyncAction action, String schema, SyncScope scope,
                                                       boolean throwExceptions) {
        return instance.sync(action, schema, scope, throwExceptions);
    }

    public static void sync(IGmsSyncAction action, String schema, SyncScope scope, ISyncResultHandler handler) {
        sync(action, schema, scope, handler, false);
    }

    public static void sync(IGmsSyncAction action, String schema, SyncScope scope, ISyncResultHandler handler,
                            boolean throwExceptions) {
        instance.sync(action, schema, scope, handler, throwExceptions);
    }

    public static List<Map<String, Object>> sync(IGmsSyncAction action, String schema, String serverKey) {
        return instance.sync(action, schema, serverKey);
    }

}
