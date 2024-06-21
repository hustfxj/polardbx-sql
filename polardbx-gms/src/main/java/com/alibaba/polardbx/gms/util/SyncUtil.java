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

package com.alibaba.polardbx.gms.util;

import com.alibaba.polardbx.gms.node.GmsNodeManager;

/**
 * @author fangwu
 */
public class SyncUtil {

    /**
     * Choose the node which has the smallest id
     */
    public static boolean isNodeWithSmallestId() {
        GmsNodeManager gmsNodeManager = GmsNodeManager.getInstance();

        // Choose the node whose has the minimum id
        GmsNodeManager.GmsNode node = gmsNodeManager.getLocalNode();
        if (node == null) {
            return true;
        }
        long localNodeId = node.origId;
        for (GmsNodeManager.GmsNode remoteNode : gmsNodeManager.getRemoteNodes()) {
            if (localNodeId > remoteNode.origId) {
                return false;
            }
        }

        return true;
    }
}
