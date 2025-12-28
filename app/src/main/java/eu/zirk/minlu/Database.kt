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
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(tableName = "steps")
data class StepCount(
    @ColumnInfo(name = "steps") val steps: Long,
    @ColumnInfo(name = "date") val date: Long,
)

@Dao
interface StepsDao {
    @Query("SELECT * FROM steps")
    suspend fun getAll(): List<StepCount>

    @Query("SELECT * FROM steps WHERE date >= :startDateTime AND date < :endDateTime")
    suspend fun loadAllStepsFromToday(startDateTime: Long, endDateTime: Long): Array<StepCount>

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


    @OptIn(ExperimentalTime::class)
    suspend fun storeSteps(stepsSinceLastReboot: Long) = withContext(Dispatchers.IO) {
        val stepCount = StepCount(
            steps = stepsSinceLastReboot,
            date = Instant.now().truncatedTo(ChronoUnit.HOURS).epochSecond
        )
        Log.d(TAG, "Storing steps: $stepCount at ${Instant.now().truncatedTo(ChronoUnit.HOURS).epochSecond}")
        stepsDao.insertAll(stepCount)
    }

    suspend fun loadTodaySteps(): Long = withContext(Dispatchers.IO) {
        //printTheWholeStepsTable() // DEBUG

        val todayAtMidnight = LocalDate.now(ZoneOffset.UTC).atStartOfDay()
        val todayDataPoints = stepsDao.loadAllStepsFromToday(startDateTime = todayAtMidnight.toEpochSecond(
            ZoneOffset.UTC), endDateTime = todayAtMidnight.plusHours(24).toEpochSecond(ZoneOffset.UTC))
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