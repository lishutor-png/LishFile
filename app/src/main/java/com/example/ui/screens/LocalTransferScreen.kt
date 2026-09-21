package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StorageStats
import com.example.transfer.LocalTransferClient
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.VaultGold
import com.example.util.FileUtils
import com.example.viewmodel.FileManagerViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalTransferScreen(viewModel: FileManagerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    val serverState by viewModel.transferServer.state.collectAsState()
    val transferHistory by viewModel.transferHistory.collectAsState()
    val currentFilesInDir by viewModel.allFilesInCurrentDir.collectAsState()

    // Client connection states
    var peerAddressInput by remember { mutableStateOf("") }
    var peerPinInput by remember { mutableStateOf("") }
    var isConnectingToPeer by remember { mutableStateOf(false) }
    var remoteFilesList by remember { mutableStateOf<List<LocalTransferClient.RemoteFile>?>(null) }
    var clientError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Transfer P2P Lokal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Privacy Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% Offline & P2P — Tanpa Server Pihak Ketiga — Privasi Penuh",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Kirim File") },
                    icon = { Icon(Icons.Default.CallMade, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Terima File") },
                    icon = { Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Riwayat") },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Send Tab (Host)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            // Server status card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (serverState.isRunning) SecureGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (serverState.isRunning) Icons.Default.Wifi else Icons.Default.NetworkCheck,
                                                contentDescription = null,
                                                tint = if (serverState.isRunning) SecureGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (serverState.isRunning) "Server Transfer Aktif" else "Server Nonaktif",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = if (serverState.isRunning) SecureGreen else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (serverState.isRunning) "Siap menerima koneksi lokal" else "Aktifkan untuk membagikan file",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = serverState.isRunning,
                                            onCheckedChange = { start ->
                                                if (start) {
                                                    val filesToShare = currentFilesInDir.filter { !it.isDirectory }.map { it.file }
                                                    viewModel.startP2pServer(filesToShare)
                                                } else {
                                                    viewModel.stopP2pServer()
                                                }
                                            },
                                            modifier = Modifier.testTag("p2p_server_toggle")
                                        )
                                    }

                                    if (serverState.isRunning) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider()
                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Alamat Perangkat (WiFi):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = "http://${serverState.ipAddress}:${serverState.port}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("PIN Sesi:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = serverState.pin,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Buka alamat di atas di browser perangkat lain atau masukkan IP & PIN di menu 'Terima File' pada perangkat File Manager + lain.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Bluetooth & Direct System Share Card
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Kirim Cepat via Bluetooth", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Kirim dokumen secara langsung menggunakan Bluetooth atau Quick Share Android", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val sampleFile = currentFilesInDir.firstOrNull { !it.isDirectory }?.file
                                            if (sampleFile != null) {
                                                FileUtils.shareFileViaBluetoothOrSystem(context, sampleFile)
                                            }
                                        },
                                        modifier = Modifier.testTag("bluetooth_share_button")
                                    ) {
                                        Text("Pilih", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Shared files section
                        item {
                            Text(
                                text = "File yang Sedang Dibagikan (${serverState.sharedFiles.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (serverState.sharedFiles.isEmpty()) {
                            item {
                                Text(
                                    text = "Belum ada file yang dipilih untuk dibagikan. Semua file di folder aktif akan otomatis dibagikan saat server aktif.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(serverState.sharedFiles) { file ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(StorageStats.formatBytes(file.length()), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (file.name.endsWith(".lish")) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SecureGreen.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("AES-256", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SecureGreen)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Receive Tab (Client)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Hubungkan ke File Manager + Lain", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Masukkan IP dan PIN dari perangkat pengirim di jaringan WiFi yang sama:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = peerAddressInput,
                                        onValueChange = { peerAddressInput = it; clientError = null },
                                        label = { Text("IP & Port Pengirim") },
                                        placeholder = { Text("contoh: 192.168.1.100:8989") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("peer_ip_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = peerPinInput,
                                        onValueChange = { peerPinInput = it; clientError = null },
                                        label = { Text("PIN Sesi (4 Digit)") },
                                        placeholder = { Text("contoh: 1234") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("peer_pin_input")
                                    )

                                    if (clientError != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(clientError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (peerAddressInput.isNotBlank() && peerPinInput.isNotBlank()) {
                                                isConnectingToPeer = true
                                                clientError = null
                                                scope.launch {
                                                    val result = LocalTransferClient.fetchRemoteFiles(peerAddressInput, peerPinInput)
                                                    isConnectingToPeer = false
                                                    if (result.isSuccess) {
                                                        remoteFilesList = result.getOrNull()
                                                    } else {
                                                        clientError = result.exceptionOrNull()?.message ?: "Gagal terhubung ke peer"
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isConnectingToPeer && peerAddressInput.isNotBlank() && peerPinInput.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth().testTag("connect_peer_button")
                                    ) {
                                        if (isConnectingToPeer) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Menghubungkan...")
                                        } else {
                                            Icon(Icons.Default.Download, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Cari & Tampilkan File")
                                        }
                                    }
                                }
                            }
                        }

                        remoteFilesList?.let { files ->
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "File Tersedia di Perangkat Pengirim (${files.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            if (files.isEmpty()) {
                                item {
                                    Text("Tidak ada file yang dibagikan oleh pengirim.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                items(files) { remFile ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(remFile.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text(StorageStats.formatBytes(remFile.size), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (remFile.isEncrypted) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(SecureGreen.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("AES-256", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SecureGreen)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.downloadRemoteP2pFile(peerAddressInput, remFile.name, peerPinInput)
                                                },
                                                modifier = Modifier.testTag("download_p2p_file_${remFile.name}")
                                            ) {
                                                Text("Unduh", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // History Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        if (transferHistory.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Belum ada riwayat transfer", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            items(transferHistory) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isSent = item.direction == "SENT"
                                        Icon(
                                            imageVector = if (isSent) Icons.Default.CallMade else Icons.Default.CallReceived,
                                            contentDescription = null,
                                            tint = if (isSent) MaterialTheme.colorScheme.primary else SecureGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.fileName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            val dateFormatted = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                                            Text(
                                                text = "${if (isSent) "Terkirim ke" else "Diterima dari"} ${item.peerAddress} • ${StorageStats.formatBytes(item.fileSize)} • $dateFormatted",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (item.isEncrypted) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SecureGreen.copy(alpha = 0.15f))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text("AES-256", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SecureGreen)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
