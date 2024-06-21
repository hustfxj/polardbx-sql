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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.polardbx.gms.node;

import java.net.URI;

public interface Node {
    HostAddress getHostAndPort();

    URI getHttpUri();

    String getCluster();

    String getInstId();

    String getNodeIdentifier();

    String getVersion();

    boolean isLeader();

    void setLeader(boolean leader);

    boolean isCoordinator();

    boolean isWorker();

    boolean isHtap();

    boolean isInBlacklist();

    void setInBlacklist(boolean inBlacklist);

    NodeServer getNodeServer();

    String getHost();

    int getPort();

    int getRpcPort();

    String getHostPort();
}
