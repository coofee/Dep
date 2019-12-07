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

    public static final int THREAD_MODE_UI_BLOCK = 0;
    public static final int THREAD_MODE_UI_ENQUEUE = 1;
    public static final int THREAD_MODE_UI_IDLE = 2;
    public static final int THREAD_MODE_ASYNC = 3;

    @IntDef(value = {THREAD_MODE_UI_BLOCK, THREAD_MODE_UI_ENQUEUE, THREAD_MODE_UI_IDLE, THREAD_MODE_ASYNC})
    @interface ThreadMode {

    }

    private final String mName;
    private final Callable<V> mCallable;
    private final int mThreadMode;
    private final CopyOnWriteArrayList<TaskExecutionListener> mListenerList = new CopyOnWriteArrayList<>();

    private volatile int mTaskState = STATE_NEW;

    private volatile TaskResult<V> mTaskResult;

    private volatile ParentTaskError mParentTaskError;

    public Task(String name, Callable<V> callable) {
        this(name, callable, THREAD_MODE_UI_BLOCK);
    }

    public Task(String name, Callable<V> callable, int threadMode) {
        this.mName = name;
        this.mCallable = callable;
        this.mThreadMode = threadMode;
    }

    @Override
    public String toString() {
        return "Task{" +
                "name='" + mName + '\'' +
                ", threadMode=" + mThreadMode +
                ", taskState=" + mTaskState +
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
        TaskManager.getInstance().getTaskExecutor().execute(mThreadMode, mInnerTask);
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
        synchronized (this) {
            if (mTaskState != STATE_NEW) {
                return;
            }
        }

        fireBeforeExecute();

        Log.d(TAG, "start execute task=" + mName);
        if (Log.sDebug && Log.SUPPORT_TRACE) {
            Trace.beginSection(mName);
        }

        final ParentTaskError parentTaskError;
        synchronized (this) {
            mTaskResult = null;
            mTaskState = STATE_RUNNING;
            parentTaskError = mParentTaskError;
        }

        TaskResult<V> taskResult;
        try {
            if (parentTaskError == null) {
                taskResult = TaskResult.success(this.mCallable.call());
            } else {
                // fail fast when parent task execute error.
                Log.e(TAG, parentTaskError.getMessage());
                taskResult = TaskResult.failure(parentTaskError);
            }
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
        Log.d(TAG, "end execute task=" + mName);

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

        final TaskResult parentTaskResult = parentTask.mTaskResult;
        if (parentTaskResult.isFailure()) {
            // fail fast.
            // it is difficult to cancel all incomplete parent task,
            // just clear them and they will continue execute.
            mParentTaskList.clear();

            synchronized (this) {
                if (mParentTaskError == null) {
                    // only save first parent task error.
                    final String cause = "fail execute task=" + mName + " caused by " + parentTask.mName;
                    mParentTaskError = new ParentTaskError(cause, parentTaskResult.error());
                }
            }

        }

        if (mParentTaskList.isEmpty()) {
            execute();
        }
    }

    public static class ParentTaskError extends Exception {
        public ParentTaskError(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
