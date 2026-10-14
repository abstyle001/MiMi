package org.example.project.data

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createSettings(): Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

actual fun createStepDataSource(): StepDataSource = NoOpStepDataSource()
