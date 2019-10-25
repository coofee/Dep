package com.coofee.dep;

public interface TaskExecutor {
    void ui(Runnable task);

    void work(Runnable task);
}
