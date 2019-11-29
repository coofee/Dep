package com.coofee.dep;

import com.coofee.dep.executor.JavaExecutor;
import com.coofee.dep.logger.JavaLogger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Callable;

public class TaskTest {

    public Task<?> task1, task2, task3, task4, task5, task6, task7;

    @BeforeClass
    public static void beforeClass() {
        TaskManager.init(new JavaExecutor());
        Log.setDebug(true);
        Log.setLogImpl(new JavaLogger());
    }

    @Before
    public void before() {
        task1 = wrapMonitor(TaskFactory.from("task_1", new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(500);
                return "task_1.result";
            }
        }, true));

        task2 = wrapMonitor(TaskFactory.from("task_2", new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(500);
                return "task_2.result";
            }
        }));

        task3 = wrapMonitor(TaskFactory.from("task_3", new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(500);
                return "task_3.result";
            }
        }, true));

        task4 = wrapMonitor(TaskFactory.from("task_4", new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(500);
                return "task_4.result";
            }
        }));

        task5 = wrapMonitor(TaskFactory.from("task_5", new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(500);
                return "task_5.result";
            }
        }, true));

        task6 = wrapMonitor(TaskFactory.from("task_6", new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(500);
                return "task_6.result";
            }
        }));

        task7 = wrapMonitor(TaskFactory.from("task_7", new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(500);
                return "task_7.result";
            }
        }, true));
    }

    @Test
    public void testTask() throws InterruptedException {
        task1.before(task2);
        task1.before(task3);
//        task1.before(task4);
        task3.after(task6);
//        task1.before(task5);
        task2.before(task6);
//        task2.before(task7);

        task1.execute();
        task1.waitForTaskResult();
        task2.waitForTaskResult();
        task3.waitForTaskResult();
        task6.waitForTaskResult();
    }

    @Test
    public void testTaskSet() throws InterruptedException {
        TaskSet.Builder builder = new TaskSet.Builder("TaskSet1");
        builder.add(task1).before(task2).before(task3);
        builder.add(task2).before(task6);
        builder.add(task3).after(task6);
        TaskSet taskSet = builder.build();
        wrapTaskSetMonitor(taskSet);
        taskSet.execute();
        taskSet.waitForTaskResult();
    }

    @Test
    public void testTaskSetBefore() throws InterruptedException {
        TaskSet.Builder builder = new TaskSet.Builder("TaskSet1");
        builder.add(task1).before(task2).before(task3);
        builder.add(task2).before(task6);
        builder.add(task3).after(task6);
        TaskSet taskSet = wrapTaskSetMonitor(builder.build());

        TaskSet taskSet2 = new TaskSet.Builder("TaskSet2")
                .add(task4).after(task7)
                .add(task5).before(task7)
                .add(taskSet).before(task4)
                .build();

        wrapTaskSetMonitor(taskSet2);
        taskSet2.execute();

        taskSet2.waitForTaskResult();
    }

    @Test
    public void testTaskSetAfter() throws InterruptedException {
        TaskSet.Builder builder = new TaskSet.Builder("TaskSet1");
        builder.add(task1).before(task2).before(task3);
        builder.add(task2).before(task6);
        builder.add(task3).after(task6);
        TaskSet taskSet = wrapTaskSetMonitor(builder.build());

        TaskSet taskSet2 = new TaskSet.Builder("TaskSet2")
                .add(task4).after(task7)
                .add(task5).before(task7)
                .add(taskSet).after(task4)
                .build();

        wrapTaskSetMonitor(taskSet2);
        taskSet2.execute();

        taskSet2.waitForTaskResult();
    }

    @Test
    public void testTaskSetJustAdd() throws InterruptedException {
        TaskSet.Builder builder = new TaskSet.Builder("TaskSet1");
        builder.add(task1).before(task2).before(task3);
        builder.add(task2).before(task6);
        builder.add(task3).after(task6);
        TaskSet taskSet = wrapTaskSetMonitor(builder.build());

        TaskSet taskSet2 = new TaskSet.Builder("TaskSet2")
                .add(task4).after(task7)
                .add(task5).before(task7)
                .add(taskSet)
                .build();

        wrapTaskSetMonitor(taskSet2);
        taskSet2.execute();

        taskSet2.waitForTaskResult();
    }

    @Test
    public void testTaskManager() {
        TaskSet.Builder builder = new TaskSet.Builder("TaskSet1");
        builder.add(task1).before(task2).before(task3);
        builder.add(task2).before(task6);
        builder.add(task3).after(task6);
        TaskSet taskSet = builder.build();
        wrapTaskSetMonitor(taskSet);

        TaskManager.getInstance().startTask(taskSet);
        String task3Result = TaskManager.getInstance().getTaskResult(task3.getName());
        System.out.println("getTaskResult(); task3Result=" + task3Result);

        TaskSet.TaskSetResult result = TaskManager.getInstance().getTaskResult(taskSet.getName());
        System.out.println("getTaskResult(); TaskSet1=" + result);
    }

    @Test
    public void testWhenCondition() {
        final TaskCondition taskCondition = new TaskCondition() {
        };

        TaskManager.getInstance().addConditionTask(taskCondition, task5);
        TaskManager.getInstance().addConditionTask(taskCondition, task7);
        TaskManager.getInstance().addConditionTask(taskCondition, task2);
        TaskSet taskSet1 = new TaskSet.Builder("TaskSet1").add(task3).add(task6).build();
        TaskManager.getInstance().addConditionTask(taskCondition, taskSet1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("condition occurs...");
                TaskManager.getInstance().invokeCondition(taskCondition);
            }
        }).start();

        TaskSet.Builder builder = new TaskSet.Builder("TaskSet1");
        builder.add(task1).before(task4);
        TaskSet taskSet = builder.build();
        wrapTaskSetMonitor(taskSet);

        TaskManager.getInstance().startTask(taskSet);

        TaskManager.getInstance().waitForCompleted();
    }

    @Test
    public void testParentTaskError() throws InterruptedException {
        task2 = wrapMonitor(TaskFactory.from("task_2", new Callable<String>() {
            @Override
            public String call() throws Exception {
                throw new Exception("parent task error");
            }
        }, true));

//        testTaskSet();
        testTaskSetJustAdd();
//        testTaskSetAfter();
//        testTaskSetBefore();
//        testTaskManager();

//        try {
//            System.in.read();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private static TaskSet wrapTaskSetMonitor(TaskSet taskSet) {
        taskSet.addTaskSetExecutionListener(new TaskSetExecutionListener() {
            @Override
            public void beforeExecuteTaskSet(TaskSet taskSet) {
                System.out.println("beforeExecuteTaskSet; name=" + taskSet.getName());
            }

            @Override
            public void afterExecuteTaskSet(TaskSet taskSet) {
                System.out.println("afterExecuteTaskSet; name=" + taskSet.getName() + ", taskResult=" + taskSet.getTaskResult());
            }

            @Override
            public void beforeExecute(Task task) {
                System.out.println("beforeExecute; name=" + task.getName());
            }

            @Override
            public void afterExecute(Task task) {
                System.out.println("afterExecute; name=" + task.getName());
            }
        });

        return taskSet;
    }

    private static Task wrapMonitor(Task task) {
//        task.addTaskExecutionListener(new TaskExecutionListener() {
//            @Override
//            public void beforeExecute(Task task) {
//                System.out.println("beforeExecute; taskName=" + task.getName());
//            }
//
//            @Override
//            public void afterExecute(Task task) {
//                System.err.println("afterExecute; taskName=" + task.getName() + ", taskResult=" + task.getTaskResult());
//            }
//        });
        return task;
    }
}
