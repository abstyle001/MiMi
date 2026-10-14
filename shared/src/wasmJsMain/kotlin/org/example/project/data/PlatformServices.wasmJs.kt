package org.example.project.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

actual fun createSettings(): Settings = StorageSettings()

actual fun createStepDataSource(): StepDataSource = NoOpStepDataSource()
