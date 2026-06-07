package com.example.data.local

import androidx.room.*
import com.example.data.model.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources")
    fun getAllSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE isActive = 1")
    fun getActiveSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getSourceById(id: String): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<SourceEntity>)

    @Query("UPDATE sources SET isActive = :isActive WHERE id = :id")
    suspend fun updateSourceActiveStatus(id: String, isActive: Boolean)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteSource(id: String)
}
