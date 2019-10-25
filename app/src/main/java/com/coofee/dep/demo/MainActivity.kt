package com.coofee.dep.demo

import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.coofee.dep.TaskManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        invokeConditionPermissionLocation.setOnClickListener {
            AppServiceManager.invokeCondition(CONDITION_PERMISSION_LOCATION)
        }
        invokeConditionLaunchAd.setOnClickListener {
            AppServiceManager.invokeCondition(CONDITION_LAUNCH_AD_SHOW)
        }
        invokeConditionLaunchShow.setOnClickListener {
            AppServiceManager.invokeCondition(CONDITION_LAUNCH_SHOW)
        }
        invokeConditionDatabase.setOnClickListener {
            AppServiceManager.invokeCondition(CONDITION_DATABASE)
        }

        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val task8Service = AppServiceManager.getService<Task8>("task_8")
            val result = task8Service?.methodTask8()
            println("Dep.MainActivity; get task_8 service ${Thread.currentThread()} task8Service?.methodTask8()=$result")
        }

        val task8Service = AppServiceManager.getService<Task8>("task_8", TaskManager.MODE_ASYNC)
        println("Dep.MainActivity; get task_8 service from ${Thread.currentThread()};  task8Service=$task8Service")
    }
}
