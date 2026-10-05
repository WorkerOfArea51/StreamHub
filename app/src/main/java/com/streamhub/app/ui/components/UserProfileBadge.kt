package com.streamhub.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.ui.theme.TextSecondary

private data class ProfileTierBadgeData(
    val text: String,
    val icon: ImageVector,
    val bg: Color,
    val textColor: Color
)

@Composable
fun UserProfileTierBadge(
    isAdmin: Boolean,
    isAccessKeyVerified: Boolean,
    remainingDays: Int,
    modifier: Modifier = Modifier
) {
    val tierData = when {
        isAdmin -> ProfileTierBadgeData(
            text = "OWNER",
            icon = Icons.Default.AdminPanelSettings,
            bg = Color(0x33FFD700),
            textColor = Color(0xFFFFD700)
        )
        isAccessKeyVerified -> {
            if (remainingDays > 0) {
                ProfileTierBadgeData(
                    text = "VIP ($remainingDays d)",
                    icon = Icons.Default.Verified,
                    bg = Color(0x2238BDF8),
                    textColor = Color(0xFF38BDF8)
                )
            } else {
                ProfileTierBadgeData(
                    text = "LIFETIME VIP",
                    icon = Icons.Default.Star,
                    bg = Color(0x22FFD700),
                    textColor = Color(0xFFFFD700)
                )
            }
        }
        else -> ProfileTierBadgeData(
            text = "USER",
            icon = Icons.Default.Person,
            bg = Color(0x18FFFFFF),
            textColor = TextSecondary
        )
    }

    Surface(
        shape = CircleShape,
        color = tierData.bg,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = tierData.icon,
                contentDescription = null,
                tint = tierData.textColor,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = tierData.text,
                color = tierData.textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
