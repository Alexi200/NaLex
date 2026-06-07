package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ChapterEntity
import com.example.data.model.ComicEntity
import com.example.ui.viewmodel.ComicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    comicId: String,
    viewModel: ComicViewModel,
    onBack: () -> Unit,
    onChapterSelect: (String) -> Unit, // chapterId
    modifier: Modifier = Modifier
) {
    // Select the current comic in ViewModel
    LaunchedEffect(comicId) {
        viewModel.selectComic(comicId)
    }

    val comic by viewModel.selectedComic.collectAsState()
    val chapters by viewModel.selectedChapters.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(comic?.title ?: "Memuat...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    comic?.let { c ->
                        IconButton(
                            onClick = { viewModel.toggleFavorite(c.id, !c.isFavorite) },
                            modifier = Modifier.testTag("detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (c.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Simpan ke Favorit",
                                tint = if (c.isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // overlay look
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (comic == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val c = comic!!
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()) // adjust padding with TopAppBar
            ) {
                // Header Banner
                item {
                    DetailHeaderBanner(c)
                }

                // Summary Expandable
                item {
                    DetailSynopsisSection(c)
                }

                // Chapters Count section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FormatListNumbered, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daftar Chapter (${chapters.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Terbaru di Atas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (chapters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada chapter tersedia.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(chapters) { chapter ->
                        ChapterListRow(
                            chapter = chapter,
                            onClick = { onChapterSelect(chapter.id) },
                            onDownload = { viewModel.downloadChapter(chapter.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailHeaderBanner(comic: ComicEntity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Blurred background layout cover
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(comic.coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)) // shade
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Content on Top of Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(width = 110.dp, height = 150.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(comic.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Pill Source
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        comic.sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Karya: ${comic.author}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Status: ${comic.status} • Rating: ${comic.rating} ⭐",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun DetailSynopsisSection(comic: ComicEntity) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Sinopsis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = comic.description.ifEmpty { "Tidak ada deskripsi synopsis untuk komik ini." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .animateContentSize()
        )
        TextButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(if (isExpanded) "Sembunyikan" else "Selengkapnya...")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Flow layout of Genesis Genres
        Text(
            "Genre",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            comic.genres.split(",").forEach { g ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = g.trim(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterListRow(
    chapter: ChapterEntity,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("chapter_list_item_${chapter.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (chapter.isRead) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (chapter.isRead) FontWeight.Normal else FontWeight.Bold,
                        color = if (chapter.isRead) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (chapter.isRead) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Sudah Dibaca",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "Rilis: ${chapter.releasedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Download trigger button actions
            IconButton(
                onClick = onDownload,
                modifier = Modifier.testTag("download_button_${chapter.id}")
            ) {
                Icon(
                    imageVector = if (chapter.isDownloaded) Icons.Filled.CloudDone else Icons.Outlined.CloudDownload,
                    contentDescription = if (chapter.isDownloaded) "Terunduh" else "Unduh Bab ini",
                    tint = if (chapter.isDownloaded) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
