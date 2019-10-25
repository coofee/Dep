package com.coofee.dep;

public final class TaskResult<T> {
    private final Object result;

    private TaskResult(Object result) {
        this.result = result;
    }

    public boolean isSuccess() {
        return !(result instanceof Throwable);
    }

    public boolean isFailure() {
        return result instanceof Throwable;
    }

    public T result() {
        if (isSuccess()) {
            return (T) result;
        }

        return null;
    }

    public Throwable error() {
        if (isFailure()) {
            return (Throwable) result;
        }

        return null;
    }

    @Override
    public String toString() {
        return "TaskResult{" +
                "isFailure=" + isFailure() +
                ", result=" + result +
                '}';
    }

    public static <T> TaskResult<T> success(T result) {
        return new TaskResult<>(result);
    }

    public static <T> TaskResult<T> failure(Throwable error) {
        return new TaskResult<>(error);
    }
}
