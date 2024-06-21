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
package com.alibaba.polardbx.executor.mpp.planner;

import com.alibaba.polardbx.executor.mpp.split.SplitInfo;
import com.google.common.collect.ImmutableList;

import javax.annotation.concurrent.Immutable;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Immutable
public class SubPlan {
    private final PlanFragment fragment;
    private final List<SplitInfo> logicalViewInfos;
    private final List<SplitInfo> expandViewInfos;
    private final List<SubPlan> children;

    public SubPlan(
        PlanFragment fragment,
        List<SplitInfo> logicalViewInfos,
        List<SplitInfo> expandViewInfos,
        List<SubPlan> children) {
        requireNonNull(fragment, "fragment is null");
        requireNonNull(children, "children is null");

        this.fragment = fragment;
        this.logicalViewInfos = logicalViewInfos;
        this.expandViewInfos = expandViewInfos;
        this.children = ImmutableList.copyOf(children);
    }

    public PlanFragment getFragment() {
        return fragment;
    }

    public List<SubPlan> getChildren() {
        return children;
    }

    public List<SplitInfo> getLogicalViewInfos() {
        return logicalViewInfos;
    }

    public List<SplitInfo> getExpandSplitInfos() {
        return expandViewInfos;
    }

    /**
     * Flattens the subplan and returns all PlanFragments in the tree
     */
    public List<PlanFragment> getAllFragments() {
        ImmutableList.Builder<PlanFragment> fragments = ImmutableList.builder();

        fragments.add(getFragment());
        if (logicalViewInfos != null) {
            getFragment().setAllSplitNums(logicalViewInfos.stream().mapToInt(SplitInfo::getSplitCount).sum());
        }
        for (SubPlan child : getChildren()) {
            fragments.addAll(child.getAllFragments());
        }

        return fragments.build();
    }
}
