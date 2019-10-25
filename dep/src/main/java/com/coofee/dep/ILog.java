package com.coofee.dep;

public interface ILog {
    ILog EMPTY = new ILog() {
        @Override
        public void d(String tag, String msg) {

        }

        @Override
        public void d(String tag, String msg, Throwable e) {

        }

        @Override
        public void e(String tag, String msg) {

        }

        @Override
        public void e(String tag, String msg, Throwable e) {

        }
    };

    void d(String tag, String msg);

    void d(String tag, String msg, Throwable e);

    void e(String tag, String msg);

    void e(String tag, String msg, Throwable e);
}
