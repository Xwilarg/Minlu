package eu.zirk.minlu

import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.coroutines.resume

class StepService : Service() {

    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val sensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }

    private lateinit var listener: SensorEventListener
    private var stepCountRef: Long = -1L
    private var currStepHour: Long = -1L
    private var hourlyStepDelta: Long = 0L

    private lateinit var db: AppDatabase

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

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "minlu-db"
        ).build()

        registerStepListener()

        return START_STICKY
    }

    private fun registerStepListener() {
        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val steps = event.values[0].toLong()
                val time = Instant.now().truncatedTo(ChronoUnit.HOURS).epochSecond
                if (stepCountRef == -1L || steps == 0L) {
                    stepCountRef = steps
                    currStepHour = time
                    hourlyStepDelta = 0L
                    return
                } else if (currStepHour != time) {
                    if (hourlyStepDelta > 0L) {
                        val stepCount = StepCount(
                            steps = hourlyStepDelta,
                            date = currStepHour
                        )
                        Log.d(ContentValues.TAG, "Storing steps: ${stepCount.steps} at ${stepCount.date}")
                        GlobalScope.future {
                            db.stepsDao().insertAll(stepCount)
                        }
                    }
                    currStepHour = time
                    hourlyStepDelta = 0L
                } else {
                    hourlyStepDelta += (steps - stepCountRef)
                }
                stepCountRef = steps
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                Log.d(TAG, "Accuracy changed to: $accuracy")
            }
        }

        sensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(listener)
    }
}
private const val TAG = "STEP_COUNT_LISTENER"
