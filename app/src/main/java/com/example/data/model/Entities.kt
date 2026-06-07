package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "comics")
data class ComicEntity(
    @PrimaryKey val id: String, // format "sourceId:comicUrlId" or "sourceId:comicId"
    val title: String,
    val coverUrl: String,
    val description: String = "",
    val author: String = "Unknown",
    val artist: String = "Unknown",
    val status: String = "Ongoing", // Ongoing, Completed
    val rating: Double = 0.0,
    val genres: String = "", // comma-separated
    val sourceId: String,
    val sourceName: String = "",
    val isFavorite: Boolean = false,
    val addedToFavAt: Long = 0L,
    val lastReadAt: Long = 0L,
    val lastReadChapterId: String? = null,
    val lastReadChapterTitle: String? = null,
    val lastReadPageIndex: Int = 0,
    val isRecommended: Boolean = false,
    val isPopular: Boolean = false,
    val isLatest: Boolean = false
) : Serializable

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: String, // "comicId:chapterId"
    val comicId: String,
    val title: String,
    val url: String = "",
    val chapterNumber: Float = 0f,
    val releasedAt: String = "",
    val isRead: Boolean = false,
    var isDownloaded: Boolean = false,
    val downloadPath: String? = null
) : Serializable

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String = "",
    val isActive: Boolean = true,
    val version: String = "1.0.0",
    val isBuiltIn: Boolean = false
) : Serializable
