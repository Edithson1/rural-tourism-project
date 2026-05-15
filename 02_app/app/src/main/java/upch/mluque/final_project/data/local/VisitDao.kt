package upch.mluque.final_project.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY registrationDate DESC")
    fun getAllVisits(): Flow<List<Visit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: Visit)

    @Update
    suspend fun updateVisit(visit: Visit)

    @Delete
    suspend fun deleteVisit(visit: Visit)

    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun getVisitById(id: Int): Visit?

    @Query("SELECT * FROM visits ORDER BY registrationDate DESC")
    suspend fun getAllOnce(): List<Visit>
}
