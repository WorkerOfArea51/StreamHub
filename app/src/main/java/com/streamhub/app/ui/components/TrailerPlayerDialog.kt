package com.streamhub.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.streamhub.app.data.YoutubeStreamExtractor
import com.streamhub.app.ui.theme.PrimaryRed
import com.streamhub.app.ui.theme.SurfaceDark
import com.streamhub.app.ui.theme.TextPrimary

/**
 * Clean & Modern In-App YouTube Trailer Cinema Player.
 * - 16:9 cinematic aspect ratio with sleek header & close button
 * - Native YouTube controls (play/pause, scrubber, sound, settings, CC, full screen)
 * - Fullscreen maximize support with smooth overlay
 */
@OptIn(UnstableApi::class)
@Composable
fun TrailerPlayerDialog(
    videoId: String,
    title: String = "Movie Trailer",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cleanVideoId = remember(videoId) {
        when {
            videoId.contains("v=") -> videoId.substringAfter("v=").substringBefore("&")
            videoId.contains("youtu.be/") -> videoId.substringAfter("youtu.be/").substringBefore("?")
            else -> videoId.trim()
        }
    }

    var streamUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var useEmbeddedPlayer by remember { mutableStateOf(false) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Handle back button when in fullscreen custom view
    BackHandler(enabled = customFullscreenView != null) {
        customViewCallback?.onCustomViewHidden()
        customFullscreenView = null
        customViewCallback = null
    }

    // 1. Try extracting direct playable stream URL
    LaunchedEffect(cleanVideoId) {
        isLoading = true
        useEmbeddedPlayer = false
        try {
            val url = YoutubeStreamExtractor.extractStreamUrl(cleanVideoId)
            if (!url.isNullOrBlank()) {
                streamUrl = url
                useEmbeddedPlayer = false
            } else {
                useEmbeddedPlayer = true
            }
        } catch (e: Exception) {
            Log.e("TrailerPlayerDialog", "Fallback to embedded player", e)
            useEmbeddedPlayer = true
        } finally {
            isLoading = false
        }
    }

    // 2. Initialize ExoPlayer when direct stream URL is ready
    DisposableEffect(streamUrl) {
        val currentUrl = streamUrl
        if (!currentUrl.isNullOrBlank()) {
            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(currentUrl))
                prepare()
                playWhenReady = true
            }
            exoPlayer = player

            onDispose {
                player.release()
                exoPlayer = null
            }
        } else {
            onDispose { }
        }
    }

    // 3. Sync playback with Activity lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer?.pause()
                    webViewInstance?.onPause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer?.play()
                    webViewInstance?.onResume()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewInstance?.destroy()
        }
    }

    Dialog(
        onDismissRequest = {
            if (customFullscreenView != null) {
                customViewCallback?.onCustomViewHidden()
                customFullscreenView = null
                customViewCallback = null
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        if (customFullscreenView != null) {
            // True Immersive Fullscreen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = {
                        customFullscreenView!!.apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Close / Minimize button on top left in fullscreen
                IconButton(
                    onClick = {
                        customViewCallback?.onCustomViewHidden()
                        customFullscreenView = null
                        customViewCallback = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 28.dp, start = 20.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA181824))
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }
            }
        } else {
            // Normal 16:9 Cinema Card Mode (Clean, Big, Modernized)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE60A0A0F)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.98f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                        .border(
                            1.5.dp,
                            Brush.linearGradient(listOf(Color(0x66FF3B30), Color(0x33FFFFFF))),
                            RoundedCornerShape(20.dp)
                        )
                        .shadow(24.dp, RoundedCornerShape(20.dp))
                        .padding(bottom = 6.dp)
                ) {
                    // Modern Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryRed,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "TRAILER",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 16:9 Cinema Video Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoading -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = PrimaryRed,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Loading HD Trailer...",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            exoPlayer != null && !useEmbeddedPlayer -> {
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = exoPlayer
                                            useController = true
                                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                            setBackgroundColor(android.graphics.Color.BLACK)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            else -> {
                                AndroidView(
                                    factory = { ctx ->
                                        createEmbeddedTrailerWebView(
                                            context = ctx,
                                            videoId = cleanVideoId,
                                            onCustomViewChange = { view, callback ->
                                                customFullscreenView = view
                                                customViewCallback = callback
                                            }
                                        ).also {
                                            webViewInstance = it
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createEmbeddedTrailerWebView(
    context: Context,
    videoId: String,
    onCustomViewChange: (View?, WebChromeClient.CustomViewCallback?) -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = false
            useWideViewPort = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 15; Poco X6 Neo) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
        }
        webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                onCustomViewChange(view, callback)
            }

            override fun onHideCustomView() {
                onCustomViewChange(null, null)
            }
        }
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                return false
            }
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <style>
                    html, body {
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        background-color: #000000;
                        overflow: hidden;
                    }
                    #player {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        border: 0;
                    }
                </style>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var tag = document.createElement('script');
                    tag.src = "https://www.youtube.com/iframe_api";
                    var firstScriptTag = document.getElementsByTagName('script')[0];
                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
                    
                    var player;
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            videoId: '$videoId',
                            playerVars: {
                                'autoplay': 1,
                                'playsinline': 1,
                                'controls': 1,
                                'rel': 0,
                                'modestbranding': 1,
                                'iv_load_policy': 3,
                                'fs': 1,
                                'enablejsapi': 1,
                                'origin': 'https://localhost'
                            },
                            events: {
                                'onReady': function(event) {
                                    event.target.playVideo();
                                }
                            }
                        });
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
    }
}
