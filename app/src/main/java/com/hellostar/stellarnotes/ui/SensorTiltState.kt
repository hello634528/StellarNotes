package com.hellostar.stellarnotes.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

data class Tilt(val x: Float, val y: Float)

@Composable
fun rememberTiltState(): Tilt {
    val context = LocalContext.current
    var tx by remember { mutableFloatStateOf(0f) }
    var ty by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        var gx = 0f
        var gy = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val ax = event.values[0] / 9.81f
                        val ay = event.values[1] / 9.81f
                        tx = ((-ax * 0.6f) + gx * 0.4f).coerceIn(-1.2f, 1.2f)
                        ty = ((ay * 0.6f) + gy * 0.4f).coerceIn(-1.2f, 1.2f)
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        gx = (gx * 0.92f + event.values[1] * 0.08f).coerceIn(-1.5f, 1.5f)
                        gy = (gy * 0.92f + event.values[0] * 0.08f).coerceIn(-1.5f, 1.5f)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accSensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroSensor?.let { sm.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

        onDispose {
            sm.unregisterListener(listener)
        }
    }
    return Tilt(tx, ty)
}
