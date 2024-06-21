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

package com.alibaba.polardbx.executor.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.polardbx.common.cdc.CdcConstants;
import com.alibaba.polardbx.common.cdc.ResultCode;
import com.alibaba.polardbx.common.exception.TddlRuntimeException;
import com.alibaba.polardbx.common.exception.code.ErrorCode;
import com.alibaba.polardbx.common.utils.HttpClientHelper;
import com.alibaba.polardbx.common.utils.logger.Logger;
import com.alibaba.polardbx.druid.util.StringUtils;
import com.alibaba.polardbx.executor.cursor.Cursor;
import com.alibaba.polardbx.executor.cursor.impl.AffectRowCursor;
import com.alibaba.polardbx.executor.spi.IRepository;
import com.alibaba.polardbx.net.util.CdcTargetUtil;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;
import com.alibaba.polardbx.optimizer.core.rel.dal.LogicalDal;
import com.alibaba.polardbx.optimizer.utils.RelUtils;
import com.alibaba.polardbx.statistics.SQLRecorderLogger;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlStopMaster;

/**
 * @author yudong
 * @since 2023/3/1 17:13
 **/
public class LogicalStopMasterHandler extends HandlerCommon {

    private static final Logger cdcLogger = SQLRecorderLogger.cdcLogger;

    public LogicalStopMasterHandler(IRepository repo) {
        super(repo);
    }

    @Override
    public Cursor handle(RelNode logicalPlan, ExecutionContext executionContext) {
        SqlStopMaster sqlStopMaster = (SqlStopMaster) ((LogicalDal) logicalPlan).getNativeSqlNode();
        SqlNode with = sqlStopMaster.getWith();
        String groupName = with == null ? "" : RelUtils.lastStringValue(with);
        String daemonEndpoint;
        if (StringUtils.isEmpty(groupName)) {
            daemonEndpoint = CdcTargetUtil.getDaemonMasterTarget();
        } else {
            daemonEndpoint = CdcTargetUtil.getDaemonMasterTarget(groupName);
        }

        String res;
        try {
            res = HttpClientHelper.doGet("http://" + daemonEndpoint + "/system/stop");
        } catch (Exception e) {
            cdcLogger.error("stop master error!", e);
            throw new RuntimeException("stop master failed", e);
        }

        ResultCode<?> resultCode = JSON.parseObject(res, ResultCode.class);
        if (resultCode.getCode() == CdcConstants.SUCCESS_CODE) {
            cdcLogger.warn("stop slave failed! code:" + resultCode.getCode() + ", msg:" + resultCode.getMsg());
            return new AffectRowCursor(0);
        } else {
            throw new TddlRuntimeException(ErrorCode.ERR_CDC_GENERIC, resultCode.getMsg());
        }
    }
}
