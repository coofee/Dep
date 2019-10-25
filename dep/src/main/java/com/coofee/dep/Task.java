package com.coofee.dep;

import android.os.Trace;

import androidx.annotation.IntDef;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class Task<V> {
    private static final String TAG = "Dep.Task";

    public static final int STATE_NEW = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_COMPLETED = 2;

    @IntDef(value = {STATE_NEW, STATE_RUNNING, STATE_COMPLETED})
    @interface TaskState {
    }

    private final String mName;
    private final Callable<V> mCallable;
    private final boolean mAsync;
    private final CopyOnWriteArrayList<TaskExecutionListener> mListenerList = new CopyOnWriteArrayList<>();

    private volatile int mTaskState = STATE_NEW;

    private volatile TaskResult<V> mTaskResult;

    public Task(String name, Callable<V> callable) {
        this(name, callable, false);
    }

    public Task(String name, Callable<V> callable, boolean async) {
        this.mName = name;
        this.mCallable = callable;
        this.mAsync = async;
    }

    @Override
    public String toString() {
        return "Task{" +
                "mName='" + mName + '\'' +
                ", mAsync=" + mAsync +
                ", mTaskState=" + mTaskState +
                '}';
    }

    public String getName() {
        return this.mName;
    }

    public boolean addTaskExecutionListener(TaskExecutionListener listener) {
        if (listener == null) {
            return false;
        }
        return mListenerList.add(listener);
    }

    public boolean removeTaskExecutionListener(TaskExecutionListener listener) {
        if (listener == null) {
            return false;
        }
        return mListenerList.remove(listener);
    }

    public void removeAllTaskExecutionListener() {
        mListenerList.clear();
    }

    public synchronized TaskResult<V> getTaskResult() {
        return mTaskResult;
    }

    public TaskResult<V> waitForTaskResult() throws InterruptedException {
        synchronized (this) {
            while (getTaskState() != Task.STATE_COMPLETED) {
                wait();
            }

            return mTaskResult;
        }
    }

    @TaskState
    public synchronized int getTaskState() {
        return mTaskState;
    }

    public void execute() {
        TaskExecutor taskExecutor = TaskManager.getInstance().getTaskExecutor();
        if (mAsync) {
            taskExecutor.work(mInnerTask);
        } else {
            taskExecutor.ui(mInnerTask);
        }
    }

    protected void fireBeforeExecute() {
        for (TaskExecutionListener listener : mListenerList) {
            listener.beforeExecute(this);
        }
    }

    protected void fireAfterExecute() {
        for (TaskExecutionListener listener : mListenerList) {
            listener.afterExecute(this);
        }
    }

    private final Runnable mInnerTask = new Runnable() {
        @Override
        public void run() {
            innerExecute();
        }
    };

    private void innerExecute() {
        fireBeforeExecute();

        Log.d(TAG, "start execute task=" + this);
        if (Log.sDebug && Log.SUPPORT_TRACE) {
            Trace.beginSection(mName);
        }

        synchronized (this) {
            mTaskResult = null;
            mTaskState = STATE_RUNNING;
        }

        TaskResult<V> taskResult;
        try {
            taskResult = TaskResult.success(this.mCallable.call());
        } catch (Throwable e) {
            Log.e(TAG, "fail execute task=" + mName, e);
            taskResult = TaskResult.failure(e);
        }

        synchronized (this) {
            mTaskResult = taskResult;
            mTaskState = STATE_COMPLETED;
            notifyAll();
        }

        if (Log.sDebug && Log.SUPPORT_TRACE) {
            Trace.endSection();
        }
        Log.d(TAG, "end execute task=" + this);

        fireAfterExecute();

        // try execute child task;
        if (!mChildTaskList.isEmpty()) {
            for (Task task : mChildTaskList) {
                task.onParentTaskFinished(this);
            }
        }
    }

    private final CopyOnWriteArraySet<Task> mParentTaskList = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArraySet<Task> mChildTaskList = new CopyOnWriteArraySet<>();

    boolean before(Task task) {
        if (task == null) {
            return false;
        }

        task.mParentTaskList.add(this);
        return mChildTaskList.add(task);
    }

    boolean after(Task task) {
        if (task == null) {
            return false;
        }

        task.mChildTaskList.add(this);
        return mParentTaskList.add(task);
    }

    Set<Task> getParentTasks() {
        return mParentTaskList;
    }

    Set<Task> getChildTasks() {
        return mChildTaskList;
    }

    private void onParentTaskFinished(Task parentTask) {
        mParentTaskList.remove(parentTask);

        if (mParentTaskList.isEmpty()) {
            execute();
        }
    }
}
