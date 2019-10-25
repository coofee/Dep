package com.coofee.dep.executor;

import com.coofee.dep.TaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaExecutor implements TaskExecutor {

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 20;
    private static final int BACKUP_POOL_SIZE = 5;
    private static final int KEEP_ALIVE_SECONDS = 3;
    private static final Executor MAIN_THREAD;
    private static final Executor THREAD_POOL_EXECUTOR;

    static {
        MAIN_THREAD = Executors.newSingleThreadExecutor(new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Main#" + mCount.incrementAndGet());
            }
        });

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new ThreadFactory() {

            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Async#" + mCount.incrementAndGet());
            }
        });
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    @Override
    public void ui(Runnable task) {
        MAIN_THREAD.execute(task);
    }

    @Override
    public void work(Runnable task) {
        THREAD_POOL_EXECUTOR.execute(task);
    }
}
