package eu.zirk.minlu

import android.content.ContentValues.TAG
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(tableName = "steps")
data class StepCount(
    @ColumnInfo(name = "steps") val steps: Long,
    @ColumnInfo(name = "date") val date: String,
)

@Dao
interface StepsDao {
    @Query("SELECT * FROM steps")
    suspend fun getAll(): List<StepCount>

    @Query("SELECT * FROM steps WHERE date >= date(:startDateTime) " +
            "AND date < date(:startDateTime, '+1 day')")
    suspend fun loadAllStepsFromToday(startDateTime: String): Array<StepCount>

    @Insert
    suspend fun insertAll(vararg steps: StepCount)

    @Delete
    suspend fun delete(steps: StepCount)
}

@Database(entities = [StepCount::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stepsDao(): StepsDao
}

class Repository(
    private val stepsDao: StepsDao,
) {

    fun truncate(date: Instant): Instant {
        date.da
    }

    @OptIn(ExperimentalTime::class)
    suspend fun storeSteps(stepsSinceLastReboot: Long) = withContext(Dispatchers.IO) {
        val stepCount = StepCount(
            steps = stepsSinceLastReboot,
            date = Calendar.current Clock.System.now().toString()
        )
        Log.d(TAG, "Storing steps: $stepCount")
        stepsDao.insertAll(stepCount)
    }

    suspend fun loadTodaySteps(): Long = withContext(Dispatchers.IO) {
        //printTheWholeStepsTable() // DEBUG

        val todayAtMidnight = (LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT).toString())
        val todayDataPoints = stepsDao.loadAllStepsFromToday(startDateTime = todayAtMidnight)
        when {
            todayDataPoints.isEmpty() -> 0
            else -> {
                val firstDataPointOfTheDay = todayDataPoints.first()
                val latestDataPointSoFar = todayDataPoints.last()

                val todaySteps = latestDataPointSoFar.steps - firstDataPointOfTheDay.steps
                Log.d(TAG, "Today Steps: $todaySteps")
                todaySteps
            }
        }
    }
}