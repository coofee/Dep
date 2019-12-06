package com.coofee.dep;

import androidx.annotation.IntDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManager {
    private static final String TAG = "Dep.TaskManager";

    public static final int MODE_BLOCK = 0;

    public static final int MODE_ASYNC = 1;

    @IntDef(value = {MODE_BLOCK, MODE_ASYNC})
    public @interface Mode {

    }

    private static volatile TaskManager sTaskManager;

    public static void init(TaskExecutor taskExecutor) {
        if (sTaskManager == null) {
            synchronized (TaskExecutor.class) {
                if (sTaskManager == null) {
                    sTaskManager = new TaskManager(taskExecutor);
                }
            }
        }
    }

    public static TaskManager getInstance() {
        return sTaskManager;
    }

    private final TaskExecutor mTaskExecutor;

    private final Map<String, Task> mTaskMap = new ConcurrentHashMap<>();

    private final Map<TaskCondition, List<Task>> mConditionTaskMap = new ConcurrentHashMap<>();

    public TaskManager(TaskExecutor mTaskExecutor) {
        this.mTaskExecutor = mTaskExecutor;
    }

    public TaskExecutor getTaskExecutor() {
        return mTaskExecutor;
    }

    public boolean startTask(Task task) {
        if (task == null || task.getName() == null) {
            Log.e(TAG, "startTask; task is null or task name is null. task=" + task + ", just return.");
            return false;
        }

        mTaskMap.put(task.getName(), task);
        Log.e(TAG, "startTask; execute task=" + task);
        task.execute();
        return true;
    }

    public boolean addConditionTask(TaskCondition condition, Task task) {
        if (condition == null || task == null || task.getName() == null) {
            Log.e(TAG, "addConditionTask; condition is null or task is null or task name is null. condition=" + condition + ", task=" + task + ", just return.");
            return false;
        }

        Log.d(TAG, "addConditionTask; condition=" + condition + ", task=" + task);
        mTaskMap.put(task.getName(), task);
        synchronized (mConditionTaskMap) {
            List<Task> taskList = mConditionTaskMap.get(condition);
            if (taskList == null) {
                taskList = new ArrayList<>();
            }
            taskList.add(task);
            mConditionTaskMap.put(condition, taskList);
        }

        return true;
    }

    public void invokeCondition(TaskCondition condition) {
        Log.d(TAG, "invokeCondition; receive condition=" + condition + " and will invoke matched task.");
        List<Task> taskList = mConditionTaskMap.get(condition);
        if (taskList == null) {
            Log.d(TAG, "invokeCondition; receive condition=" + condition + ", matched task is empty, just return.");
            return;
        }

        ArrayList<Task> copyTaskList = new ArrayList<>(taskList);
        Log.d(TAG, "invokeCondition; receive condition=" + condition + ", matched task=" + copyTaskList + ", and execute...");
        for (Task task : copyTaskList) {
            task.execute();
        }
        Log.d(TAG, "invokeCondition; receive condition=" + condition + ", matched task=" + copyTaskList + " execute done.");
    }

    public void waitForCompleted() {
        for (Map.Entry<String, Task> entry : mTaskMap.entrySet()) {
            final Task task = entry.getValue();
            try {
                task.waitForTaskResult();
            } catch (InterruptedException e) {
                Log.d(TAG, "interrupt task=" + task + " waitForTaskResult", e);
            }
        }
    }

    public <T> T getTaskResult(String taskName) {
        return getTaskResult(taskName, MODE_BLOCK);
    }

    public <T> T getTaskResult(String taskName, @TaskManager.Mode int mode) {
        Task targetTask = getTask(taskName);
        if (targetTask == null) {
            return null;
        }

        if (Task.STATE_COMPLETED == targetTask.getTaskState()) {
            TaskResult taskResult = targetTask.getTaskResult();
            Log.d(TAG, "getTaskResult; task=" + taskName + " is completed, taskResult=" + taskResult);

            if (taskResult == null) {
                return null;
            }

            return (T) taskResult.result();
        }

        if (mode == MODE_ASYNC) {
            Log.d(TAG, "getTaskResult; task=" + taskName + " is not completed and mode is async, just return.");
            return null;
        }

        try {
            Log.d(TAG, "getTaskResult; task=" + taskName + " is not completed and mode is block, so waitForTaskResult...");
            TaskResult taskResult = targetTask.waitForTaskResult();
            Log.d(TAG, "getTaskResult; task=" + taskName + " is not completed and mode is block, result=" + taskResult);

            if (taskResult != null) {
                return (T) taskResult.result();
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "interrupt task=" + targetTask + " waitForTaskResult", e);
        }

        return null;
    }

    public <T> Task<T> getTask(String taskName) {
        if (taskName == null) {
            Log.d(TAG, "getTask; taskName is null, just return.");
            return null;
        }

        Task targetTask = null;

        for (Map.Entry<String, Task> entry : mTaskMap.entrySet()) {
            Task task = entry.getValue();
            if (taskName.equals(task.getName())) {
                targetTask = task;

            } else if (task instanceof TaskSet) {
                targetTask = ((TaskSet) task).getTask(taskName);

            }

            if (targetTask != null) {
                break;
            }
        }

        if (targetTask == null) {
            Log.e(TAG, "getTask; cannot find task by name=" + taskName);
        } else {
            Log.d(TAG, "getTask; find task=" + targetTask + " by name=" + taskName);
        }

        return (Task<T>) targetTask;
    }


}
