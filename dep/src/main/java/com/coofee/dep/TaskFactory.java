package com.coofee.dep;

import java.util.concurrent.Callable;

public class TaskFactory {
    public static <V> Task<V> from(String name, Callable<V> callable) {
        return new Task<>(name, callable);
    }

    public static <V> Task<V> from(String name, Callable<V> callable, boolean async) {
        return new Task<>(name, callable, async ? Task.THREAD_MODE_ASYNC : Task.THREAD_MODE_UI_BLOCK);
    }

    public static <V> Task<V> from(String name, Callable<V> callable, @Task.ThreadMode int threadMode) {
        return new Task<>(name, callable, threadMode);
    }

}
