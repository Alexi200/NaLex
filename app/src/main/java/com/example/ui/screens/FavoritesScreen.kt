package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ComicEntity
import com.example.ui.viewmodel.ComicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: ComicViewModel,
    onComicSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()

    var activeTabState by remember { mutableStateOf(0) } // 0 = Bookmark (Favorit), 1 = Riwayat & Statistik
    var sourceGroupState by remember { mutableStateOf("Semua") }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("Koleksi Anda", fontWeight = FontWeight.Bold) },
                    actions = {
                        if (activeTabState == 1 && history.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearHistory() }) {
                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )

                // Tab selectors
                TabRow(
                    selectedTabIndex = activeTabState,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTabState == 0,
                        onClick = { activeTabState = 0 },
                        text = { Text("Favorit (${favorites.size})", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Favorite, null) }
                    )
                    Tab(
                        selected = activeTabState == 1,
                        onClick = { activeTabState = 1 },
                        text = { Text("Riwayat & Stats", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.History, null) }
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (activeTabState == 0) {
                // Bookmarks Layout
                if (favorites.isEmpty()) {
                    EmptyShelfView(
                        icon = Icons.Default.FavoriteBorder,
                        title = "Daftar Favorit Kosong",
                        subtitle = "Buka Beranda atau Jelajahi untuk menambahkan komik ke daftar favorit Anda!"
                    )
                } else {
                    // Grouping/Filtering horizontal buttons bar
                    val sourcesInFav = listOf("Semua") + favorites.map { it.sourceName }.distinct()
                    
                    Text(
                        "Kelompokkan Berdasarkan Sumber",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sourcesInFav) { src ->
                            FilterChip(
                                selected = sourceGroupState == src,
                                onClick = { sourceGroupState = src },
                                label = { Text(src) },
                                leadingIcon = {
                                    if (sourceGroupState == src) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            )
                        }
                    }

                    val groupedFavs = if (sourceGroupState == "Semua") {
                        favorites
                    } else {
                        favorites.filter { it.sourceName == sourceGroupState }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(groupedFavs) { comic ->
                            GridComicCard(comic = comic, onComicSelect = onComicSelect)
                        }
                    }
                }
            } else {
                // Histories and Statistics Layout
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Gorgeous Stats Cards
                    item {
                        Text(
                            "Statistik Bacaan Anda",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        StatsSection(favorites = favorites, history = history)
                    }

                    item {
                        Text(
                            "Riwayat Membaca Terakhir",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (history.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Belum ada riwayat",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Mulai membaca chapter mana saja untuk mencatat riwayat bacaan otomatis.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(history) { comic ->
                            HistoryListItem(comic = comic, onComicSelect = onComicSelect)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsSection(favorites: List<ComicEntity>, history: List<ComicEntity>) {
    val totalChaptersRead = history.size * 3 + 2 // Simulated calculations for user fun metrics of total chapters read
    val avgRating = if (favorites.isNotEmpty()) String.format("%.1f", favorites.map { it.rating }.average()) else "0.0"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Ringkasan Aktivitas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatMetric(value = favorites.size.toString(), label = "Favorit")
                StatMetric(value = history.size.toString(), label = "Dibaca")
                StatMetric(value = totalChaptersRead.toString(), label = "Chapter")
                StatMetric(value = avgRating, label = "Rata Rating")
            }
        }
    }
}

@Composable
fun RowScope.StatMetric(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
    }
}

@Composable
fun HistoryListItem(comic: ComicEntity, onComicSelect: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onComicSelect(comic.id) }
            .testTag("history_item_${comic.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(comic.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = comic.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 50.dp, height = 70.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Bab terakhir dibaca: ${comic.lastReadChapterTitle ?: "Halaman Awal"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Kemajuan: Halaman ${comic.lastReadPageIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = { onComicSelect(comic.id) }) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Lanjut Membaca",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptyShelfView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
