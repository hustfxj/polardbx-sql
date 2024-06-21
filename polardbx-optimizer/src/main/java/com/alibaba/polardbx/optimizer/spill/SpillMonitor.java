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
package com.alibaba.polardbx.optimizer.spill;

import com.alibaba.polardbx.common.properties.FileConfig;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import static java.nio.file.Files.getFileStore;

public interface SpillMonitor extends Closeable {

    void updateBytes(long bytes);

    default LocalSpillMonitor newLocalSpillMonitor() {
        return new LocalSpillMonitor(this);
    }

    default long getTotalSpillSpace() {
        try {
            Path path = FileConfig.getInstance().getSpillerTempPath();
            return getFileStore(path).getTotalSpace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    default void close() {
    }

    String tag();
}
