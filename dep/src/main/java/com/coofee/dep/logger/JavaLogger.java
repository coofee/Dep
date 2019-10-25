package com.coofee.dep.logger;

import com.coofee.dep.ILog;

public class JavaLogger implements ILog {
    @Override
    public void d(String tag, String msg) {
        System.out.println(tag + ": " + msg);
    }

    @Override
    public void d(String tag, String msg, Throwable e) {
        String s = new StringBuilderWriter().append(tag).append(": ").append(msg).append(e).toString();
        System.out.println(s);
    }

    @Override
    public void e(String tag, String msg) {
        System.err.println(tag + ": " + msg);
    }

    @Override
    public void e(String tag, String msg, Throwable e) {
        String s = new StringBuilderWriter().append(tag).append(": ").append(msg).append(e).toString();
        System.err.println(s);
    }
}
