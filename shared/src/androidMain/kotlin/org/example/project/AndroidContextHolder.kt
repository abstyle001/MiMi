package org.example.project

import android.content.Context

/** MainActivity 启动时初始化，供 shared/androidMain 各处取 Context */
object AndroidContextHolder {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
