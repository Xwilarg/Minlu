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
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
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
