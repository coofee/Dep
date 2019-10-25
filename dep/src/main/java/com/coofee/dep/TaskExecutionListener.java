package com.coofee.dep;

public interface TaskExecutionListener {

    void beforeExecute(Task task);

    void afterExecute(Task task);
}
