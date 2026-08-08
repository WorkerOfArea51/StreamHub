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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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

    var isEnabled by remember(currentConfig.isEnabled) { mutableStateOf(currentConfig.isEnabled) }
    var selectedType by remember(currentConfig.type) { mutableStateOf(currentConfig.type) }
    var server by remember(currentConfig.server) { mutableStateOf(currentConfig.server) }
    var portText by remember(currentConfig.port) { mutableStateOf(currentConfig.port.toString()) }
    var secret by remember(currentConfig.secret) { mutableStateOf(currentConfig.secret) }
    var username by remember(currentConfig.username) { mutableStateOf(currentConfig.username) }
    var password by remember(currentConfig.password) { mutableStateOf(currentConfig.password) }

    // Proxifier Authentication & Protocol Extension States
    var authEnabled by remember(currentConfig.authEnabled) { mutableStateOf(currentConfig.authEnabled) }
    var useSocks4a by remember(currentConfig.useSocks4a) { mutableStateOf(currentConfig.useSocks4a) }
    var sendUserAgent by remember(currentConfig.sendUserAgent) { mutableStateOf(currentConfig.sendUserAgent) }
    var useNtlm by remember(currentConfig.useNtlm) { mutableStateOf(currentConfig.useNtlm) }
    var useKerberos by remember(currentConfig.useKerberos) { mutableStateOf(currentConfig.useKerberos) }

    // Proxifier Advanced Settings States
    var showAdvancedDialog by remember { mutableStateOf(false) }
    var customLabel by remember(currentConfig.customLabel) { mutableStateOf(currentConfig.customLabel) }
    var useRemoteDns by remember(currentConfig.useRemoteDns) { mutableStateOf(currentConfig.useRemoteDns) }
    var promptIfEmpty by remember(currentConfig.promptIfEmpty) { mutableStateOf(currentConfig.promptIfEmpty) }
    var promptOnAuthFail by remember(currentConfig.promptOnAuthFail) { mutableStateOf(currentConfig.promptOnAuthFail) }
    var useAuthUrl by remember(currentConfig.useAuthUrl) { mutableStateOf(currentConfig.useAuthUrl) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = "Proxy", tint = primaryColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proxy Server 🛡️", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Bypass regional network restrictions with MTProto, SOCKS5, SOCKS4, or HTTPS Proxies.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ENABLE PROXY TOGGLE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Proxy Routing", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                items(publicProxies, key = { "${it.server}:${it.port}" }) { pItem ->
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
                                                TelegramProxyManager.saveConfig(
                                                    pItem.server, pItem.port, pItem.secret, pItem.username, pItem.password,
                                                    pItem.type, true, customLabel, authEnabled, useSocks4a, sendUserAgent,
                                                    useNtlm, useKerberos, useRemoteDns, promptIfEmpty, promptOnAuthFail, useAuthUrl
                                                )
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
                                                    Text("Port ${pItem.port} • ${pItem.type.name}", color = TextSecondary, fontSize = 10.sp)
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
                    // TAB 2: CUSTOM PROXIFIER-STYLE SETUP (MTProto, SOCKS5, SOCKS4, HTTPS)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Protocol Selector Section
                        Text("Protocol:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                ProxyType.SOCKS5 to "SOCKS v5",
                                ProxyType.SOCKS4 to "SOCKS v4",
                                ProxyType.HTTP to "HTTPS",
                                ProxyType.MTPROTO to "MTProto"
                            ).forEach { (pType, label) ->
                                val isSel = selectedType == pType
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSel) primaryColor.copy(alpha = 0.25f) else Color(0xFF14141E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(if (isSel) 1.5.dp else 1.dp, if (isSel) primaryColor else Color(0xFF2C2C3E), RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedType = pType
                                            if (portText.isBlank() || portText == "443" || portText == "1080" || portText == "8080") {
                                                portText = when (pType) {
                                                    ProxyType.MTPROTO -> "443"
                                                    ProxyType.SOCKS5, ProxyType.SOCKS4 -> "1080"
                                                    ProxyType.HTTP -> "8080"
                                                }
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(label, color = if (isSel) primaryColor else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Server Address and Port in Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = server,
                                onValueChange = { server = it },
                                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = "Address", tint = primaryColor, modifier = Modifier.size(16.dp)) },
                                label = { Text("Address / IP", color = TextSecondary, fontSize = 11.sp) },
                                placeholder = { Text("103.91.130.38", color = TextSecondary, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF2C2C3E),
                                    focusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(2f)
                            )

                            OutlinedTextField(
                                value = portText,
                                onValueChange = { portText = it },
                                label = { Text("Port", color = TextSecondary, fontSize = 11.sp) },
                                placeholder = { Text("1080", color = TextSecondary, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF2C2C3E),
                                    focusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SECTION: AUTHENTICATION (Matching Proxifier Screenshot 1 & 4)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF2C2C3E), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (selectedType != ProxyType.MTPROTO) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = authEnabled,
                                            onCheckedChange = { authEnabled = it },
                                            colors = CheckboxDefaults.colors(checkedColor = primaryColor, checkmarkColor = Color.White)
                                        )
                                        Text("Authentication Enable", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                                        if (selectedType == ProxyType.HTTP) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = useNtlm,
                                                    onCheckedChange = { useNtlm = it },
                                                    colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                                )
                                                Text("NTLM", color = TextSecondary, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }

                                if (selectedType == ProxyType.MTPROTO) {
                                    OutlinedTextField(
                                        value = secret,
                                        onValueChange = { secret = it },
                                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = "Secret", tint = primaryColor, modifier = Modifier.size(16.dp)) },
                                        label = { Text("Secret Key (MTProto)", color = TextSecondary, fontSize = 11.sp) },
                                        placeholder = { Text("ee123456... or dd...", color = TextSecondary, fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primaryColor,
                                            unfocusedBorderColor = Color(0xFF2C2C3E),
                                            focusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else if (selectedType == ProxyType.SOCKS4) {
                                    // SOCKS4 User ID field
                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User ID", tint = primaryColor, modifier = Modifier.size(16.dp)) },
                                        label = { Text("User ID", color = TextSecondary, fontSize = 11.sp) },
                                        placeholder = { Text("EC_QGT", color = TextSecondary, fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primaryColor,
                                            unfocusedBorderColor = Color(0xFF2C2C3E),
                                            focusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    // SOCKS5 & HTTPS Username & Password
                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it },
                                        enabled = authEnabled,
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username", tint = primaryColor, modifier = Modifier.size(16.dp)) },
                                        label = { Text("Username", color = TextSecondary, fontSize = 11.sp) },
                                        placeholder = { Text("EC_QGT", color = TextSecondary, fontSize = 11.sp) },
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
                                        enabled = authEnabled,
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = primaryColor, modifier = Modifier.size(16.dp)) },
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = "Toggle password",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        label = { Text("Password", color = TextSecondary, fontSize = 11.sp) },
                                        placeholder = { Text("••••••", color = TextSecondary, fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primaryColor,
                                            unfocusedBorderColor = Color(0xFF2C2C3E),
                                            focusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // SECTION: PROTOCOL SPECIFIC OPTIONS (Matching Proxifier Screenshot 3 & 4)
                        if (selectedType == ProxyType.SOCKS4) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = useSocks4a,
                                    onCheckedChange = { useSocks4a = it },
                                    colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                )
                                Text(
                                    text = "Use SOCKS 4A extension (remote hostname resolving)",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        } else if (selectedType == ProxyType.HTTP) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = sendUserAgent,
                                    onCheckedChange = { sendUserAgent = it },
                                    colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                )
                                Text(
                                    text = "Send User-Agent header in HTTP proxy request",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        // ACTION ROW: Check & Advanced... (Matching Proxifier desktop)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isTesting = true
                                    testResult = null
                                    val p = portText.toIntOrNull()?.coerceIn(1, 65535) ?: 443
                                    scope.launch {
                                        val res = TelegramProxyManager.testConnection(
                                            server = server,
                                            port = p,
                                            type = selectedType,
                                            username = username,
                                            password = password,
                                            secret = secret
                                        )
                                        res.fold(
                                            onSuccess = { ping ->
                                                isSuccess = true
                                                testResult = "${selectedType.name} Ready: ${ping}ms ⚡"
                                            },
                                            onFailure = { err ->
                                                isSuccess = false
                                                val msg = err.message ?: "Connect error"
                                                testResult = if (msg.length > 22) msg.take(20) + "..." else msg
                                            }
                                        )
                                        isTesting = false
                                    }
                                },
                                enabled = server.isNotBlank() && !isTesting,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = primaryColor, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Checking...", color = primaryColor, fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.Speed, contentDescription = "Check", tint = if (isSuccess && testResult != null) Color(0xFF10B981) else primaryColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        testResult ?: "Check ⚡",
                                        color = if (isSuccess && testResult != null) Color(0xFF10B981) else primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showAdvancedDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Advanced", tint = primaryColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Advanced...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = portText.toIntOrNull()?.coerceIn(1, 65535) ?: 443
                    TelegramProxyManager.saveConfig(
                        server, p, secret, username, password, selectedType, isEnabled,
                        customLabel, authEnabled, useSocks4a, sendUserAgent, useNtlm,
                        useKerberos, useRemoteDns, promptIfEmpty, promptOnAuthFail, useAuthUrl
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )

    // ADVANCED PROXY SERVER SETTINGS MODAL (Matching Proxifier Screenshot 5)
    if (showAdvancedDialog) {
        AlertDialog(
            onDismissRequest = { showAdvancedDialog = false },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = "Advanced", tint = primaryColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Advanced Proxy Settings ⚙️", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    // Appearance Section
                    Text("Appearance", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("Custom Label", color = TextSecondary, fontSize = 11.sp) },
                        placeholder = { Text("(\"address:port\" by default)", color = TextSecondary, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF2C2C3E),
                            focusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Authentication Behavior
                    Text("Authentication", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = promptIfEmpty,
                            onCheckedChange = { promptIfEmpty = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                        )
                        Text("Ask Username and Password if field is empty", color = TextSecondary, fontSize = 11.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = promptOnAuthFail,
                            onCheckedChange = { promptOnAuthFail = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                        )
                        Text("Ask Username and Password if auth fails", color = TextSecondary, fontSize = 11.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useAuthUrl,
                            onCheckedChange = { useAuthUrl = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                        )
                        Text("Use Authentication URL", color = TextSecondary, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Other / DNS Resolution
                    Text("DNS & Routing", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useRemoteDns,
                            onCheckedChange = { useRemoteDns = it },
                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                        )
                        Text("Use target hostname in proxy request (Remote DNS)", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAdvancedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAdvancedDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
