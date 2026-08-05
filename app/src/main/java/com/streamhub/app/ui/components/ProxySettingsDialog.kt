package com.streamhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.telegram.ProxyType
import com.streamhub.app.data.telegram.PublicProxyItem
import com.streamhub.app.data.telegram.TelegramProxyManager
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ProxySettingsDialog(
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val currentConfig by TelegramProxyManager.proxyConfig.collectAsState()
    val publicProxies by TelegramProxyManager.publicProxies.collectAsState()
    val isFetchingProxies by TelegramProxyManager.isFetchingProxies.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    var isEnabled by remember { mutableStateOf(currentConfig.isEnabled) }
    var selectedType by remember { mutableStateOf(currentConfig.type) }
    var server by remember { mutableStateOf(currentConfig.server) }
    var portText by remember { mutableStateOf(currentConfig.port.toString()) }
    var secret by remember { mutableStateOf(currentConfig.secret) }
    var username by remember { mutableStateOf(currentConfig.username) }
    var password by remember { mutableStateOf(currentConfig.password) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = "Proxy", tint = primaryColor)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Telegram MTProto Proxy 🛡️", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                Text(
                    text = "Bypass regional Telegram blocks and ISP restrictions with MTProto Proxies.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ENABLE PROXY TOGGLE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Telegram Proxy", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            TelegramProxyManager.setEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF14141E),
                    contentColor = primaryColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = primaryColor
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Public, contentDescription = "Auto-Fetch", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-Fetch Live", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ElectricalServices, contentDescription = "Custom", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom Setup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // TAB 1: TELSTREAM LIVE AUTO-FETCH PROXIES & PARALLEL PING TESTER
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        TelegramProxyManager.autoFetchPublicProxies()
                                    }
                                },
                                enabled = !isFetchingProxies,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isFetchingProxies) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ping Testing...", fontSize = 10.sp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Fetch", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("⚡ Auto-Fetch & Ping", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            if (publicProxies.any { it.pingMs > 0 }) {
                                OutlinedButton(
                                    onClick = {
                                        TelegramProxyManager.selectFastestProxy()
                                        isEnabled = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = "Fastest", tint = primaryColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto-Select Fastest", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (publicProxies.isEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Tap '⚡ Auto-Fetch & Ping' to find live proxies with lowest latency.", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().height(230.dp)
                            ) {
                                items(publicProxies) { pItem ->
                                    val isCurrentActive = currentConfig.server == pItem.server && currentConfig.isEnabled
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isCurrentActive) primaryColor.copy(alpha = 0.15f) else Color(0xFF14141E)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, if (isCurrentActive) primaryColor else Color(0xFF2C2C3E), RoundedCornerShape(12.dp))
                                            .clickable {
                                                server = pItem.server
                                                portText = pItem.port.toString()
                                                secret = pItem.secret
                                                username = pItem.username
                                                password = pItem.password
                                                selectedType = pItem.type
                                                isEnabled = true
                                                TelegramProxyManager.saveConfig(pItem.server, pItem.port, pItem.secret, pItem.username, pItem.password, pItem.type, true)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(pItem.country, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(pItem.server, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("Port ${pItem.port} • MTProto", color = TextSecondary, fontSize = 10.sp)
                                                }
                                            }

                                            if (pItem.pingMs > 0) {
                                                val pingColor = when {
                                                    pItem.pingMs < 100 -> Color(0xFF10B981)
                                                    pItem.pingMs < 300 -> Color(0xFFF59E0B)
                                                    else -> Color(0xFFEF4444)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(pingColor))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("${pItem.pingMs}ms", color = pingColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            } else {
                                                Text("Offline", color = Color(0xFFEF4444), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 2: CUSTOM MANUAL PROXY SETUP
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProxyType.entries.forEach { pType ->
                                val isSel = selectedType == pType
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSel) primaryColor.copy(alpha = 0.2f) else Color(0xFF14141E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, if (isSel) primaryColor else Color(0xFF2C2C3E), RoundedCornerShape(10.dp))
                                        .clickable { selectedType = pType }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(pType.name, color = if (isSel) primaryColor else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = server,
                            onValueChange = { server = it },
                            label = { Text("Proxy Server / Host", color = TextSecondary) },
                            placeholder = { Text("e.g. 192.168.1.1 or proxy.tg.com", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = portText,
                            onValueChange = { portText = it },
                            label = { Text("Port", color = TextSecondary) },
                            placeholder = { Text("443", color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2C2C3E),
                                focusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (selectedType == ProxyType.MTPROTO) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = secret,
                                onValueChange = { secret = it },
                                label = { Text("Secret Key (MTProto)", color = TextSecondary) },
                                placeholder = { Text("e.g. ee1234567890...", color = TextSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF2C2C3E),
                                    focusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // SOCKS5 & HTTP USERNAME / PASSWORD AUTH
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username (Optional)", color = TextSecondary) },
                                placeholder = { Text("Proxy Username", color = TextSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF2C2C3E),
                                    focusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password (Optional)", color = TextSecondary) },
                                placeholder = { Text("Proxy Password", color = TextSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF2C2C3E),
                                    focusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                isTesting = true
                                testResult = null
                                val p = portText.toIntOrNull() ?: 443
                                scope.launch {
                                    val res = TelegramProxyManager.testConnection(server, p)
                                    res.fold(
                                        onSuccess = { ping ->
                                            isSuccess = true
                                            testResult = "Proxy Online • Latency: ${ping}ms ⚡"
                                        },
                                        onFailure = { err ->
                                            isSuccess = false
                                            testResult = "Connection Failed: ${err.message}"
                                        }
                                    )
                                    isTesting = false
                                }
                            },
                            enabled = server.isNotBlank() && !isTesting,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = primaryColor, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing Proxy...", color = primaryColor, fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.ElectricalServices, contentDescription = "Test", tint = primaryColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection Latency", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (testResult != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = testResult!!,
                                color = if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = portText.toIntOrNull() ?: 443
                    TelegramProxyManager.saveConfig(server, p, secret, username, password, selectedType, isEnabled)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save & Apply", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
