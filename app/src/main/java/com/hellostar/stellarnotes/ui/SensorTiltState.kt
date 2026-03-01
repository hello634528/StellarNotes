package com.hellostar.stellarnotes.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

data class Tilt(val x: Float = 0f, val y: Float = 0f)

@Composable
fun rememberTiltState(): Tilt {
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(Tilt()) }

    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val l = object : SensorEventListener {
            private var gx = 0f
            private var gy = 0f
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val ax = e.values[0] / 9.81f
                        val ay = e.values[1] / 9.81f
                        tilt = Tilt(((-ax * 0.6f) + gx * 0.4f).coerceIn(-1.2f, 1.2f), ((ay * 0.6f) + gy * 0.4f).coerceIn(-1.2f, 1.2f))
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        gx = (gx * 0.92f + e.values[1] * 0.08f).coerceIn(-1.5f, 1.5f)
                        gy = (gy * 0.92f + e.values[0] * 0.08f).coerceIn(-1.5f, 1.5f)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        acc?.let { sm.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        gyro?.let { sm.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sm.unregisterListener(l) }
    }
    return tilt
}
