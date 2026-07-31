package top.chengdongqing.wechat.core.location.heading

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 手机面向的磁北方位角：正北 0°，顺时针递增。
 *
 * 高德的定位蓝点会在 SDK 内部使用同类传感器旋转，但不会通过位置回调暴露该角度，
 * 因此需要独立读取 TYPE_ROTATION_VECTOR 才能把静止时的朝向共享给对方。
 */
@Composable
fun rememberDeviceHeading(): State<Float?> {
    val context = LocalContext.current
    val heading = remember { mutableStateOf<Float?>(null) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(SensorManager::class.java)

        @Suppress("DEPRECATION")
        val windowManager = context.getSystemService(WindowManager::class.java)
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val rotationMatrix = FloatArray(9)
        val adjustedMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var smoothedRadians: Float? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                @Suppress("DEPRECATION")
                val displayRotation = windowManager.defaultDisplay.rotation
                val (axisX, axisY) = when (displayRotation) {
                    Surface.ROTATION_90 ->
                        SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X

                    Surface.ROTATION_180 ->
                        SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y

                    Surface.ROTATION_270 ->
                        SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X

                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    axisX,
                    axisY,
                    adjustedMatrix
                )
                val raw = SensorManager.getOrientation(adjustedMatrix, orientation)[0]
                // Circular low-pass filter avoids the 359° → 0° discontinuity.
                val previous = smoothedRadians
                val filtered = if (previous == null) raw else atan2(
                    sin(previous) * (1f - SMOOTHING) + sin(raw) * SMOOTHING,
                    cos(previous) * (1f - SMOOTHING) + cos(raw) * SMOOTHING
                )
                smoothedRadians = filtered
                heading.value = Math.toDegrees(filtered.toDouble()).toFloat()
                    .let { (it + 360f) % 360f }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (sensor != null) {
            sensorManager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return heading
}

private const val SMOOTHING = .18f
