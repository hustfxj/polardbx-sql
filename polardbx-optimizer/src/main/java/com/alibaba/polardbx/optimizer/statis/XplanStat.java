package com.alibaba.polardbx.optimizer.statis;

import com.alibaba.polardbx.common.properties.ConnectionParams;
import com.alibaba.polardbx.optimizer.context.ExecutionContext;

import java.util.concurrent.atomic.AtomicLong;

public class XplanStat {
    /**
     * set the flag to be true if xplan scans too many rows
     */
    private final boolean forbidXplan;
    private AtomicLong examinedRowCount;
    private String xplanIndex;

    public XplanStat(boolean forbidXplan) {
        this.forbidXplan = forbidXplan;
        this.examinedRowCount = new AtomicLong(0);
        this.xplanIndex = null;
    }

    public void setXplanIndex(String xplanIndex) {
        this.xplanIndex = xplanIndex;
    }

    public static boolean disableXplanByFeedback(XplanStat xplanStat, ExecutionContext ec) {
        if (xplanStat == null) {
            return false;
        }
        // don't feedback when disbale xplan feedback
        if (!ec.getParamManager().getBoolean(ConnectionParams.ENABLE_XPLAN_FEEDBACK)) {
            return false;
        }
        // disable already
        if (xplanStat.forbidXplan) {
            return false;
        }
        if (xplanStat.examinedRowCount != null) {
            if ((xplanStat.examinedRowCount.get() >=
                ec.getParamManager().getLong(ConnectionParams.XPLAN_MAX_SCAN_ROWS) * 10)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isForbidXplan(XplanStat xplanStat, ExecutionContext ec) {
        if (xplanStat == null) {
            return false;
        }
        if (!ec.getParamManager().getBoolean(ConnectionParams.ENABLE_XPLAN_FEEDBACK)) {
            return false;
        }
        return xplanStat.forbidXplan;
    }

    public static String getXplanIndex(XplanStat xplanStat) {
        if (xplanStat == null) {
            return null;
        }
        return xplanStat.xplanIndex;
    }

    public static long getExaminedRowCount(XplanStat xplanStat) {
        if (xplanStat == null) {
            return -1;
        }
        if (xplanStat.examinedRowCount == null) {
            return 0;
        }
        return xplanStat.examinedRowCount.get();
    }

    public static void addExaminedRowCount(XplanStat xplanStat, long row) {
        if (xplanStat == null) {
            return;
        }
        xplanStat.examinedRowCount.getAndAdd(row);
    }
}