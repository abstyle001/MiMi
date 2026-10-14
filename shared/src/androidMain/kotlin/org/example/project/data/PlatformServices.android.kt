package org.example.project.data

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.example.project.AndroidContextHolder

actual fun createSettings(): Settings {
    val delegate = AndroidContextHolder.appContext
        .getSharedPreferences("pet_prefs", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(delegate)
}

actual fun createStepDataSource(): StepDataSource =
    AndroidStepDataSource(AndroidContextHolder.appContext, createSettings())
