package com.coofee.dep.demo

import android.app.Application
import com.coofee.dep.executor.AndroidExecutor
import com.coofee.dep.logger.AndroidLogger

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