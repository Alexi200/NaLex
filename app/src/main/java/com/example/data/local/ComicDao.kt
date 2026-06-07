package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ComicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {
    @Query("SELECT * FROM comics")
    fun getAllComics(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE isFavorite = 1 ORDER BY addedToFavAt DESC")
    fun getFavorites(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE lastReadAt > 0 ORDER BY lastReadAt DESC")
    fun getHistory(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE id = :id")
    fun getComicById(id: String): Flow<ComicEntity?>

    @Query("SELECT * FROM comics WHERE id = :id")
    suspend fun getComicByIdSync(id: String): ComicEntity?

    @Query("SELECT * FROM comics WHERE isPopular = 1")
    fun getPopularComics(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE isLatest = 1")
    fun getLatestComics(): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE isRecommended = 1")
    fun getRecommendedComics(): Flow<List<ComicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComic(comic: ComicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComics(comics: List<ComicEntity>)

    @Query("UPDATE comics SET isFavorite = :isFavorite, addedToFavAt = :addedToFavAt WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean, addedToFavAt: Long)

    @Query("UPDATE comics SET lastReadAt = :lastReadAt, lastReadChapterId = :chapterId, lastReadChapterTitle = :chapterTitle, lastReadPageIndex = :pageIndex WHERE id = :id")
    suspend fun updateReadHistory(id: String, lastReadAt: Long, chapterId: String, chapterTitle: String, pageIndex: Int)

    @Query("UPDATE comics SET lastReadAt = 0, lastReadChapterId = NULL, lastReadChapterTitle = NULL, lastReadPageIndex = 0")
    suspend fun clearHistory()
}
