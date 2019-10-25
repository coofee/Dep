package com.coofee.dep;

public interface TaskSetExecutionListener extends TaskExecutionListener {
    void beforeExecuteTaskSet(TaskSet taskSet);

    void afterExecuteTaskSet(TaskSet taskSet);
}
