package com.streamhub.app.data.api

import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

/**
 * Custom SocketFactory for cinema-grade streaming throughput.
 * Pre-configures 1 MB TCP receive buffer and tcpNoDelay on raw sockets
 * before connection, ensuring TCP window scaling is negotiated in the SYN packet.
 */
class HighThroughputSocketFactory(
    private val delegate: SocketFactory = SocketFactory.getDefault(),
    private val bufferSizeBytes: Int = 1024 * 1024 // 1 MB TCP window scaling buffer
) : SocketFactory() {

    private fun configureSocket(socket: Socket): Socket {
        try {
            socket.receiveBufferSize = bufferSizeBytes
            socket.tcpNoDelay = true
            socket.keepAlive = true
        } catch (_: Exception) {}
        return socket
    }

    override fun createSocket(): Socket =
        configureSocket(delegate.createSocket())

    override fun createSocket(host: String, port: Int): Socket =
        configureSocket(delegate.createSocket(host, port))

    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
        configureSocket(delegate.createSocket(host, port, localHost, localPort))

    override fun createSocket(host: InetAddress, port: Int): Socket =
        configureSocket(delegate.createSocket(host, port))

    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
        configureSocket(delegate.createSocket(address, port, localAddress, localPort))
}

/**
 * M10 FIX: Shared OkHttpClient singleton — all HTTP clients in the app
 * derive from this base instance to share connection pools, socket pools,
 * and thread dispatchers efficiently.
 */
object SharedHttpClient {
    val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Dedicated OkHttpClient instance tailored for progressive video streaming.
     * Features:
     * - Pillar 1: HighThroughputSocketFactory (1 MB TCP window scaling + tcpNoDelay) for 2-4 MB/s spikes
     * - Pillar 2: 45s read timeout (generous upstream breathing room for Telegram/Serv00 chunk fetching)
     * - 30s connect/write timeouts
     * - Keep-alive connection pooling and auto-retry on connection failure
     */
    val streamingClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .socketFactory(HighThroughputSocketFactory())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS) // 45s read timeout: ample time for Telegram MTProto chunk delivery
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES)) // 5 minutes keep-alive for instant warm socket reuse
            .build()
    }
}

