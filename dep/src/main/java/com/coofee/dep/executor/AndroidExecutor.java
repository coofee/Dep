package com.coofee.dep.executor;

import android.os.Handler;
import android.os.Looper;

import com.coofee.dep.Log;
import com.coofee.dep.TaskExecutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AndroidExecutor implements TaskExecutor {

    private static final Looper MAIN_LOOPER = Looper.getMainLooper();
    private static final Handler UI_THREAD = new Handler(MAIN_LOOPER);

    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;

    static {
        final int coreProcessor = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreProcessor, coreProcessor,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    private final AtomicInteger mCount = new AtomicInteger();

                    @Override
                    public Thread newThread(Runnable r) {
                        Log.d("Dep.AndroidExecutor", "coreProcessor=" + coreProcessor);
                        return new Thread(r, "DepThread#" + mCount.incrementAndGet());
                    }
                });
        executor.allowCoreThreadTimeOut(true);

        THREAD_POOL_EXECUTOR = executor;
    }

    @Override
    public void ui(Runnable task) {
        if (task == null) {
            return;
        }

        UI_THREAD.post(task);
    }

    @Override
    public void work(Runnable task) {
        THREAD_POOL_EXECUTOR.execute(task);
    }
}
