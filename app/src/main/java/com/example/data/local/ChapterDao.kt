package com.example.data.local

import androidx.room.*
import com.example.data.model.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE comicId = :comicId ORDER BY chapterNumber DESC")
    fun getChaptersForComic(comicId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    fun getChapterById(id: String): Flow<ChapterEntity?>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterByIdSync(id: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE isDownloaded = 1")
    fun getDownloadedChapters(): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("UPDATE chapters SET isRead = :isRead WHERE id = :id")
    suspend fun updateChapterReadStatus(id: String, isRead: Boolean)

    @Query("UPDATE chapters SET isDownloaded = :isDownloaded, downloadPath = :downloadPath WHERE id = :id")
    suspend fun updateChapterDownloadStatus(id: String, isDownloaded: Boolean, downloadPath: String?)

    @Query("DELETE FROM chapters WHERE comicId = :comicId")
    suspend fun deleteChaptersForComic(comicId: String)
}
