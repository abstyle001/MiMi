package org.example.project.data

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import com.russhwolf.settings.Settings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * 用 TYPE_STEP_COUNTER 读开机累计步数。
 * 传感器只在步数变化时回调，站着不动可能 2 秒内无事件，此时回退到本地缓存值。
 */
class AndroidStepDataSource(
    private val context: Context,
    private val settings: Settings,
) : StepDataSource {

    override suspend fun cumulativeSteps(): Long? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return null
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null

        val fresh = withTimeoutOrNull(SENSOR_EVENT_TIMEOUT_MS) { awaitStepEvent(sensorManager, sensor) }
        if (fresh != null) {
            settings.putLong(KEY_LAST_CUMULATIVE, fresh)
            return fresh
        }
        return settings.getLong(KEY_LAST_CUMULATIVE, -1L).takeIf { it >= 0 }
    }

    private suspend fun awaitStepEvent(sensorManager: SensorManager, sensor: Sensor): Long? =
        suspendCancellableCoroutine { cont ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    sensorManager.unregisterListener(this)
                    cont.resume(event.values[0].toLong())
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

    private companion object {
        const val SENSOR_EVENT_TIMEOUT_MS = 2_000L
        const val KEY_LAST_CUMULATIVE = "last_cumulative_steps"
    }
}
