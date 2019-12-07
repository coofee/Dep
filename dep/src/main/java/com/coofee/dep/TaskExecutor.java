package com.coofee.dep;

public interface TaskExecutor {
    void execute(@Task.ThreadMode int threadMode, Runnable task);
}
