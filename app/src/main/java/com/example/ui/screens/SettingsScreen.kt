package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ComicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val readerOrientation by viewModel.readerOrientation.collectAsState()
    val imageQuality by viewModel.imageQuality.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    // Dialog control states
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var backupJsonString by remember { mutableStateOf("") }
    var restoreInputString by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pengaturan", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Elegant Settings Category: Tampilan & UI
            SettingsHeader("Tampilan & UI")
            SettingsToggleItem(
                title = "Mode Gelap (Dark Mode)",
                subtitle = "Ganti tema visual malam hari untuk kenyamanan membaca",
                icon = Icons.Outlined.DarkMode,
                checked = isDarkMode,
                onCheckedChange = { viewModel.setDarkMode(it) },
                testTag = "dark_mode_switch"
            )

            // Settings Category: Pembaca Komik
            SettingsHeader("Pembaca Komik (Reader)")
            SettingsSelectorItem(
                title = "Mode Baca Utama",
                subtitle = "Ubah cara navigasi halaman dalam pembaca",
                icon = Icons.Outlined.ChromeReaderMode,
                value = readerOrientation,
                options = listOf("Webtoon", "Vertical", "Horizontal"),
                onSelected = { viewModel.setReaderOrientation(it) }
            )
            SettingsSelectorItem(
                title = "Kualitas Gambar",
                subtitle = "Pengaturan resolusi memuat gambar agar hemat RAM/koneksi",
                icon = Icons.Outlined.HighQuality,
                value = imageQuality,
                options = listOf("Low", "Medium", "High"),
                onSelected = { viewModel.setImageQuality(it) }
            )

            // Settings Category: Sinkronisasi & Pemberitahuan
            SettingsHeader("Notifikasi & Sinkronisasi")
            SettingsToggleItem(
                title = "Notifikasi Chapter Baru",
                subtitle = "Terima pemberitahuan popup jika chapter baru terdeteksi live",
                icon = Icons.Outlined.NotificationsActive,
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                testTag = "notif_switch"
            )

            // Settings Category: Data Lokal & Pencadangan
            SettingsHeader("Manajemen Data")
            SettingsActionItem(
                title = "Hapus Cache Gambar",
                subtitle = "Kosongkan RAM dan refresh memori penyimpanan sementara",
                icon = Icons.Outlined.DeleteSweep,
                actionLabel = "Bersihkan",
                onClick = { viewModel.clearCache() }
            )
            SettingsActionItem(
                title = "Pencadangan (Backup)",
                subtitle = "Simpan daftar favorit & riwayat bacaan menjadi teks cadangan",
                icon = Icons.Outlined.CloudUpload,
                actionLabel = "Cadangkan",
                onClick = {
                    viewModel.performBackup { json ->
                        backupJsonString = json
                        showBackupDialog = true
                    }
                }
            )
            SettingsActionItem(
                title = "Pemulihan (Restore)",
                subtitle = "Pulihkan koleksi favorit Anda menggunakan teks cadangan",
                icon = Icons.Outlined.CloudDownload,
                actionLabel = "Pulihkan",
                onClick = {
                    restoreInputString = ""
                    showRestoreDialog = true
                }
            )

            // Settings Category: Tentang Aplikasi
            SettingsHeader("Tentang Aplikasi")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Komik NaLex v1.0.0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Aplikasi pembaca komik modern berkecepatan tinggi, hemat RAM, responsif, dan terinspirasi dari Kotatsu, Mihon, serta Tachiyomi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Dibuat khusus untuk kenyamanan membaca terbaik dengan Material 3 Design.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Backup Show Dialog
        if (showBackupDialog) {
            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                title = { Text("Teks Cadangan Anda", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Salin teks format enkripsi JSON di bawah ini untuk disimpan di note Anda.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = backupJsonString,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(backupJsonString))
                            showBackupDialog = false
                        }
                    ) {
                        Text("Salin Teks")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupDialog = false }) {
                        Text("Tutup")
                    }
                }
            )
        }

        // Restore Input Dialog
        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Pulihkan Cadangan", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Tempel teks format JSON cadangan yang sudah disalin sebelumnya.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = restoreInputString,
                            onValueChange = { restoreInputString = it },
                            placeholder = { Text("{\"favorites\": [...], \"history\": [...]}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            maxLines = 6,
                            textStyle = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.performRestore(restoreInputString)
                            showRestoreDialog = false
                        }
                    ) {
                        Text("Pulihkan Sekarang")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
fun SettingsSelectorItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(value, style = MaterialTheme.typography.bodySmall)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                onSelected(opt)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
