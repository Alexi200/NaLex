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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ComicEntity
import com.example.data.model.SourceEntity
import com.example.ui.viewmodel.ComicViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    viewModel: ComicViewModel,
    onComicSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchResults by viewModel.filteredComics.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allSources by viewModel.allSources.collectAsState()

    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedSourceFilter by viewModel.selectedSourceId.collectAsState()

    var activeTabState by remember { mutableStateOf(0) } // 0 = Katalog, 1 = Ekstensi & Sumber

    // Add Source dialog state
    var showAddSourceDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("Jelajahi Komik", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { activeTabState = 1 }) {
                            Icon(imageVector = Icons.Default.Extension, contentDescription = "Sistem Ekstensi")
                        }
                    }
                )

                // Tab Switcher
                TabRow(
                    selectedTabIndex = activeTabState,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTabState == 0,
                        onClick = { activeTabState = 0 },
                        text = { Text("Katalog Komik", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Book, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTabState == 1,
                        onClick = { activeTabState = 1 },
                        text = { Text("Ekstensi (${allSources.size})", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Extension, contentDescription = null) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTabState == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSourceDialog = true },
                    icon = { Icon(Icons.Outlined.Add, "Tambah") },
                    text = { Text("Sumber Baru") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_source_fab")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (activeTabState == 0) {
                // Catalogue search and filters
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search layout input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("explore_search_input"),
                        placeholder = { Text("Cari judul, pengarang atau ilustrator...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Genre Filters Row
                    Text(
                        "Genre Utama",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    val genres = listOf("Semua", "Aksi", "Petualangan", "Sihir", "Fantasi", "Sci-Fi", "Mecha", "Romantis", "Drama", "Isekai")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(genres) { genreName ->
                            FilterChip(
                                selected = selectedGenre == genreName,
                                onClick = { viewModel.selectGenre(genreName) },
                                label = { Text(genreName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Status and source filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Status Filter
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Status", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            val statuses = listOf("Semua", "Ongoing", "Completed")
                            var expandedStatus by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expandedStatus = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(selectedStatus)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(
                                    expanded = expandedStatus,
                                    onDismissRequest = { expandedStatus = false }
                                ) {
                                    statuses.forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(st) },
                                            onClick = {
                                                viewModel.selectStatus(st)
                                                expandedStatus = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Source Filter
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Pilih Sumber", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            var expandedSources by remember { mutableStateOf(false) }
                            val sourceNameMap = allSources.associate { it.id to it.name }
                            val activeSourceName = if (selectedSourceFilter == "Semua") "Semua Sumber" else sourceNameMap[selectedSourceFilter] ?: "Kustom"
                            Box {
                                OutlinedButton(
                                    onClick = { expandedSources = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(activeSourceName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(
                                    expanded = expandedSources,
                                    onDismissRequest = { expandedSources = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Semua Sumber") },
                                        onClick = {
                                            viewModel.selectSourceFilter("Semua")
                                            expandedSources = false
                                        }
                                    )
                                    allSources.forEach { src ->
                                        DropdownMenuItem(
                                            text = { Text(src.name) },
                                            onClick = {
                                                viewModel.selectSourceFilter(src.id)
                                                expandedSources = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Column results of filtered comics
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Komik tidak ditemukan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Coba ubah kata kunci atau bersihkan filter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        // Grid layout of comics to match Tachiyomi catalog visual
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 110.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(searchResults) { comic ->
                                GridComicCard(comic, onComicSelect)
                            }
                        }
                    }
                }
            } else {
                // Extensions list
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            "Manajemen plugin eksternal pembaca.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(allSources) { source ->
                        ExtensionSourceItem(
                            source = source,
                            onToggleActive = { active -> viewModel.toggleSourceActive(source.id, active) },
                            onDelete = { viewModel.deleteSource(source.id) }
                        )
                    }
                }
            }

            // High Fidelity Add Source Dialog
            if (showAddSourceDialog) {
                AddSourceDialog(
                    onDismiss = { showAddSourceDialog = false },
                    onConfirm = { name, url ->
                        viewModel.addExtensionSource(name, url) {
                            showAddSourceDialog = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GridComicCard(comic: ComicEntity, onComicSelect: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onComicSelect(comic.id) }
            .testTag("explore_comic_item_${comic.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
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
                
                // Overlay source indicator in grid
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = comic.sourceName,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${comic.rating} ⭐ • ${comic.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ExtensionSourceItem(
    source: SourceEntity,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var checkStatus by remember { mutableStateOf("Konek") } // Konek, Loading, Ok, No

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (source.isBuiltIn) Icons.Default.Verified else Icons.Outlined.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (source.isBuiltIn) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("Bawaan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text(
                    text = "v${source.version} • ${source.url}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Connection Check Button & Switch Active
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Check Connection
                    TextButton(
                        onClick = {
                            checkStatus = "Loading"
                            // Mock connection ping delay
                            java.util.Timer().schedule(object : java.util.TimerTask() {
                                override fun run() {
                                    checkStatus = "Aktif (OK)"
                                }
                            }, 1000)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (checkStatus == "Loading") "Ping..." else checkStatus)
                    }

                    Switch(
                        checked = source.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.testTag("source_switch_${source.id}")
                    )
                }

                if (!source.isBuiltIn) {
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nameState by remember { mutableStateOf("") }
    var urlState by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Sumber Komik", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Masukkan spesifikasi plugin / URL sumber katalog yang kompatibel dengan standar Tachiyomi/Mihon.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    label = { Text("Nama Ekstensi/Sumber") },
                    placeholder = { Text("Contoh: Mangaku, BatoIndonesia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = urlState,
                    onValueChange = { urlState = it },
                    label = { Text("URL Sumber Ekstensi (.json atau catalog endpoint)") },
                    placeholder = { Text("https://contoh-sumber.org/index.json") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nameState, urlState) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Tambahkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
