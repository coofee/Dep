package com.coofee.dep.demo

import android.os.SystemClock
import com.coofee.dep.*
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList

private val allJobTimeRef = AtomicLong(0)

private fun <R> doFakeJob(maxTimeMilliseconds: Int, result: R): R {
    var jobTime = maxTimeMilliseconds
//    var jobTime = Random().nextInt(maxTimeMilliseconds)
    if (jobTime < 20) {
        jobTime = 20
    }

    allJobTimeRef.addAndGet(jobTime.toLong())
    val endTime = SystemClock.uptimeMillis() + jobTime
    while (SystemClock.uptimeMillis() < endTime);

    return result
}

private val TASK_0 = from("task_0", Callable {
    doFakeJob(83, "sas")
})

private val TASK_1 = TaskFactory.from("task_1", {
    doFakeJob(300, 20)
})

private val TASK_2 = TaskFactory.from("task_2", {
    doFakeJob(2000, "哈哈哈哈")
}, true)

private val TASK_3 = TaskFactory.from("task_3") {
    doFakeJob(40, arrayOf("a", "b", "c"))
}

private val TASK_4 = TaskFactory.from("task_4", {
    doFakeJob(900, mapOf("name" to "张三", "age" to 90))
}, true)

private val TASK_5 = TaskFactory.from("task_5", {
    doFakeJob(90, listOf(",", ".", "/"))
}, true)

private val TASK_6 = TaskFactory.from("task_6") {
    doFakeJob(213, "task6.300")
}

private val TASK_7 = TaskFactory.from("task_7", {
    doFakeJob(1022, Result.success(90))
}, true)


interface Task8 {
    fun methodTask8(): String
}

private val TASK_8 = from<Task8>("task_8", true, Callable {
    doFakeJob(123, object : Task8 {
        override fun methodTask8(): String {
            println("task_8 method invoked.")
            return "task_8 method invoked."
        }
    })
})

private val TASK_9 = TaskFactory.from("task_9", {
    doFakeJob(998, "task9.598")
}, true)

private val TASK_10 = TaskFactory.from("task_10") {
    doFakeJob(42, "task10.42")
}

private val TASK_11 = TaskFactory.from("task_11") {
    doFakeJob(37, "task11.37")
}

private val TASK_12 = TaskFactory.from("task_12") {
    doFakeJob(66, "task12.66")
}

private val TASK_13 = TaskFactory.from("task_13") {
    doFakeJob(77, "task13.77")
}

private val TASK_14 = TaskFactory.from("task_14", {
    doFakeJob(88, "task14.88")
}, true)

private val TASK_15 = TaskFactory.from("task_15", {
    doFakeJob(1023, "task15.1023")
}, true)


fun provideDemoTask(): Task<*> {
    val builder = TaskSet.Builder("TaskSet1")
    // TaskSet1: TASK_1 -> TASK_2 -> TASK_6 -> TASK_3
    builder.add(TASK_1).before(TASK_2).before(TASK_3)
    builder.add(TASK_2).before(TASK_6)
    builder.add(TASK_3).after(TASK_6)
    val taskSet1 = builder.build()

    // TaskSet2:
    // TASK_5 -> TASK_7 -> TASK_4
    // TASK_9
    // TASK_10 -> TASK_8
    // TaskSet1 (TASK_1 -> TASK_2 -> TASK_6 -> TASK_3)
    val root = TaskSet.Builder("TaskSet2")
        .add(TASK_4).after(TASK_7)
        .add(TASK_5).before(TASK_7)
//        .add(taskSet1).before(TASK_4)
        .add(taskSet1)
        .add(TASK_8)
        .add(TASK_9)
        .add(TASK_10).before(TASK_8)
        .build()

    root.addTaskSetExecutionListener(object : TaskSetExecutionListener {
        var startTime: Long? = null

        override fun afterExecuteTaskSet(taskSet: TaskSet?) {
            Log.e(
                "Dep.Tasks",
                "TaskSet2 end, consume ${SystemClock.uptimeMillis() - startTime!!} ms, all job time=${allJobTimeRef.get()}"
            )
        }

        override fun beforeExecute(task: Task<*>?) {
        }

        override fun afterExecute(task: Task<*>?) {
        }

        override fun beforeExecuteTaskSet(taskSet: TaskSet?) {
            startTime = SystemClock.uptimeMillis()
            Log.e("Dep.Tasks", "TaskSet2 start, startTime=$startTime")
        }

    })

    return root
}

val CONDITION_PERMISSION_LOCATION = object : TaskCondition {}
val CONDITION_LAUNCH_AD_SHOW = object : TaskCondition {}
val CONDITION_DATABASE = object : TaskCondition {}
val CONDITION_LAUNCH_SHOW = object : TaskCondition {}

private val CONDITION_LIST = listOf(
    CONDITION_PERMISSION_LOCATION,
    CONDITION_LAUNCH_AD_SHOW,
    CONDITION_DATABASE,
    CONDITION_LAUNCH_SHOW
)

private val CONDITION_TASK_LIST = listOf(TASK_11, TASK_12, TASK_13, TASK_14, TASK_15)

class ConditionTask(val condition: TaskCondition, val task: Task<*>)

fun provideConditionTask(): List<ConditionTask> {
    val result = ArrayList<ConditionTask>(CONDITION_TASK_LIST.size)

    val random = Random()
    for (task in CONDITION_TASK_LIST) {
        val condition = CONDITION_LIST[random.nextInt(CONDITION_LIST.size)]
        result.add(ConditionTask(condition, task))
    }

    return result
}


