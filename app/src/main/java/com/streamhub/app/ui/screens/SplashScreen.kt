package com.streamhub.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamhub.app.data.repository.CatalogState
import com.streamhub.app.data.repository.FirebaseRepository
import com.streamhub.app.ui.theme.AccentOrange
import com.streamhub.app.ui.theme.BackgroundDark
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.TextPrimary
import com.streamhub.app.ui.theme.TextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val ExpressiveScaleEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun SplashScreen(
    repository: FirebaseRepository,
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            val catalogReady = async {
                withTimeoutOrNull(2_000L) {
                    repository.catalogState.first { it !is CatalogState.Loading }
                }
            }

            val animJob = launch {
                launch {
                    alpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    scale.animateTo(
                        targetValue = 1.0f,
                        animationSpec = tween(durationMillis = 800, easing = ExpressiveScaleEasing)
                    )
                }
            }

            animJob.join()
            catalogReady.await()
            // Silky smooth cross-dissolve exit into home screen
            exitAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
            onSplashFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .graphicsLayer {
                this.alpha = exitAlpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glowing Animated Logo Circle Container (hardware-accelerated graphicsLayer)
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryRed, AccentOrange)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "StreamHub Logo",
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Branding Text (hardware-accelerated graphicsLayer)
            Text(
                text = "StreamHub",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha.value
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Fast. Premium. Unlimited.",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.0.sp,
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha.value * 0.7f
                }
            )
        }
    }
}
