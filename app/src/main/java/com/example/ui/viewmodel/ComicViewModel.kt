package com.example.ui.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ComicEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.SourceEntity
import com.example.data.repository.ComicRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ComicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ComicRepository(application)

    // Central App Settings States
    private val _isDarkMode = MutableStateFlow(true) // defaults to dark mode
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _readerOrientation = MutableStateFlow("Webtoon") // Webtoon, Vertical, Horizontal
    val readerOrientation: StateFlow<String> = _readerOrientation.asStateFlow()

    private val _imageQuality = MutableStateFlow("High") // Low, Medium, High
    val imageQuality: StateFlow<String> = _imageQuality.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // 1. Home Flows
    val popularComics: StateFlow<List<ComicEntity>> = repository.popularComics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestComics: StateFlow<List<ComicEntity>> = repository.latestComics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recommendedComics: StateFlow<List<ComicEntity>> = repository.recommendedComics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Favorites Flow
    val favorites: StateFlow<List<ComicEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. History Flow
    val history: StateFlow<List<ComicEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Sources Flow
    val allSources: StateFlow<List<SourceEntity>> = repository.allSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSources: StateFlow<List<SourceEntity>> = repository.activeSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Explore Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("Semua")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _selectedStatus = MutableStateFlow("Semua")
    val selectedStatus: StateFlow<String> = _selectedStatus.asStateFlow()

    private val _selectedSourceId = MutableStateFlow("Semua")
    val selectedSourceId: StateFlow<String> = _selectedSourceId.asStateFlow()

    // Combined filtered comics for Explaining
    val filteredComics: StateFlow<List<ComicEntity>> = combine(
        repository.allComics,
        _searchQuery,
        _selectedGenre,
        _selectedStatus,
        _selectedSourceId
    ) { comics, query, genre, status, sourceId ->
        comics.filter { comic ->
            val matchQuery = query.isEmpty() || 
                    comic.title.contains(query, ignoreCase = true) || 
                    comic.author.contains(query, ignoreCase = true)
            
            val matchGenre = genre == "Semua" || 
                    comic.genres.split(",").map { it.trim() }.contains(genre)
            
            val matchStatus = status == "Semua" || 
                    comic.status.equals(status, ignoreCase = true)
            
            val matchSource = sourceId == "Semua" || 
                    comic.sourceId == sourceId

            matchQuery && matchGenre && matchStatus && matchSource
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Selected Comic Details
    private val _selectedComicId = MutableStateFlow<String?>(null)
    val selectedComic: StateFlow<ComicEntity?> = _selectedComicId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getComicById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedChapters: StateFlow<List<ChapterEntity>> = _selectedComicId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getChaptersForComic(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Downloaded chapters for offline management
    val downloadedChapters: StateFlow<List<ChapterEntity>> = repository.getDownloadedChapters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. Active Reader Page
    private val _activeChapterId = MutableStateFlow<String?>(null)
    val activeChapter: StateFlow<ChapterEntity?> = _activeChapterId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getChapterById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activePages = MutableStateFlow<List<String>>(emptyList())
    val activePages: StateFlow<List<String>> = _activePages.asStateFlow()

    private val _activePageIndex = MutableStateFlow(0)
    val activePageIndex: StateFlow<Int> = _activePageIndex.asStateFlow()

    // Init Logic to load Mock data
    init {
        viewModelScope.launch {
            repository.initializeDefaultData()
        }
    }

    // Settings adjustments
    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    fun setReaderOrientation(orientation: String) {
        _readerOrientation.value = orientation
    }

    fun setImageQuality(quality: String) {
        _imageQuality.value = quality
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun clearCache() {
        viewModelScope.launch {
            // Emulate caching clear
            Toast.makeText(getApplication(), "Berhasil menghapus cache gambar & data!", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            Toast.makeText(getApplication(), "Riwayat bacaan berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
        }
    }

    // Detail Navigation trigger
    fun selectComic(comicId: String?) {
        _selectedComicId.value = comicId
    }

    // Comic Bookmark / Unbookmark
    fun toggleFavorite(comicId: String, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(comicId, isFav)
            val msg = if (isFav) "Disimpan ke Favorit" else "Dihapus dari Favorit"
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Source manipulation
    fun toggleSourceActive(sourceId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.setSourceActive(sourceId, isActive)
        }
    }

    fun deleteSource(sourceId: String) {
        viewModelScope.launch {
            repository.deleteSource(sourceId)
            Toast.makeText(getApplication(), "Sumber berhasil dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    // Add source validator and action
    fun addExtensionSource(name: String, url: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.addSourceFromUrl(name, url)
            result.onSuccess {
                Toast.makeText(getApplication(), "Sumber '${it.name}' sukses ditambahkan!", Toast.LENGTH_LONG).show()
                onSuccess()
            }.onFailure {
                Toast.makeText(getApplication(), it.message ?: "Gagal menambah sumber", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Chapter reader navigation
    fun selectChapter(chapterId: String?, comicId: String) {
        _activeChapterId.value = chapterId
        _activePageIndex.value = 0
        if (chapterId != null) {
            viewModelScope.launch {
                // Fetch pages
                val pages = repository.getChapterPages(chapterId)
                _activePages.value = pages

                // Save to read history
                val chList = repository.getChaptersForComic(comicId).first()
                val activeCh = chList.find { it.id == chapterId }
                if (activeCh != null) {
                    repository.updateReadHistory(comicId, chapterId, activeCh.title, 0)
                }
            }
        } else {
            _activePages.value = emptyList()
        }
    }

    fun updateActivePage(index: Int, comicId: String, chapterId: String, chapterTitle: String) {
        _activePageIndex.value = index
        viewModelScope.launch {
            repository.updateReadHistory(comicId, chapterId, chapterTitle, index)
        }
    }

    // Chapter download trigger
    fun downloadChapter(chapterId: String) {
        viewModelScope.launch {
            repository.toggleDownloadChapter(chapterId)
            Toast.makeText(getApplication(), "Berhasil mengunduh chapter!", Toast.LENGTH_SHORT).show()
        }
    }

    // Check new chapters (Automatic Sync)
    fun syncSources() {
        viewModelScope.launch {
            val updated = repository.checkNewChapters()
            if (updated.isNotEmpty() && _notificationsEnabled.value) {
                Toast.makeText(
                    getApplication(), 
                    "Pembaruan Baru: Chapter baru tersedia di '${updated.joinToString()}'", 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(getApplication(), "Sinkronisasi selesai. Semua sumber sudah mutakhir.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Backup & Restore actions
    fun performBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportUserData()
            onReady(json)
            Toast.makeText(getApplication(), "Pencadangan berhasil selesai!", Toast.LENGTH_SHORT).show()
        }
    }

    fun performRestore(backupString: String) {
        viewModelScope.launch {
            val result = repository.importUserData(backupString)
            result.onSuccess {
                Toast.makeText(getApplication(), "Pemulihan data sukses dilakukan!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(getApplication(), "Format cadangan tidak valid atau rusak!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectGenre(genre: String) {
        _selectedGenre.value = genre
    }

    fun selectStatus(status: String) {
        _selectedStatus.value = status
    }

    fun selectSourceFilter(sourceId: String) {
        _selectedSourceId.value = sourceId
    }
}
