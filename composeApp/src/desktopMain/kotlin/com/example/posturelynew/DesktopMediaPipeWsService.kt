package com.mobil80.posturely

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.client.plugins.websocket.ws
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.floatOrNull
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO
import java.io.File
import com.mobil80.posturely.PoseMetrics

class DesktopMediaPipeWsService {
    private var isInitialized = false
    private var isRunning = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val wsHost = "127.0.0.1"
    private val wsPort = 8765
    private val wsPath = "/" // root

    private var client: HttpClient? = null
    private var session: DefaultClientWebSocketSession? = null
    private var receiveJob: Job? = null
    private var heartbeatJob: Job? = null
    private var serverProcess: Process? = null

    // Streams
    private val _landmarksFlow = MutableStateFlow<List<Pair<Float, Float>>>(List(33) { Pair(0.5f, 0.5f) })
    val landmarksFlow: StateFlow<List<Pair<Float, Float>>> = _landmarksFlow.asStateFlow()
    private val _scoreFlow = MutableStateFlow(0)
    val scoreFlow: StateFlow<Int> = _scoreFlow.asStateFlow()
    private val _postureStatusFlow = MutableStateFlow("NO SIGNAL")
    val postureStatusFlow: StateFlow<String> = _postureStatusFlow.asStateFlow()

    interface PoseLandmarkerListener {
        fun onPoseLandmarkerResult(landmarks: List<Pair<Float, Float>>, score: Int, status: String)
        fun onPoseLandmarkerError(error: String)
    }
    private var listener: PoseLandmarkerListener? = null
    fun setListener(listener: PoseLandmarkerListener) { this.listener = listener }
    
    fun getStatus(): String {
        return "MediaPipe WS Service - Initialized: $isInitialized, Running: $isRunning, Session: ${session != null}, Server: ${serverProcess?.isAlive ?: false}, Landmarks: ${_landmarksFlow.value.size}, Score: ${_scoreFlow.value}, Status: ${_postureStatusFlow.value}"
    }

    fun setupPoseLandmarker() {
        try {
            println("DesktopMediaPipeWsService: Starting setup...")
            client = HttpClient(CIO) { install(WebSockets) }
            println("DesktopMediaPipeWsService: Connecting to ws://$wsHost:$wsPort$wsPath ...")
            serviceScope.launch {
                try {
                    // Try connect, if fails, start local server and retry
                    var connected = false
                    repeat(2) { attempt ->
                        try {
                            println("DesktopMediaPipeWsService: Attempt $attempt to connect...")
                            client!!.ws(host = wsHost, port = wsPort, path = wsPath) {
                                session = this
                                connected = true
                                println("DesktopMediaPipeWsService: WebSocket connected successfully!")
                                startReceiveLoop()

                                val initJson = "{" + "\"type\":\"init\"" + "}"
                                println("DesktopMediaPipeWsService: Sending init message: $initJson")
                                send(Frame.Text(initJson))
                                var attempts = 0
                                while (!isInitialized && attempts < 100) {
                                    delay(100)
                                    attempts++
                                    if (attempts % 10 == 0) {
                                        println("DesktopMediaPipeWsService: Waiting for init response... attempt $attempts")
                                    }
                                }
                                if (!isInitialized) {
                                    println("DesktopMediaPipeWsService: Init timed out after $attempts attempts")
                                } else {
                                    println("DesktopMediaPipeWsService: Init succeeded!")
                                }

                                // Keep WS session alive until this coroutine is cancelled
                                try {
                                    while (isActive) {
                                        delay(60000)
                                    }
                                } catch (_: CancellationException) {}
                            }
                        } catch (e: Exception) {
                            println("DesktopMediaPipeWsService: Connect attempt $attempt failed: ${e.message}")
                            if (attempt == 0) {
                                println("DesktopMediaPipeWsService: Starting local server and retrying...")
                                startLocalServerIfNeeded()
                                delay(1000)
                            }
                        }
                    }
                    if (!connected) {
                        println("DesktopMediaPipeWsService: All connection attempts failed")
                        listener?.onPoseLandmarkerError("WS connect error: could not connect to local server")
                    } else {
                        println("DesktopMediaPipeWsService: Connection established successfully")
                    }
                } catch (e: Exception) {
                    println("DesktopMediaPipeWsService: WS connect error: ${e.message}")
                    listener?.onPoseLandmarkerError("WS connect error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("DesktopMediaPipeWsService: setup error: ${e.message}")
            listener?.onPoseLandmarkerError(e.message ?: "setup error")
        }
    }

    private fun startLocalServerIfNeeded() {
        try {
            if (serverProcess?.isAlive == true) {
                println("DesktopMediaPipeWsService: Server already running")
                return
            }
            val resourcesDir = resolveResourcesDir()
            println("DesktopMediaPipeWsService: Resources directory: ${resourcesDir.absolutePath}")
            val script = File(resourcesDir, "ws_pose_server.py")
            if (!script.exists()) {
                println("DesktopMediaPipeWsService: ws_pose_server.py not found at ${script.absolutePath}")
                return
            }
            println("DesktopMediaPipeWsService: Found script at ${script.absolutePath}")
            val python = resolvePython(resourcesDir)
            println("DesktopMediaPipeWsService: Using Python: $python")
            println("DesktopMediaPipeWsService: Starting local WS server...")
            val pb = ProcessBuilder(python, script.absolutePath)
            pb.directory(resourcesDir)
            pb.redirectErrorStream(true)
            serverProcess = pb.start()
            println("DesktopMediaPipeWsService: Server process started with PID: ${serverProcess?.pid()}")
            
            // Note: Server startup delay is handled by the calling coroutine
            println("DesktopMediaPipeWsService: Server process started, waiting for startup...")
        } catch (e: Exception) {
            println("DesktopMediaPipeWsService: failed to start local server: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun resolveResourcesDir(): File {
        // Try classpath resource
        val res = this::class.java.classLoader.getResource("")
        if (res != null) {
            val dir = File(res.path).parentFile
            if (dir.exists()) return dir
        }
        // Fall back to project layout during dev
        val cwd = File(System.getProperty("user.dir"))
        val candidates = listOf(
            File(cwd, "composeApp/src/desktopMain/resources"),
            cwd
        )
        return candidates.firstOrNull { it.exists() } ?: cwd
    }

    private fun resolvePython(resourcesDir: File): String {
        val venv = File(resourcesDir, "venv/bin/python3.11")
        if (venv.exists()) return venv.absolutePath
        val venvPy = File(resourcesDir, "venv/bin/python")
        if (venvPy.exists()) return venvPy.absolutePath
        return "python3"
    }

    private fun startReceiveLoop() {
        val s = session ?: return
        receiveJob = serviceScope.launch {
            try {
                for (frame in s.incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        handleMessage(text)
                    }
                }
            } catch (e: Exception) {
                println("DesktopMediaPipeWsService: receive loop error: ${e.message}")
            }
        }
        // heartbeat
        heartbeatJob = serviceScope.launch {
            while (isRunning) {
                delay(5000)
                try {
                    s.send(Frame.Text("{\"type\":\"ping\"}"))
                } catch (_: Exception) { }
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val root = Json.parseToJsonElement(text).jsonObject
            when (root["type"]?.jsonPrimitive?.content) {
                "init_response" -> {
                    val success = root["success"]?.jsonPrimitive?.booleanOrNull
                        ?: root["data"]?.jsonObject?.get("success")?.jsonPrimitive?.booleanOrNull
                        ?: false
                    isInitialized = success
                    println("DesktopMediaPipeWsService: init_response success=$success")
                }
                "detection" -> {
                    val data = root
                    val success = data["success"]?.jsonPrimitive?.booleanOrNull ?: false
                    if (success) {
                        val arr = data["landmarks"]?.jsonArray
                        if (arr != null) {
                            val list = arr.mapNotNull { el ->
                                val o = el.jsonObject
                                val x = o["x"]?.jsonPrimitive?.floatOrNull
                                val y = o["y"]?.jsonPrimitive?.floatOrNull
                                if (x != null && y != null) Pair(x, y) else null
                            }
                            if (list.size == 33) {
                                _landmarksFlow.value = list
                                // Compute metrics and score/status like mobile
                                val metrics = calculateRealMetrics(list)
                                updatePostureStatus(metrics)
                                listener?.onPoseLandmarkerResult(list, _scoreFlow.value, _postureStatusFlow.value)
                            }
                        }
                    }
                }
                "error" -> {
                    val msg = root["message"]?.jsonPrimitive?.content
                    if (msg != null) listener?.onPoseLandmarkerError(msg)
                }
            }
        } catch (e: Exception) {
            println("DesktopMediaPipeWsService: parse error: ${e.message}")
        }
    }

    fun detectAsync(bufferedImage: BufferedImage, timestamp: Long) {
        if (!isInitialized || !isRunning) return
        val s = session ?: return
        try {
            val b64 = bufferedImageToBase64Jpeg(bufferedImage)
            val json = "{" +
                "\"type\":\"detect\"," +
                "\"image\":\"$b64\"," +
                "\"ts\":$timestamp" +
                "}"
            serviceScope.launch { s.send(Frame.Text(json)) }
        } catch (e: Exception) {
            println("DesktopMediaPipeWsService: detect error: ${e.message}")
        }
    }

    private fun bufferedImageToBase64Jpeg(bufferedImage: BufferedImage): String {
        val baos = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "JPEG", baos)
        val bytes = baos.toByteArray()
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun startTracking() {
        isRunning = true
    }

    fun stopTracking() {
        isRunning = false
    }

    fun close() {
        try {
            isRunning = false
            receiveJob?.cancel()
            heartbeatJob?.cancel()
            serviceScope.launch {
                try { session?.send(Frame.Text("{\"type\":\"close\"}")) } catch (_: Exception) {}
                try { client?.close() } catch (_: Exception) {}
            }
            isInitialized = false
            try { serverProcess?.destroy() } catch (_: Exception) {}
        } catch (_: Exception) {}
    }

    fun isInitialized(): Boolean = isInitialized

    // --- Posture scoring logic (mirrors DesktopMediaPipeService/mobile) ---
    private fun calculateRealMetrics(landmarks: List<Pair<Float, Float>>): PoseMetrics {
        if (landmarks.size < 33) return PoseMetrics(0.0, 0.0, 0.0, 0.0, 0.0)

        val nose = landmarks[0]
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]

        val torsoCenterX = (leftHip.first + rightHip.first) / 2f
        val torsoCenterY = (leftHip.second + rightHip.second) / 2f
        val shoulderCenterX = (leftShoulder.first + rightShoulder.first) / 2f
        val shoulderCenterY = (leftShoulder.second + rightShoulder.second) / 2f

        val torsoTilt = calculateAngleFromVertical(
            shoulderCenterX, shoulderCenterY,
            torsoCenterX,  torsoCenterY
        )

        val shoulderTilt = calculateShoulderTilt(leftShoulder, rightShoulder)
        val headZDelta = (nose.second - shoulderCenterY).toDouble()
        val neckFlex = calculateAngleFromVertical(
            nose.first, nose.second,
            shoulderCenterX, shoulderCenterY
        )
        val shoulderAsymY = kotlin.math.abs((leftShoulder.second - rightShoulder.second).toDouble())

        return PoseMetrics(
            torsoTilt = torsoTilt,
            shoulderTilt = shoulderTilt,
            neckFlex = neckFlex,
            headZDelta = headZDelta,
            shoulderAsymY = shoulderAsymY
        )
    }

    private fun calculateAngleFromVertical(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        val dx = (x2 - x1).toDouble()
        val dy = (y2 - y1).toDouble()
        val angle = kotlin.math.atan2(dx, dy) * 180.0 / kotlin.math.PI
        return kotlin.math.abs(angle)
    }

    private fun calculateShoulderTilt(leftShoulder: Pair<Float, Float>, rightShoulder: Pair<Float, Float>): Double {
        val heightDiff = (leftShoulder.second - rightShoulder.second).toDouble()
        return kotlin.math.abs(heightDiff) * 100.0
    }

    private fun updatePostureStatus(metrics: PoseMetrics) {
        var newScore = 100
        if (metrics.torsoTilt > 15.0) newScore -= 20
        if (metrics.shoulderTilt > 0.1) newScore -= 15
        if (metrics.neckFlex > 20.0) newScore -= 25
        if (metrics.shoulderAsymY > 0.05) newScore -= 10

        _scoreFlow.value = newScore.coerceIn(0, 100)
        _postureStatusFlow.value = when {
            _scoreFlow.value >= 80 -> "EXCELLENT"
            _scoreFlow.value >= 60 -> "GOOD"
            _scoreFlow.value >= 40 -> "FAIR"
            _scoreFlow.value >= 20 -> "POOR"
            else -> "VERY POOR"
        }
    }
}


