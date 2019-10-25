package com.coofee.dep;

import android.os.Build;

public class Log {

    public static final boolean SUPPORT_TRACE = Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1;
    public static final boolean SUPPORT_TRACE_ASYNC = Build.VERSION.SDK_INT > Build.VERSION_CODES.P;
    public static boolean sDebug = false;

    private static ILog sLog = ILog.EMPTY;

    public static void setLogImpl(ILog iLog) {
        if (iLog == null) {
            return;
        }

        sLog = iLog;
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    public static void d(String tag, String msg) {
        if (sDebug) {
            sLog.d(tag, msg);
        }
    }

    public static void d(String tag, String msg, Throwable e) {
        if (sDebug) {
            sLog.d(tag, msg, e);
        }
    }

    public static void e(String tag, String msg) {
        if (sDebug) {
            sLog.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable e) {
        if (sDebug) {
            sLog.e(tag, msg, e);
        }
    }
}
