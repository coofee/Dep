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
    val threadMode = if (serviceCreator.async) Task.THREAD_MODE_ASYNC else Task.THREAD_MODE_UI_BLOCK
    return Task<V>(serviceCreator.serviceName, serviceCreator, threadMode)
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
    val threadMode = if (async) Task.THREAD_MODE_ASYNC else Task.THREAD_MODE_UI_BLOCK
    return Task<V>(serviceName, CacheServiceCreator(callable), threadMode)
}



