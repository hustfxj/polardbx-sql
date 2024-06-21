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

package com.alibaba.polardbx.optimizer.utils;

/**
 * @author chenghui.lch
 */
public class IntervalEndPointInfo {

    /**
     * <pre>
     * cmpDirection=true  <=>  const < col or const <= col, so const is the left end point,
     * cmpDirection=false <=>  col < const or col <= const, so const is NOT the left end point,
     *
     * includeEndPoint=true <=> const <= col or col <= const
     * includeEndPoint=false <=> const < col or col < const
     * </pre>
     */

    protected boolean cmpDirection;
    protected boolean includeEndPoint = new Boolean(true);

    public IntervalEndPointInfo() {

    }

    public IntervalEndPointInfo copy() {
        IntervalEndPointInfo newEpInfo = new IntervalEndPointInfo();
        newEpInfo.setCmpDirection(this.getCmpDirection());
        newEpInfo.setIncludeEndPoint(this.getIncludeEndPoint());
        return newEpInfo;
    }

    public boolean getCmpDirection() {
        return cmpDirection;
    }

    public void setCmpDirection(boolean cmpDirection) {
        this.cmpDirection = cmpDirection;
    }

    public boolean getIncludeEndPoint() {
        return includeEndPoint;
    }

    public void setIncludeEndPoint(boolean includeEndPoint) {
        this.includeEndPoint = includeEndPoint;
    }
}
