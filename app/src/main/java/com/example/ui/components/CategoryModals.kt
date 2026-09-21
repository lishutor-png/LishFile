package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SecureGreen

@Composable
fun CloudServicesDialog(
    onDismiss: () -> Unit,
    onConnectService: (String) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE1F5FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                "Layanan Cloud Storage",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Hubungkan penyimpanan cloud Anda untuk sinkronisasi dan akses berkas langsung:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                val services = listOf(
                    Triple("Google Drive", "Akses file & dokumen Google Anda", Color(0xFF34A853)),
                    Triple("Dropbox", "Penyimpanan cloud pribadi & tim", Color(0xFF0061FF)),
                    Triple("Microsoft OneDrive", "Berkas cloud akun Microsoft", Color(0xFF0078D4)),
                    Triple("Nextcloud / WebDAV", "Server cloud pribadi mandiri", Color(0xFF0082C9))
                )

                services.forEach { (name, desc, color) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onConnectService(name) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = color)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun RemoteConnectionsDialog(
    onDismiss: () -> Unit,
    onConnectProtocol: (String) -> Unit = {}
) {
    var showAddForm by remember { mutableStateOf(false) }
    var selectedProtocol by remember { mutableStateOf("FTP") }
    var hostInput by remember { mutableStateOf("192.168.1.100") }
    var portInput by remember { mutableStateOf("21") }
    var usernameInput by remember { mutableStateOf("user") }

    if (showAddForm) {
        AlertDialog(
            onDismissRequest = { showAddForm = false },
            title = { Text("Tambah Koneksi $selectedProtocol", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("Host / Alamat IP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text("Port") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Nama Pengguna (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showAddForm = false
                    onConnectProtocol("$selectedProtocol://$hostInput:$portInput")
                }) {
                    Text("Sambungkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddForm = false }) {
                    Text("Kembali")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFEBE9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Dns,
                        contentDescription = null,
                        tint = Color(0xFF6D4C41),
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text("Koneksi Remote", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Kelola koneksi server jaringan lokal dan jarak jauh:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val protocols = listOf(
                        Triple("FTP", "File Transfer Protocol standar (Port 21)", Icons.Default.Dns),
                        Triple("SFTP", "SSH File Transfer aman terenkripsi (Port 22)", Icons.Default.Security),
                        Triple("SMB / LAN", "Windows Network Sharing lokal (Port 445)", Icons.Default.Lan),
                        Triple("WebDAV", "Protokol kolaborasi berkas web HTTP/HTTPS", Icons.Default.Public)
                    )

                    protocols.forEach { (proto, desc, icon) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedProtocol = proto
                                    portInput = when (proto) {
                                        "SFTP" -> "22"
                                        "SMB / LAN" -> "445"
                                        "WebDAV" -> "80"
                                        else -> "21"
                                    }
                                    showAddForm = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF8D6E63).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = Color(0xFF6D4C41))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(proto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun NetworkAccessDialog(
    onDismiss: () -> Unit,
    serverRunning: Boolean = false,
    serverUrl: String = "http://192.168.1.5:8080",
    onToggleServer: (Boolean) -> Unit = {},
    onOpenFullTransferScreen: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Color(0xFF00796B),
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                "Akses dari Jaringan (PC)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Akses dan kelola seluruh berkas di ponsel Anda langsung melalui peramban (browser) web di PC / Laptop tanpa kabel USB.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (serverRunning) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (serverRunning) SecureGreen else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (serverRunning) "Server Web Berjalan Aktif" else "Server Tidak Aktif",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (serverRunning) SecureGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (serverRunning) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Buka alamat ini di browser PC:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, SecureGreen)
                            ) {
                                Text(
                                    text = serverUrl,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Pastikan PC dan ponsel terhubung ke jaringan Wi-Fi yang sama.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onToggleServer(!serverRunning) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (serverRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (serverRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (serverRunning) "Hentikan Server" else "Mulai Layanan Web PC")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenFullTransferScreen()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderShared, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Layar Transfer P2P Lengkap")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Selesai")
            }
        }
    )
}

@Composable
fun PremiumVipDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "File Manager + Premium",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Nikmati pengalaman manajemen file profesional tanpa hambatan dan batas:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val perks = listOf(
                    "Bebas dari iklan selamanya",
                    "Enkripsi Brankas AES-256 tanpa batas file",
                    "Pencarian cerdas dan pemindaian file duplikat",
                    "Akses Remote & Web PC kecepatan penuh",
                    "Dukungan prioritas & pembaruan berkala"
                )

                perks.forEach { perk ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color(0xFF059669), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(perk, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("Dapatkan Sekarang", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Nanti Saja")
            }
        }
    )
}

@Composable
fun FileManagerDrawerSheet(
    onDismiss: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onToggleTheme: () -> Unit,
    themeMode: com.example.ui.theme.AppThemeMode,
    overviewStats: com.example.model.CategoryOverviewStats
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.FolderShared,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("File Manager +", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Versi 3.4.1 (Build Pro)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Ringkasan Memori Utama", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(overviewStats.primaryStorage.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                DrawerActionRow(
                    title = "Brankas Pribadi (AES-256)",
                    subtitle = "Amankan foto, video & dokumen pribadi",
                    onClick = onOpenVault
                )

                DrawerActionRow(
                    title = "Pindai File Duplikat",
                    subtitle = "Hemat ruang dengan menghapus file ganda",
                    onClick = onOpenDuplicates
                )

                DrawerActionRow(
                    title = if (themeMode == com.example.ui.theme.AppThemeMode.DARK) "Beralih ke Mode Terang" else "Beralih ke Mode Gelap",
                    subtitle = "Sesuaikan tampilan aplikasi",
                    onClick = onToggleTheme
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun DrawerActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

