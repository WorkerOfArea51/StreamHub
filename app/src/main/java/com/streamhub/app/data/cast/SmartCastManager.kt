package com.streamhub.app.data.cast

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.Toast
import com.streamhub.app.data.api.SharedHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Discovered Smart TV / DLNA MediaRenderer entity.
 */
data class CastDevice(
    val id: String,
    val name: String,
    val model: String,
    val manufacturer: String,
    val ipAddress: String,
    val locationUrl: String,
    val controlUrl: String
)

/**
 * Cinema-Grade Smart TV & DLNA Casting Engine.
 *
 * Discovers DLNA/UPnP MediaRenderer devices on the local Wi-Fi via SSDP multicast (Samsung Tizen,
 * LG webOS, Sony Bravia, Android TV, Roku, Fire TV, Kodi) and controls streaming via AVTransport SOAP.
 * Also provides an external Intent bridge for Google Cast and streaming player apps.
 */
object SmartCastManager {

    private const val TAG = "SmartCastManager"
    private const val SSDP_PORT = 1900
    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SCAN_TIMEOUT_MS = 3500

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _discoveredDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<CastDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _activeDevice = MutableStateFlow<CastDevice?>(null)
    val activeDevice: StateFlow<CastDevice?> = _activeDevice.asStateFlow()

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private var scanJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * Initiates SSDP M-SEARCH query on local Wi-Fi network to discover DLNA Renderers.
     */
    fun startDiscovery(context: Context) {
        if (_isScanning.value) return
        _discoveredDevices.value = emptyList()
        _isScanning.value = true

        scanJob?.cancel()
        scanJob = scope.launch {
            try {
                // Acquire MulticastLock to allow incoming multicast packet inspection
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wifiManager?.createMulticastLock("StreamHubCastLock")?.apply {
                    setReferenceCounted(true)
                    acquire()
                }

                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(0))
                    soTimeout = SCAN_TIMEOUT_MS
                }

                val mSearchPayload = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: urn:schemas-upnp-org:service:AVTransport:1\r\n\r\n"
                ).toByteArray()

                val group = InetAddress.getByName(SSDP_ADDRESS)
                val packet = DatagramPacket(mSearchPayload, mSearchPayload.size, group, SSDP_PORT)
                socket.send(packet)

                // Also send a general query for MediaRenderers
                val mSearchRenderer = (
                    "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n"
                ).toByteArray()
                socket.send(DatagramPacket(mSearchRenderer, mSearchRenderer.size, group, SSDP_PORT))

                val buffer = ByteArray(2048)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                val endTime = System.currentTimeMillis() + SCAN_TIMEOUT_MS

                while (isActive && System.currentTimeMillis() < endTime) {
                    try {
                        socket.receive(receivePacket)
                        val response = String(receivePacket.data, 0, receivePacket.length)
                        val location = extractHeader(response, "LOCATION")
                        val ip = receivePacket.address.hostAddress ?: ""
                        if (!location.isNullOrBlank()) {
                            launch { parseDeviceDescription(location.trim(), ip) }
                        }
                    } catch (e: Exception) {
                        // SocketTimeoutException expected when scan window closes
                        break
                    }
                }

                socket.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error during SSDP scan: ${e.message}")
            } finally {
                try {
                    multicastLock?.let { if (it.isHeld) it.release() }
                } catch (_: Exception) {}
                multicastLock = null
                _isScanning.value = false
            }
        }
    }

    fun stopDiscovery() {
        scanJob?.cancel()
        _isScanning.value = false
        try {
            multicastLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {}
        multicastLock = null
    }

    private suspend fun parseDeviceDescription(locationUrl: String, ip: String) {
        withContext(Dispatchers.IO) {
            try {
                val client = SharedHttpClient.baseClient.newBuilder()
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(locationUrl).build()
                val response = client.newCall(request).execute()
                val xml = response.body?.string().orEmpty()
                if (xml.isBlank()) return@withContext

                val friendlyName = extractTag(xml, "friendlyName") ?: "Smart TV"
                val modelName = extractTag(xml, "modelName") ?: "DLNA Media Renderer"
                val manufacturer = extractTag(xml, "manufacturer") ?: "Smart TV"
                val udn = extractTag(xml, "UDN") ?: locationUrl

                // Extract AVTransport control URL
                val controlUrl = extractAvTransportControlUrl(xml, locationUrl)
                if (controlUrl.isNotBlank()) {
                    val device = CastDevice(
                        id = udn,
                        name = friendlyName,
                        model = modelName,
                        manufacturer = manufacturer,
                        ipAddress = ip,
                        locationUrl = locationUrl,
                        controlUrl = controlUrl
                    )

                    _discoveredDevices.update { current ->
                        if (current.none { it.id == device.id || it.controlUrl == device.controlUrl }) {
                            current + device
                        } else current
                    }
                    Log.i(TAG, "Discovered DLNA TV: $friendlyName ($manufacturer - $ip) at $controlUrl")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to parse device XML from $locationUrl: ${e.message}")
            }
        }
    }

    private fun extractAvTransportControlUrl(xml: String, locationUrl: String): String {
        // Look for serviceType AVTransport and its controlURL
        val serviceRegex = Pattern.compile(
            "<service>[\\s\\S]*?<serviceType>[^<]*?AVTransport:[^<]*?</serviceType>[\\s\\S]*?<controlURL>([^<]+)</controlURL>[\\s\\S]*?</service>",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = serviceRegex.matcher(xml)
        if (matcher.find()) {
            val rawControl = matcher.group(1)?.trim().orEmpty()
            return resolveUrl(locationUrl, rawControl)
        }
        // Fallback: simple search for controlURL
        val fallbackMatcher = Pattern.compile("<controlURL>([^<]*?AVTransport[^<]*?)</controlURL>", Pattern.CASE_INSENSITIVE).matcher(xml)
        if (fallbackMatcher.find()) {
            return resolveUrl(locationUrl, fallbackMatcher.group(1)?.trim().orEmpty())
        }
        return ""
    }

    private fun resolveUrl(baseUrl: String, relativeOrAbsolute: String): String {
        if (relativeOrAbsolute.startsWith("http://", ignoreCase = true) ||
            relativeOrAbsolute.startsWith("https://", ignoreCase = true)) {
            return relativeOrAbsolute
        }
        return try {
            val base = URL(baseUrl)
            URL(base, relativeOrAbsolute).toString()
        } catch (_: Exception) {
            relativeOrAbsolute
        }
    }

    private fun extractHeader(response: String, headerName: String): String? {
        val lines = response.lines()
        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2 && parts[0].trim().equals(headerName, ignoreCase = true)) {
                return parts[1].trim()
            }
        }
        return null
    }

    private fun extractTag(xml: String, tagName: String): String? {
        val matcher = Pattern.compile("<$tagName>([^<]+)</$tagName>", Pattern.CASE_INSENSITIVE).matcher(xml)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    /**
     * Casts video stream URL to the selected DLNA device with current playback position.
     */
    fun castToDevice(
        device: CastDevice,
        videoUrl: String,
        title: String,
        positionMs: Long = 0L,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                val escapedUrl = videoUrl.replace("&", "&amp;")
                val escapedTitle = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

                val setUriSoap = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                      <s:Body>
                        <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                          <InstanceID>0</InstanceID>
                          <CurrentURI>$escapedUrl</CurrentURI>
                          <CurrentURIMetaData>&lt;DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"&gt;&lt;item id="0" parentID="-1" restricted="1"&gt;&lt;dc:title&gt;$escapedTitle&lt;/dc:title&gt;&lt;upnp:class&gt;object.item.videoItem&lt;/upnp:class&gt;&lt;res protocolInfo="http-get:*:video/mp4:*"&gt;$escapedUrl&lt;/res&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;</CurrentURIMetaData>
                        </u:SetAVTransportURI>
                      </s:Body>
                    </s:Envelope>
                """.trimIndent()

                val setOk = sendSoapAction(device.controlUrl, "SetAVTransportURI", setUriSoap)
                if (!setOk) {
                    withContext(Dispatchers.Main) { onError("Failed to connect to ${device.name}") }
                    return@launch
                }

                // Send Play Action
                val playSoap = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                      <s:Body>
                        <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                          <InstanceID>0</InstanceID>
                          <Speed>1</Speed>
                        </u:Play>
                      </s:Body>
                    </s:Envelope>
                """.trimIndent()

                sendSoapAction(device.controlUrl, "Play", playSoap)

                // Seek if starting at saved progress (> 5 seconds)
                if (positionMs > 5000L) {
                    delay(800)
                    val totalSec = positionMs / 1000
                    val hours = totalSec / 3600
                    val minutes = (totalSec % 3600) / 60
                    val seconds = totalSec % 60
                    val targetTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                    val seekSoap = """
                        <?xml version="1.0" encoding="utf-8"?>
                        <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                          <s:Body>
                            <u:Seek xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                              <InstanceID>0</InstanceID>
                              <Unit>REL_TIME</Unit>
                              <Target>$targetTime</Target>
                            </u:Seek>
                          </s:Body>
                        </s:Envelope>
                    """.trimIndent()
                    sendSoapAction(device.controlUrl, "Seek", seekSoap)
                }

                _activeDevice.value = device
                _isCasting.value = true
                withContext(Dispatchers.Main) { onSuccess() }
                Log.i(TAG, "Successfully cast to ${device.name} ($escapedTitle)")
            } catch (e: Exception) {
                Log.e(TAG, "Cast execution error: ${e.message}", e)
                withContext(Dispatchers.Main) { onError(e.message ?: "Failed to cast") }
            }
        }
    }

    fun pause() {
        val device = _activeDevice.value ?: return
        scope.launch {
            val pauseSoap = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                  <s:Body>
                    <u:Pause xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                      <InstanceID>0</InstanceID>
                    </u:Pause>
                  </s:Body>
                </s:Envelope>
            """.trimIndent()
            sendSoapAction(device.controlUrl, "Pause", pauseSoap)
        }
    }

    fun resume() {
        val device = _activeDevice.value ?: return
        scope.launch {
            val playSoap = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                  <s:Body>
                    <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                      <InstanceID>0</InstanceID>
                      <Speed>1</Speed>
                    </u:Play>
                  </s:Body>
                </s:Envelope>
            """.trimIndent()
            sendSoapAction(device.controlUrl, "Play", playSoap)
        }
    }

    fun stopCasting() {
        val device = _activeDevice.value
        if (device != null) {
            scope.launch {
                val stopSoap = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                      <s:Body>
                        <u:Stop xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                          <InstanceID>0</InstanceID>
                        </u:Stop>
                      </s:Body>
                    </s:Envelope>
                """.trimIndent()
                sendSoapAction(device.controlUrl, "Stop", stopSoap)
            }
        }
        _activeDevice.value = null
        _isCasting.value = false
    }

    private fun sendSoapAction(controlUrl: String, action: String, xmlPayload: String): Boolean {
        return try {
            val mediaType = "text/xml; charset=\"utf-8\"".toMediaType()
            val requestBody = xmlPayload.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(controlUrl)
                .addHeader("SOAPAction", "\"urn:schemas-upnp-org:service:AVTransport:1#$action\"")
                .post(requestBody)
                .build()

            val response = SharedHttpClient.baseClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "SOAP $action failed on $controlUrl: ${e.message}")
            false
        }
    }

    /**
     * Launches external Google Cast or media player apps via Android's universal video intent.
     */
    fun launchExternalCastIntent(context: Context, videoUrl: String, title: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(videoUrl), "video/*")
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra("title", title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Cast with App (Google Cast / VLC / Web Video Caster)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No external video player / caster found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies direct stream link to clipboard for Smart TV browsers or network streaming.
     */
    fun copyStreamLink(context: Context, videoUrl: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("StreamHub Video Link", videoUrl)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Stream link copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
    }
}
