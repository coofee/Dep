[TOC]

# 0x00 Task

Task是一个抽象任务，可以运行在`ui`线程或`子线程` ，当任务之间存在依赖时，可以通过`before()、after()`等Api指定其依赖关系。



## 1. 创建Task

创建`Task`时可以通过`async`参数指定该任务运行在`ui`线程还是`子线程`；默认为false，表示运行在`ui`线程。

* 直接创建

  ```kotlin
  Task<String>("location", Callable {
    "virtual location"
  }, true) // async为true，
  
  Task<String>("imei", Callable {
    "fake imei"
  })
  ```

* 通过TaskFactory创建

  ```java
  TaskFactory.from("task_1", new Callable<String>() {
    @Override
    public String call() throws Exception {
      Thread.sleep(500);
      return "task_1.result";
    }
  }, true)
  ```

* 通过自定义策略创建，缓存Task结果。

  ```kotlin
  // 缓存任务执行结果
  class CacheServiceCreator<V>(private val callable: Callable<V>) : Callable<V> {
      @Volatile
      private var cacheValue: V? = null
  
      override fun call(): V? {
          return cacheValue ?: synchronized(this) {
              cacheValue ?: callable.call()
          }.also { cacheValue = it }
      }
  }
  
  fun <V> from(serviceName: String, async: Boolean, callable: Callable<V>): Task<V> {
      return Task<V>(serviceName, CacheServiceCreator(callable), async)
  }
  
  from<String>("location", true, Callable {
      "virtual location"
  })
  ```

  

## 2. 指定依赖关系

通过`before()、after()`等Api指定任务其依赖关系。

```java
task1.before(task2);
task1.before(task3);
task3.after(task6);
task2.before(task6);
```



## 3. 执行任务

通过调用根任务的`execute()`方法执行任务，当根任务执行完成后，会继续执行其各个子任务。

> 注意：调用`execute()`方法的任务必须是根任务，否则其前置任务无法执行。

```java
task1.execute();
```



## 4. 监听任务

通过添加`TaskExecutionListener`监听器可以获取任务执行状态。

```java
task.addTaskExecutionListener(new TaskExecutionListener() {
  @Override
  public void beforeExecute(Task task) { // 任务开始执行
    System.out.println("beforeExecute; taskName=" + task.getName());
  }

  @Override
  public void afterExecute(Task task) { // 任务执行结束
    System.err.println("afterExecute; taskName=" + task.getName() + ", taskResult=" + task.getTaskResult());
  }
});
```



## 5. 获取执行结果

获取任务执行结果有以下三种方式：

* 添加`TaskExecutionListener`监听器在回调中获取，如上所示。

* 调用`task.getTaskResult()`方法获取执行结果。该方法为非阻塞方法，若任务未执行结束，立即返回null；当任务执行结束时返回执行结果。

* 调用`task.waitForTaskResult()`方法获取执行结果，该方法会阻塞当前线程，直到该任务执行结束，返回任务执行结果。



# 0x01 TaskSet

`TaskSet`是一组任务的集合，其本身就是一个`Task`，可以指定该集合中各任务间的依赖关系，也可以通过`TaskExecutionListener`和`TaskSetExecutionListener`监听器获取`TaskSet`的执行进度。

下图描述了两组任务集合`TaskSet1`和`TaskSet2`：



## 1. 创建TaskSet

通过`TaskSet.Builder`创建`TaskSet`，通过`add`添加`Task`到`TaskSet`中，通过`Task`之间的依赖顺序。

```java
TaskSet.Builder builder = new TaskSet.Builder("TaskSet1");
        builder.add(task1).before(task2).before(task3);
        builder.add(task2).before(task6);
        builder.add(task3).after(task6);
// 创建`TaskSet1`
TaskSet taskSet = builder.build();

TaskSet taskSet2 = new TaskSet.Builder("TaskSet2")
  .add(task4).after(task7)
  .add(task5).before(task7) // 添加task5到TaskSet2中，指定其在task7之前运行。
  .add(taskSet) // 添加TaskSet1到TaskSet2中。
  .build();

taskSet2.execute();
```



## 2. 添加任务

使用`TaskSet.Builder`的`add`函数可以添加`Task`到一个`TaskSet`中。

```java
TaskSet.Builder builder = new TaskSet.Builder("TaskSet1");
builder.add(task1);
```



## 3. 指定依赖关系

由于`TaskSet`本身也是一个`Task`，指定依赖关系也是使用`before`和`after`等api实现。

```java
TaskSet taskSet = new TaskSet.Builder("TaskSet1").build();

TaskSet taskSet2 = new TaskSet.Builder("TaskSet2")
  .add(task4).after(task7)
  .add(task5).before(task7) 
  .add(taskSet).after(task4) // 添加TaskSet1到TaskSet2中，指定其在task4之后运行。
  .build();
```



## 4. 执行TaskSet

通过调用`execute()`方法执行。

> 注意：调用`execute()`方法的TaskSet必须是根任务，否则其前置任务无法执行。

```java
taskSet.execute();
```



## 5. 监听TaskSet

* `TaskExecutionListener`

  继承自`Task`，只能监听`TaskSet`自身的的执行情况。

* `TaskSetExecutionListener`

  可以同时监听`TaskSet`和`TaskSet`中任务的执行情况，如下所示：

  ```java
  taskSet.addTaskSetExecutionListener(new TaskSetExecutionListener() {
    @Override
    public void beforeExecuteTaskSet(TaskSet taskSet) { // TaskSet开始执行
      System.out.println("beforeExecuteTaskSet; name=" + taskSet.getName());
    }
  
    @Override
    public void afterExecuteTaskSet(TaskSet taskSet) { // TaskSet执行结束
      System.out.println("afterExecuteTaskSet; name=" + taskSet.getName() + ", taskResult=" + taskSet.getTaskResult());
    }
  
    @Override
    public void beforeExecute(Task task) { // TaskSet中任务开始执行
      System.out.println("beforeExecute; name=" + task.getName());
    }
  
    @Override
    public void afterExecute(Task task) { // TaskSet中的任务执行结束
      System.out.println("afterExecute; name=" + task.getName());
    }
  });
  ```

  

## 6. 获取执行结果

获取任务执行结果的接口与`Task`类似，只是它会返回所有任务的执行结果。

>  如果需要获取某一项任务的执行结果，可以在任务执行结束之后，调用`getTaskResult(String taskName)`接口获取。



# 0x03 TaskManager

为了便于管理`Task`和`TaskSet`，我们封装了`TaskManager`，它支持以下操作：

* 设置`TaskExecutor`，提供任务执行的线程池，包括`UI线程`和`工作线程`。
* `startTask(task)`执行任务。
* `getTaskResult(taskName, mode)` 同步异步获取任务执行结果，同步模式会阻塞当前线程。
* `waitForCompleted()` 阻塞当前线程等待全部的任务执行结束。
* `addConditionTask(taskCondition, task)` 添加条件任务，当特定条件产生时，触发该任务。
* `invokeCondition(taskCondition)` 收到特定条件，会触发条件任务执行。



# 0x04 Demo

例子详见`app`模块的`App.kt`和`AppServiceManager.kt`。



* 初始化

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        AppServiceManager
            .init(AndroidExecutor())
            .debug(true)
            .log(AndroidLogger())
            .apply {
                val provideConditionTask = provideConditionTask()
                for (conditionTask in provideConditionTask) {
                    addConditionServiceCreator(conditionTask.condition, conditionTask.task)
                }
            }.start(provideDemoTask())
    }
}
```

* 包装TaskManager

```kotlin
package com.coofee.dep.demo

import com.coofee.dep.*
import java.util.concurrent.Callable

object AppServiceManager {

    fun init(executor: TaskExecutor): AppServiceManager {
        TaskManager.init(executor)
        return this
    }

    fun debug(debug: Boolean): AppServiceManager {
        Log.setDebug(debug)
        return this
    }

    fun log(log: ILog): AppServiceManager {
        Log.setLogImpl(log)
        return this
    }

    fun start(service: Task<*>) {
        TaskManager.getInstance().startTask(service)
    }

    fun addConditionServiceCreator(taskCondition: TaskCondition, service: Task<*>) {
        TaskManager.getInstance().addConditionTask(taskCondition, service)
    }

    fun invokeCondition(taskCondition: TaskCondition) {
        TaskManager.getInstance().invokeCondition(taskCondition)
    }

    fun <T> getService(serviceName: String): T? {
        return TaskManager.getInstance().getTaskResult<T>(serviceName)
    }

    fun <T> getService(serviceName: String, @TaskManager.Mode mode: Int): T? {
        return TaskManager.getInstance().getTaskResult(serviceName, mode)
    }
}

abstract class ServiceCreator<V>(val serviceName: String, val async: Boolean) : Callable<V> {
    @Volatile
    private var cacheValue: V? = null

    override fun call(): V? {
        return cacheValue ?: synchronized(this) {
            cacheValue ?: onCreate()
        }.also { cacheValue = it }
    }

    abstract fun onCreate(): V?
}

fun <V> from(serviceCreator: ServiceCreator<V>): Task<V> {
    return Task<V>(serviceCreator.serviceName, serviceCreator, serviceCreator.async)
}

class CacheServiceCreator<V>(private val callable: Callable<V>) : Callable<V> {
    @Volatile
    private var cacheValue: V? = null

    override fun call(): V? {
        return cacheValue ?: synchronized(this) {
            cacheValue ?: callable.call()
        }.also { cacheValue = it }
    }
}

fun <V> from(serviceName: String, callable: Callable<V>): Task<V> {
    return Task<V>(serviceName, callable)
}

fun <V> from(serviceName: String, async: Boolean, callable: Callable<V>): Task<V> {
    return Task<V>(serviceName, CacheServiceCreator(callable), async)
}
```

