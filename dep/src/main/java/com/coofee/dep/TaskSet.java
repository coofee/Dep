package com.coofee.dep;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskSet extends Task {

    private static final Callable<Object> EMPTY = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
            return true;
        }
    };

    private final Task mRootTask;

    private final Map<String, Task> mAllTask;

    private final Task mEndTask;

    private final CopyOnWriteArrayList<TaskSetExecutionListener> mTaskSetListenerList = new CopyOnWriteArrayList<>();

    private TaskSet(String name, Task rootTask, Map<String, Task> allTask, Task endTask) {
        super(name, EMPTY);
        this.mRootTask = rootTask;
        this.mAllTask = allTask;
        this.mEndTask = endTask;
        registerTaskListeners();
    }

    public boolean addTaskSetExecutionListener(TaskSetExecutionListener listener) {
        if (listener == null) {
            return false;
        }
        return mTaskSetListenerList.add(listener);
    }

    public boolean removeTaskSetExecutionListener(TaskSetExecutionListener listener) {
        if (listener == null) {
            return false;
        }
        return mTaskSetListenerList.remove(listener);
    }

    public void removeAllTaskSetExecutionListener() {
        mTaskSetListenerList.clear();
    }

    public void execute() {
        mRootTask.execute();
    }

    public TaskResult<?> getTaskResult(String taskName) {
        Task<?> task = mAllTask.get(taskName);
        return (task == null ? null : task.getTaskResult());
    }

    public Task<?> getTask(String taskName) {
        return mAllTask.get(taskName);
    }

    @Override
    public TaskResult<Map<String, TaskResult<?>>> waitForTaskResult() throws InterruptedException {
        synchronized (this) {
            while (getTaskState() != Task.STATE_COMPLETED) {
                wait();
            }

            return getTaskResult();
        }
    }

    @Override
    public TaskResult<Map<String, TaskResult<?>>> getTaskResult() {
        if (getTaskState() != Task.STATE_COMPLETED) {
            return null;
        }

        // get result from mAllTask and return.
        final Map<String, TaskResult<?>> resultMap = new HashMap<>(mAllTask.size());
        for (Map.Entry<String, Task> entry : mAllTask.entrySet()) {
            resultMap.put(entry.getKey(), entry.getValue().getTaskResult());
        }

        return TaskResult.success(resultMap);
    }

    @Override
    public synchronized int getTaskState() {
        if (mRootTask.getTaskState() == Task.STATE_NEW) {
            return Task.STATE_NEW;
        } else if (mEndTask.getTaskState() == Task.STATE_COMPLETED) {
            return Task.STATE_COMPLETED;
        } else {
            return Task.STATE_RUNNING;
        }
    }

    boolean before(Task task) {
        if (task == null) {
            return false;
        }

        return mEndTask.before(task);
    }

    boolean after(Task task) {
        if (task == null) {
            return false;
        }

        return mRootTask.after(task);
    }

    Set<Task> getParentTasks() {
        return mRootTask.getParentTasks();
    }

    Set<Task> getChildTasks() {
        return mEndTask.getChildTasks();
    }

    private void registerTaskListeners() {
        this.mRootTask.addTaskExecutionListener(new TaskExecutionListener() {
            @Override
            public void beforeExecute(Task task) {
                fireBeforeExecute();

                for (TaskSetExecutionListener listener : mTaskSetListenerList) {
                    listener.beforeExecuteTaskSet(TaskSet.this);
                }
            }

            @Override
            public void afterExecute(Task task) {

            }
        });

        this.mEndTask.addTaskExecutionListener(new TaskExecutionListener() {
            @Override
            public void beforeExecute(Task task) {

            }

            @Override
            public void afterExecute(Task task) {
                fireAfterExecute();
                synchronized (TaskSet.this) {
                    TaskSet.this.notifyAll();
                }

                for (TaskSetExecutionListener listener : mTaskSetListenerList) {
                    listener.afterExecuteTaskSet(TaskSet.this);
                }

            }
        });

        final TaskExecutionListener allTaskExecutionListener = new TaskExecutionListener() {
            @Override
            public void beforeExecute(Task task) {
                for (TaskSetExecutionListener listener : mTaskSetListenerList) {
                    listener.beforeExecute(task);
                }
            }

            @Override
            public void afterExecute(Task task) {
                for (TaskSetExecutionListener listener : mTaskSetListenerList) {
                    listener.afterExecute(task);
                }
            }
        };
        for (Task task : mAllTask.values()) {
            task.addTaskExecutionListener(allTaskExecutionListener);
        }
    }

    public static class Builder {

        private String name;

        private Task currentTask;

        private final Map<String, Task> allTask = new HashMap<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder add(Task task) {
            allTask.put(task.getName(), task);
            currentTask = task;
            return this;
        }

        public Builder after(Task task) {
            allTask.put(task.getName(), task);
            if (currentTask != null) {
                currentTask.after(task);
            }
            return this;
        }

        public Builder before(Task task) {
            allTask.put(task.getName(), task);
            if (currentTask != null) {
                currentTask.before(task);
            }
            return this;
        }

        public TaskSet build() {
            final Task rootTask = TaskFactory.from(name + ".root", EMPTY);
            final Task endTask = TaskFactory.from(name + ".end", EMPTY);
            final Map<String, Task> taskSetAllTasks = new HashMap<>();
            for (Map.Entry<String, Task> entry : allTask.entrySet()) {
                final Task task = entry.getValue();
                if (task instanceof TaskSet) {
                    final TaskSet taskSet = (TaskSet) task;
                    if (taskSet.getParentTasks().isEmpty()) {
                        rootTask.before(taskSet.mRootTask);
                    }

                    if (taskSet.getChildTasks().isEmpty()) {
                        endTask.after(taskSet.mEndTask);
                    }

                    taskSetAllTasks.putAll(taskSet.mAllTask);

                } else {
                    if (task.getParentTasks().isEmpty()) {
                        rootTask.before(task);
                    }

                    if (task.getChildTasks().isEmpty()) {
                        endTask.after(task);
                    }
                }
            }

            allTask.putAll(taskSetAllTasks);
            return new TaskSet(name, rootTask, allTask, endTask);
        }
    }
}
