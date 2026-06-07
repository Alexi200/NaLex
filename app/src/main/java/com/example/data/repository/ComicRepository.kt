package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.ComicEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.SourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ComicRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val comicDao = database.comicDao()
    private val chapterDao = database.chapterDao()
    private val sourceDao = database.sourceDao()

    // Exposed Flows from Local Room Cache
    val allComics: Flow<List<ComicEntity>> = comicDao.getAllComics().flowOn(Dispatchers.IO)
    val favorites: Flow<List<ComicEntity>> = comicDao.getFavorites().flowOn(Dispatchers.IO)
    val history: Flow<List<ComicEntity>> = comicDao.getHistory().flowOn(Dispatchers.IO)
    val popularComics: Flow<List<ComicEntity>> = comicDao.getPopularComics().flowOn(Dispatchers.IO)
    val latestComics: Flow<List<ComicEntity>> = comicDao.getLatestComics().flowOn(Dispatchers.IO)
    val recommendedComics: Flow<List<ComicEntity>> = comicDao.getRecommendedComics().flowOn(Dispatchers.IO)
    val allSources: Flow<List<SourceEntity>> = sourceDao.getAllSources().flowOn(Dispatchers.IO)
    val activeSources: Flow<List<SourceEntity>> = sourceDao.getActiveSources().flowOn(Dispatchers.IO)

    fun getComicById(id: String): Flow<ComicEntity?> = comicDao.getComicById(id).flowOn(Dispatchers.IO)
    fun getChaptersForComic(comicId: String): Flow<List<ChapterEntity>> = chapterDao.getChaptersForComic(comicId).flowOn(Dispatchers.IO)
    fun getChapterById(id: String): Flow<ChapterEntity?> = chapterDao.getChapterById(id).flowOn(Dispatchers.IO)
    fun getDownloadedChapters(): Flow<List<ChapterEntity>> = chapterDao.getDownloadedChapters().flowOn(Dispatchers.IO)

    suspend fun toggleFavorite(comicId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val timestamp = if (isFavorite) System.currentTimeMillis() else 0L
        comicDao.updateFavoriteStatus(comicId, isFavorite, timestamp)
    }

    suspend fun updateReadHistory(comicId: String, chapterId: String, chapterTitle: String, pageIndex: Int) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        // Save read history in ComicEntity
        comicDao.updateReadHistory(comicId, timestamp, chapterId, chapterTitle, pageIndex)
        // Mark chapter as read
        chapterDao.updateChapterReadStatus(chapterId, true)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        comicDao.clearHistory()
    }

    suspend fun setSourceActive(id: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        sourceDao.updateSourceActiveStatus(id, isActive)
    }

    suspend fun deleteSource(id: String) = withContext(Dispatchers.IO) {
        sourceDao.deleteSource(id)
    }

    suspend fun toggleDownloadChapter(chapterId: String) = withContext(Dispatchers.IO) {
        val current = chapterDao.getChapterByIdSync(chapterId)
        if (current != null) {
            val nextState = !current.isDownloaded
            val path = if (nextState) "internal/downloads/$chapterId" else null
            chapterDao.updateChapterDownloadStatus(chapterId, nextState, path)
        }
    }

    // Dynamic extension validation & additions
    fun validateSourceUrl(url: String): Boolean {
        if (url.isEmpty()) return false
        return try {
            val lower = url.lowercase()
            (lower.startsWith("http://") || lower.startsWith("https://")) && 
            (lower.contains(".") || lower.contains("localhost"))
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addSourceFromUrl(name: String, url: String): Result<SourceEntity> = withContext(Dispatchers.IO) {
        if (!validateSourceUrl(url)) {
            return@withContext Result.failure(IllegalArgumentException("Format URL sumber tidak valid!"))
        }
        val cleanName = name.trim().ifEmpty { "Sumber Kustom - " + url.substringAfter("://").substringBefore("/") }
        val id = "custom_" + cleanName.lowercase().replace(" ", "_")
        val source = SourceEntity(
            id = id,
            name = cleanName,
            url = url,
            isActive = true,
            version = "1.0.0",
            isBuiltIn = false
        )
        sourceDao.insertSource(source)

        // Generate mock comics for this added source to show real extension ecosystem!
        val mockComics = listOf(
            ComicEntity(
                id = "$id:comic_custom_1",
                title = "$cleanName Special Live",
                coverUrl = "https://images.unsplash.com/photo-1627856013091-fed6e4e30025?w=500&auto=format&fit=crop&q=60",
                description = "Komik kustom eksklusif yang diterbitkan langsung dari plugin sumber kustom $cleanName.",
                author = "Kreator Kustom",
                artist = "Ilustrator Kustom",
                status = "Ongoing",
                rating = 4.7,
                genres = "Petualangan,Aksi,Fiksi",
                sourceId = id,
                sourceName = cleanName,
                isRecommended = true,
                isPopular = true
            ),
            ComicEntity(
                id = "$id:comic_custom_2",
                title = "Metropolis Rise",
                coverUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&auto=format&fit=crop&q=60",
                description = "Bangkitnya peradaban baru di masa depan pasca apokalips. Petualangan futuristik menanti Anda.",
                author = "FuturePen",
                artist = "CyberArt",
                status = "Completed",
                rating = 4.2,
                genres = "Sci-Fi,Aksi",
                sourceId = id,
                sourceName = cleanName,
                isLatest = true
            )
        )
        comicDao.insertComics(mockComics)

        // Add mock chapters
        for (comic in mockComics) {
            val chapters = List(5) { i ->
                ChapterEntity(
                    id = "${comic.id}:ch_${i+1}",
                    comicId = comic.id,
                    title = "Chapter ${i+1}: Langkah Awal Perjalanan",
                    chapterNumber = (i + 1).toFloat(),
                    releasedAt = "0${i+1}-06-2026",
                    isRead = false
                )
            }
            chapterDao.insertChapters(chapters)
        }

        Result.success(source)
    }

    // Get pages of comic chapter. Webtoons or single layouts.
    suspend fun getChapterPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        // High quality webtoon-style background strip mockups using clean unsplash illustration categories
        val list = mutableListOf<String>()
        // Determine theme based on chapter id prefix to feel highly specific
        val isWebtoon = chapterId.contains("webtoon")
        val isNalex = chapterId.contains("built_in_nalex")

        val keywords = when {
            isWebtoon -> listOf("anime", "illustration", "cityscape", "cyberpunk", "portrait", "manga")
            isNalex -> listOf("mountains", "fantasy", "sword", "magic", "temple", "warrior")
            else -> listOf("sketch", "retro", "comic", "drawing", "concept", "ink")
        }

        // Generate beautiful mockup pages
        return@withContext listOf(
            "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=800&auto=format&fit=crop&q=60&sig=1",
            "https://images.unsplash.com/photo-1563089145-599997674d42?w=800&auto=format&fit=crop&q=60&sig=2",
            "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&auto=format&fit=crop&q=60&sig=3",
            "https://images.unsplash.com/photo-1501472312651-726afd116ff1?w=800&auto=format&fit=crop&q=60&sig=4",
            "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop&q=60&sig=5",
            "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&auto=format&fit=crop&q=60&sig=6"
        )
    }

    // Sync automatic system simulation
    suspend fun checkNewChapters(): List<String> = withContext(Dispatchers.IO) {
        // Let's check some comics and insert a new chapter inside room
        val comics = comicDao.getAllComics().first()
        val updatedComics = mutableListOf<String>()
        if (comics.isNotEmpty()) {
            val target = comics.random()
            val chapters = chapterDao.getChaptersForComic(target.id).first()
            val nextNumber = (chapters.maxOfOrNull { it.chapterNumber } ?: 0f) + 1f
            val newChId = "${target.id}:ch_$nextNumber"
            val newCh = ChapterEntity(
                id = newChId,
                comicId = target.id,
                title = "Chapter $nextNumber: Kejutan Tak Terduga [BARU]",
                chapterNumber = nextNumber,
                releasedAt = "07-06-2026",
                isRead = false,
                isDownloaded = false
            )
            chapterDao.insertChapters(listOf(newCh))
            updatedComics.add(target.title)
        }
        return@withContext updatedComics
    }

    // User Data Backup and Restore
    suspend fun exportUserData(): String = withContext(Dispatchers.IO) {
        val backupJson = JSONObject()
        
        // Export favorites list
        val favs = comicDao.getFavorites().first()
        val favsArray = JSONArray()
        for (c in favs) {
            favsArray.put(c.id)
        }
        backupJson.put("favorites", favsArray)

        // Export history info
        val hist = comicDao.getHistory().first()
        val histArray = JSONArray()
        for (c in hist) {
            val hObj = JSONObject().apply {
                put("comicId", c.id)
                put("lastReadAt", c.lastReadAt)
                put("lastReadChapterId", c.lastReadChapterId ?: "")
                put("lastReadChapterTitle", c.lastReadChapterTitle ?: "")
                put("lastPageIndex", c.lastReadPageIndex)
            }
            histArray.put(hObj)
        }
        backupJson.put("history", histArray)

        return@withContext backupJson.toString()
    }

    suspend fun importUserData(backupString: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val backupJson = JSONObject(backupString)
            
            // Restore favorites
            val favsArray = backupJson.optJSONArray("favorites")
            if (favsArray != null) {
                for (i in 0 until favsArray.length()) {
                    val comicId = favsArray.getString(i)
                    comicDao.updateFavoriteStatus(comicId, true, System.currentTimeMillis() - (i * 1000))
                }
            }

            // Restore history
            val histArray = backupJson.optJSONArray("history")
            if (histArray != null) {
                for (i in 0 until histArray.length()) {
                    val hObj = histArray.getJSONObject(i)
                    val comicId = hObj.getString("comicId")
                    val lastReadAt = hObj.getLong("lastReadAt")
                    val lastReadChapterId = hObj.getString("lastReadChapterId")
                    val lastReadChapterTitle = hObj.getString("lastReadChapterTitle")
                    val lastPageIndex = hObj.getInt("lastPageIndex")

                    comicDao.updateReadHistory(comicId, lastReadAt, lastReadChapterId, lastReadChapterTitle, lastPageIndex)
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Populate Beautiful Modern Manga/Manhua/Webtoon Series Mock data
    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        val existingSources = sourceDao.getAllSources().first()
        if (existingSources.isNotEmpty()) return@withContext // already populated

        // 1. Establish Built-in Sources
        val sources = listOf(
            SourceEntity("built_in_nalex", "Katalog NaLex", "https://katalog.komiknalex.com", isActive = true, version = "1.2.0", isBuiltIn = true),
            SourceEntity("webtoon", "WEBTOON Official", "https://webtoons.com", isActive = true, version = "2.0.4", isBuiltIn = true),
            SourceEntity("mangadex", "MangaDex Plugin", "https://mangadex.org", isActive = true, version = "1.1.0", isBuiltIn = false)
        )
        sourceDao.insertSources(sources)

        // 2. Establish Beautiful Comics
        val comics = listOf(
            // NaLex Catalog Specials
            ComicEntity(
                id = "built_in_nalex:dawn_blue",
                title = "NaLex Legend: Dawn of Blue",
                coverUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop&q=60",
                description = "Petualangan ksatria NaLex mempertahankan kerajaan dari kepungan monster laut biru gelap raksasa. Dibumbui intrik politik istana dan seni sihir legendaris.",
                author = "Nala Alexander",
                artist = "Studio LexArt",
                status = "Ongoing",
                rating = 4.9,
                genres = "Petualangan,Aksi,Sihir,Fantasi",
                sourceId = "built_in_nalex",
                sourceName = "Katalog NaLex",
                isRecommended = true,
                isPopular = true
            ),
            ComicEntity(
                id = "built_in_nalex:mech_nalex",
                title = "Mechatronics of NaLex",
                coverUrl = "https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=500&auto=format&fit=crop&q=60",
                description = "Robot-robot perang modular buatan NaLex bangkit menguasai kota metropolitan kelam di tahun 2050. Apakah mekanik muda bernama Arya mampu memprogram ulang mereka?",
                author = "Arya Bima",
                artist = "Rogue Mecha Studio",
                status = "Ongoing",
                rating = 4.7,
                genres = "Sci-Fi,Mecha,Aksi,Thriller",
                sourceId = "built_in_nalex",
                sourceName = "Katalog NaLex",
                isLatest = true,
                isPopular = true
            ),
            ComicEntity(
                id = "built_in_nalex:shadow_sovereign",
                title = "Shadow Sovereign",
                coverUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop&q=60",
                description = "Penguasa bayangan yang bersembunyi dalam keheningan mengawasi evolusi manusia baru. Dia membimbing umat manusia maju atau memicu kiamat dalam senyuman.",
                author = "Lex Write",
                artist = "Sovereign Team",
                status = "Completed",
                rating = 4.6,
                genres = "Isekai,Aksi,Misteri",
                sourceId = "built_in_nalex",
                sourceName = "Katalog NaLex",
                isRecommended = true
            ),

            // Webtoon Official Specials
            ComicEntity(
                id = "webtoon:tower_god",
                title = "Tower of God",
                coverUrl = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=500&auto=format&fit=crop&q=60",
                description = "Tempat segalanya dapat diraih. Siapa pun yang menaklukkan menara akan memperoleh segalanya. Kekayaan, kekuatan, takhta, cinta. Ikuti perjalanan Bam mencari Rachel di sana.",
                author = "SIU",
                artist = "SIU",
                status = "Ongoing",
                rating = 4.8,
                genres = "Fiksi,Petualangan,Sihir",
                sourceId = "webtoon",
                sourceName = "WEBTOON Official",
                isPopular = true
            ),
            ComicEntity(
                id = "webtoon:lore_olympus",
                title = "Lore Olympus",
                coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500&auto=format&fit=crop&q=60",
                description = "Kisah dewa-dewi mitologi Yunani dalam balutan modern, warna-warna menawan, dan romansa rumit antara Hades dan Persephone.",
                author = "Rachel Smythe",
                artist = "Rachel Smythe",
                status = "Completed",
                rating = 4.5,
                genres = "Romantis,Drama,Fantasi",
                sourceId = "webtoon",
                sourceName = "WEBTOON Official",
                isRecommended = true
            ),
            ComicEntity(
                id = "webtoon:the_boxer",
                title = "The Boxer",
                coverUrl = "https://images.unsplash.com/photo-1517466787929-bc90951d0974?w=500&auto=format&fit=crop&q=60",
                description = "Apakah ada bakat alami sesungguhnya? Juara bertahan legendaris menatap dunia tinju dengan dingin ketika monster tak terkalahkan muncul dari gang sempit.",
                author = "JH",
                artist = "JH",
                status = "Completed",
                rating = 4.9,
                genres = "Olahraga,Aksi,Drama",
                sourceId = "webtoon",
                sourceName = "WEBTOON Official",
                isLatest = true,
                isPopular = true
            ),

            // MangaDex Mock Specials
            ComicEntity(
                id = "mangadex:frieren",
                title = "Frieren: Beyond Journey's End",
                coverUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=500&auto=format&fit=crop&q=60",
                description = "Menceritakan tentang penyihir abadi elf Frieren yang melakukan perjalanan ziarah melacak jejak langkah tim pahlawan masa lalunya pasca sang pahlawan wafat.",
                author = "Kanehito Yamada",
                artist = "Tsukasa Abe",
                status = "Ongoing",
                rating = 4.9,
                genres = "Petualangan,Fantasi,Drama,Overpowered",
                sourceId = "mangadex",
                sourceName = "MangaDex Plugin",
                isRecommended = true,
                isLatest = true
            ),
            ComicEntity(
                id = "mangadex:one_piece",
                title = "One Piece",
                coverUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=500&auto=format&fit=crop&q=60",
                description = "Petualangan fantastis Monkey D. Luffy dan kru topi jeraminya mengarungi Grand Line untuk menemukan harta karun pamungkas: One Piece.",
                author = "Eiichiro Oda",
                artist = "Eiichiro Oda",
                status = "Ongoing",
                rating = 4.9,
                genres = "Petualangan,Aksi,Comedy,Shounen",
                sourceId = "mangadex",
                sourceName = "MangaDex Plugin",
                isPopular = true
            )
        )
        comicDao.insertComics(comics)

        // 3. Establish Chapter Lists for each comic
        for (comic in comics) {
            val count = if (comic.isPopular) 15 else 8
            val chapters = List(count) { idx ->
                val chNum = idx + 1
                ChapterEntity(
                    id = "${comic.id}:ch_$chNum",
                    comicId = comic.id,
                    title = "Chapter $chNum: " + getMockChapterName(comic.title, chNum),
                    chapterNumber = chNum.toFloat(),
                    releasedAt = "${String.format("%02d", (10 + chNum) % 28)}-05-2026",
                    isRead = false,
                    isDownloaded = false
                )
            }
            chapterDao.insertChapters(chapters)
        }
    }

    private fun getMockChapterName(title: String, num: Int): String {
        val names = listOf(
            "Takdir yang Berjalan",
            "Matahari di Balik Bukit",
            "Seni yang Hilang",
            "Pertemuan Tak Disengaja",
            "Bayang Tersembunyi",
            "Membuka Gerbang Kuno",
            "Latihan Keras",
            "Tantangan Pertama",
            "Kekuatan Tersembunyi",
            "Kebohongan Indah",
            "Aliansi Darurat",
            "Menembus Batas",
            "Musuh dari Masa Lalu",
            "Malam Sebelum Badai",
            "Pertempuran Puncak"
        )
        return if (num - 1 < names.size) names[num - 1] else "Rahasia Semesta Bagian $num"
    }
}
