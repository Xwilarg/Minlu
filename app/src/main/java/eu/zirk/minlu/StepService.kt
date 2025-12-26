package eu.zirk.minlu

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat

class StepService : Service() {

    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val sensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "CHANNEL_SERVICE")
            .setContentTitle("Step counter")
            .setContentText("Tracking your steps...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        startForeground(
            100,
            notification
        )
        return START_STICKY
    }
}