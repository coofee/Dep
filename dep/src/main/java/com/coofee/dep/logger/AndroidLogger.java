package com.coofee.dep.logger;

import android.util.Log;

import com.coofee.dep.ILog;


public class AndroidLogger implements ILog {

    @Override
    public void d(String tag, String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void d(String tag, String msg, Throwable e) {
        Log.d(tag, msg, e);
    }

    @Override
    public void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    @Override
    public void e(String tag, String msg, Throwable e) {
        Log.e(tag, msg, e);
    }
}
