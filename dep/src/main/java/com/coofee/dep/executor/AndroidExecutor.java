package com.coofee.dep.executor;

import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;

import com.coofee.dep.Log;
import com.coofee.dep.Task;
import com.coofee.dep.TaskExecutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AndroidExecutor implements TaskExecutor {

    private static final Looper MAIN_LOOPER = Looper.getMainLooper();
    private static final Handler UI_THREAD = new Handler(MAIN_LOOPER);
    private static final MessageQueue MAIN_MESSAGE_QUEUE = Looper.myQueue();

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
    public void execute(int threadMode, final Runnable task) {
        switch (threadMode) {
            case Task.THREAD_MODE_UI_BLOCK: {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    task.run();
                } else {
                    UI_THREAD.post(task);
                }
            }
            break;

            case Task.THREAD_MODE_UI_ENQUEUE: {
                UI_THREAD.post(task);
            }
            break;

            case Task.THREAD_MODE_UI_IDLE: {
                MAIN_MESSAGE_QUEUE.addIdleHandler(new MessageQueue.IdleHandler() {
                    @Override
                    public boolean queueIdle() {
                        task.run();
                        return false;
                    }
                });
            }
            break;

            case Task.THREAD_MODE_ASYNC: {
                THREAD_POOL_EXECUTOR.execute(task);
            }
            break;

            default:
                // ignore unknown thread mode
                break;
        }
    }
}
