package com.streamhub.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import com.streamhub.app.ui.theme.bouncyTouch

/**
 * Studio Tools & Catalog Operations for Creator Studio.
 * Extracted into a standalone composable to isolate bytecode and DEX registers.
 */
@Composable
fun AdminToolsTab(
    state: AdminEditorState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Tools & Catalog Management", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        // 1. User Codes & Voucher Manager Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Access Codes & Vouchers", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Manage private community gate access codes", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Button(
                    onClick = { state.showVoucherDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .bouncyTouch()
                ) {
                    Text("Manage", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Backup / Restore Card with M3 Expressive Split Button
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Database Backup & Restore", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Export, share snapshots & manage device archives", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                // Official M3 Expressive Split Button: [ Backup (18.dp, 6.dp) ] <3.dp gap> [ Restore ▾ (6.dp, 18.dp) ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(36.dp)
                ) {
                    // Leading Button: Backup
                    Surface(
                        shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                        color = Color(0xFF0284C7),
                        modifier = Modifier
                            .fillMaxHeight()
                            .bouncyTouch(pressedScale = 0.94f)
                            .clickable { state.showBackupDialog = true }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Backup", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(3.dp))

                    // Trailing Button: Restore
                    Surface(
                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp),
                        color = Color(0xFF0284C7),
                        modifier = Modifier
                            .fillMaxHeight()
                            .bouncyTouch(pressedScale = 0.94f)
                            .clickable { state.showBackupDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Restore", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Restore", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // 4. Server URL Migration Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Stream URL Rewriter", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Batch rewrite media stream domains across catalog", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Button(
                    onClick = { state.showMigrationDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .bouncyTouch()
                ) {
                    Text("Rewrite", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
