package com.mobil80.posturely

import androidx.compose.ui.graphics.ImageBitmap
import java.awt.image.BufferedImage
import java.io.*
import java.util.concurrent.*
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.nio.file.WatchKey
import java.nio.file.WatchEvent
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream
import com.mobil80.posturely.PoseMetrics

class DesktopMediaPipeService {
    private var isInitialized = false
    private var isRunning = false
    private var serviceReady = false
    private var pythonProcess: Process? = null
    private var responseWatcherJob: Job? = null
    private var heartbeatJob: Job? = null
    private var serviceScope = CoroutineScope(Dispatchers.IO)
    
    // Communication directories
    private val commandDir = File("/tmp/posture_commands")
    private val responseDir = File("/tmp/posture_responses")
    private val pidFile = File("/tmp/posture_python.pid")
    
    // Posture tracking variables
    private var poseData = "No pose data available"
    private var landmarks = List(33) { Pair(0.5f, 0.5f) }
    private var score = 0
    private var postureStatus = "NO SIGNAL"
    
    // Service type detection
    private var serviceType: ServiceType = ServiceType.UNKNOWN
    private var bundledBinaryPath: String? = null
    
    enum class ServiceType {
        BUNDLED_BINARY,    // PyInstaller bundled binary
        PYTHON_SCRIPT,     // Python script with virtual environment
        UNKNOWN            // Not determined yet
    }
    
    // Coroutine scope for async operations
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
    
    fun setListener(listener: PoseLandmarkerListener) {
        this.listener = listener
    }
    
    fun setupPoseLandmarker() {
        try {
            println("DesktopMediaPipeService: ===== SETUP START =====")
            println("DesktopMediaPipeService: Setting up Python MediaPipe subprocess...")
            
            // Detect service type and find the best available option
            println("DesktopMediaPipeService: Step 1: Detecting service type...")
            detectServiceType()
            println("DesktopMediaPipeService: Service type detected: $serviceType")
            
            // Create communication directories
            println("DesktopMediaPipeService: Step 2: Setting up communication directories...")
            setupCommunicationDirs()
            
            // Start Python subprocess based on detected service type
            println("DesktopMediaPipeService: Step 3: Starting Python subprocess...")
            startPythonProcess()
            
            // Set service as running before starting watchers
            isRunning = true

            // Start response watcher BEFORE init to ensure we capture init_response
            println("DesktopMediaPipeService: Step 4: Starting response watcher...")
            startResponseWatcher()

            // Small delay to ensure watcher loop is active
            Thread.sleep(150)

            // Probing service readiness via ping/pong
            println("DesktopMediaPipeService: Probing Python service readiness (ping)...")
            sendCommandToPython(mapOf("type" to "ping"))
            var attempts = 0
            val maxAttempts = 50 // up to 5s
            while (!serviceReady && attempts < maxAttempts) {
                Thread.sleep(100)
                attempts++
            }
            if (!serviceReady) {
                println("DesktopMediaPipeService: Warning: Service did not respond to ping in time; continuing with init")
            } else {
                println("DesktopMediaPipeService: Python service responded to ping; proceeding with init")
            }

            // Initialize MediaPipe through Python
            println("DesktopMediaPipeService: Step 5: Starting MediaPipe initialization...")
            initializeMediaPipe()

            // Start heartbeat monitoring
            println("DesktopMediaPipeService: Step 6: Starting heartbeat monitoring...")
            startHeartbeat()

            println("DesktopMediaPipeService: ===== SETUP COMPLETE =====")
            
        } catch (e: Exception) {
            println("DesktopMediaPipeService: ===== SETUP FAILED =====")
            println("DesktopMediaPipeService: Error setting up MediaPipe: ${e.message}")
            e.printStackTrace()
            isInitialized = false
            
            // Log additional details about the failure
            println("DesktopMediaPipeService: Exception type: ${e.javaClass.simpleName}")
            println("DesktopMediaPipeService: Exception cause: ${e.cause}")
            if (e.cause != null) {
                e.cause?.printStackTrace()
            }
        }
    }
    
    private fun detectServiceType() {
        println("DesktopMediaPipeService: ===== DETECTING SERVICE TYPE =====")
        println("DesktopMediaPipeService: Step 1: Looking for bundled binary...")
        
        // First, try to find bundled binary
        val bundledBinary = findBundledBinary()
        if (bundledBinary != null) {
            serviceType = ServiceType.BUNDLED_BINARY
            bundledBinaryPath = bundledBinary
            println("DesktopMediaPipeService: ✅ SUCCESS: Found bundled binary: $bundledBinary")
            println("DesktopMediaPipeService: Service type set to: $serviceType")
            return
        }
        
        println("DesktopMediaPipeService: ❌ No bundled binary found, falling back to Python script...")
        
        // Fallback to Python script
        val pythonScript = findPythonScript()
        if (pythonScript != null) {
            serviceType = ServiceType.PYTHON_SCRIPT
            println("DesktopMediaPipeService: ✅ SUCCESS: Using Python script: $pythonScript")
            println("DesktopMediaPipeService: Service type set to: $serviceType")
            return
        }
        
        // No service found
        println("DesktopMediaPipeService: ❌ FATAL: No MediaPipe service found!")
        throw RuntimeException("No MediaPipe service found. Please build the bundled binary or ensure Python script is available.")
    }
    
    private fun findBundledBinary(): String? {
        val osName = System.getProperty("os.name").lowercase()
        val possiblePaths = mutableListOf<String>()
        
        if (osName.contains("mac")) {
            // macOS: look for .app bundle or binary
            possiblePaths.addAll(listOf(
                "pose_server.app/Contents/MacOS/pose_server",
                "pose_server",
                "dist/pose_server.app/Contents/MacOS/pose_server",
                "dist/pose_server"
            ))
        } else if (osName.contains("windows")) {
            // Windows: look for .exe
            possiblePaths.addAll(listOf(
                "pose_server.exe",
                "dist/pose_server.exe"
            ))
        } else {
            // Linux: look for binary
            possiblePaths.addAll(listOf(
                "pose_server",
                "dist/pose_server"
            ))
        }
        
        // Check relative to resources directory
        val resourcesDir = getResourcesDirectory()
        println("DesktopMediaPipeService: Checking resources directory: ${resourcesDir.absolutePath}")
        
        for (path in possiblePaths) {
            val file = File(resourcesDir, path)
            println("DesktopMediaPipeService: Checking path: ${file.absolutePath} (exists: ${file.exists()}, executable: ${file.canExecute()})")
            if (file.exists() && file.canExecute()) {
                println("DesktopMediaPipeService: Found bundled binary at: ${file.absolutePath}")
                return file.absolutePath
            }
        }
        
        // Check relative to current working directory
        val currentDir = File(System.getProperty("user.dir"))
        println("DesktopMediaPipeService: Checking current working directory: ${currentDir.absolutePath}")
        
        for (path in possiblePaths) {
            val file = File(path)
            println("DesktopMediaPipeService: Checking current dir path: ${file.absolutePath} (exists: ${file.exists()}, executable: ${file.canExecute()})")
            if (file.exists() && file.canExecute()) {
                println("DesktopMediaPipeService: Found bundled binary at current dir: ${file.absolutePath}")
                return file.absolutePath
            }
        }
        
        // Check relative to project root (common case for KMP apps)
        val projectRoot = File(currentDir, "composeApp/src/desktopMain/resources")
        if (projectRoot.exists()) {
            println("DesktopMediaPipeService: Checking project root resources: ${projectRoot.absolutePath}")
            
            for (path in possiblePaths) {
                val file = File(projectRoot, path)
                println("DesktopMediaPipeService: Checking project root path: ${file.absolutePath} (exists: ${file.exists()}, executable: ${file.canExecute()})")
                if (file.exists() && file.canExecute()) {
                    println("DesktopMediaPipeService: Found bundled binary at project root: ${file.absolutePath}")
                    return file.absolutePath
                }
            }
        }
        
        return null
    }
    
    private fun findPythonScript(): String? {
        val possiblePaths = listOf(
            "mediapipe_pose_detector.py",
            "src/desktopMain/resources/mediapipe_pose_detector.py",
            "composeApp/src/desktopMain/resources/mediapipe_pose_detector.py",
            "resources/mediapipe_pose_detector.py"
        )
        
        // Check relative to resources directory
        val resourcesDir = getResourcesDirectory()
        for (path in possiblePaths) {
            val file = File(resourcesDir, path)
            if (file.exists()) {
                return file.absolutePath
            }
        }
        
        // Check relative to current working directory
        for (path in possiblePaths) {
            val file = File(path)
            if (file.exists()) {
                return file.absolutePath
            }
        }
        
        // Fallback to classpath resource
        val resource = javaClass.classLoader.getResource("mediapipe_pose_detector.py")
        if (resource != null) {
            return resource.path
        }
        
        return null
    }
    
    private fun getResourcesDirectory(): File {
        // Try multiple approaches to find the resources directory
        
        // Approach 1: Try to get from classpath
        val resource = javaClass.classLoader.getResource("")
        if (resource != null) {
            val classpathDir = File(resource.path).parentFile
            if (classpathDir.exists()) {
                println("DesktopMediaPipeService: Found resources directory via classpath: ${classpathDir.absolutePath}")
                return classpathDir
            }
        }
        
        // Approach 2: Try relative to current working directory
        val currentDir = File(System.getProperty("user.dir"))
        val possibleResourcesDirs = listOf(
            currentDir,
            File(currentDir, "composeApp/src/desktopMain/resources"),
            File(currentDir, "src/desktopMain/resources"),
            File(currentDir, "resources")
        )
        
        for (dir in possibleResourcesDirs) {
            if (dir.exists() && dir.isDirectory) {
                // Check if this looks like a resources directory
                val hasPythonScript = File(dir, "mediapipe_pose_detector.py").exists()
                val hasModels = File(dir, "pose_landmarker_full.task").exists()
                
                if (hasPythonScript || hasModels) {
                    println("DesktopMediaPipeService: Found resources directory via working directory: ${dir.absolutePath}")
                    return dir
                }
            }
        }
        
        // Approach 3: Fallback to current working directory
        println("DesktopMediaPipeService: Using fallback resources directory: ${currentDir.absolutePath}")
        return currentDir
    }
    
    private fun setupCommunicationDirs() {
        try {
            commandDir.mkdirs()
            responseDir.mkdirs()
            println("DesktopMediaPipeService: Communication directories created: ${commandDir.absolutePath}, ${responseDir.absolutePath}")
        } catch (e: Exception) {
            println("DesktopMediaPipeService: Error creating communication directories: ${e.message}")
            throw e
        }
    }
    
    private fun startPythonProcess() {
        try {
            println("DesktopMediaPipeService: ===== PYTHON PROCESS START =====")
            
            when (serviceType) {
                ServiceType.BUNDLED_BINARY -> startBundledBinary()
                ServiceType.PYTHON_SCRIPT -> startPythonScript()
                else -> throw RuntimeException("Unknown service type: $serviceType")
            }
            
            println("DesktopMediaPipeService: ===== PYTHON PROCESS START COMPLETE =====")
            
        } catch (e: Exception) {
            println("DesktopMediaPipeService: ===== PYTHON PROCESS START FAILED =====")
            println("DesktopMediaPipeService: Error starting Python process: ${e.message}")
            e.printStackTrace()
            throw RuntimeException("Failed to start Python MediaPipe service: ${e.message}")
        }
    }
    
    private fun startBundledBinary() {
        println("DesktopMediaPipeService: Starting bundled binary...")
        
        val binaryPath = bundledBinaryPath ?: throw RuntimeException("Bundled binary path not set")
        val binaryFile = File(binaryPath)
        
        if (!binaryFile.exists()) {
            throw RuntimeException("Bundled binary not found: $binaryPath")
        }
        
        if (!binaryFile.canExecute()) {
            // Try to make it executable
            binaryFile.setExecutable(true)
        }
        
        // Start the bundled binary
        val processBuilder = ProcessBuilder(binaryFile.absolutePath)
        
        // Set working directory to the binary's directory for better model loading
        val workingDir = binaryFile.parentFile
        processBuilder.directory(workingDir)
        println("DesktopMediaPipeService: Working directory set to: ${workingDir.absolutePath}")
        
        // Redirect error stream to output for debugging
        processBuilder.redirectErrorStream(true)
        
        println("DesktopMediaPipeService: Starting bundled binary...")
        pythonProcess = processBuilder.start()
        
        // Wait for process to start and check if it's alive
        waitForProcessStartup()
    }
    
    private fun startPythonScript() {
        println("DesktopMediaPipeService: Starting Python script...")
        
        val scriptPath = findPythonScript() ?: throw RuntimeException("Python script not found")
        println("DesktopMediaPipeService: Python script path: $scriptPath")
        
        // Use the virtual environment Python interpreter
        val pythonCommand = if (System.getProperty("os.name").lowercase().contains("windows")) {
            "python"
        } else {
            // Use the virtual environment Python interpreter
            val venvPython = File(scriptPath).parentFile.resolve("venv/bin/python3.11")
            if (venvPython.exists()) {
                venvPython.absolutePath
            } else {
                // Fallback to system Python
                try {
                    if (Runtime.getRuntime().exec(arrayOf("python3.11", "--version")).waitFor() == 0) {
                        "python3.11"
                    } else if (Runtime.getRuntime().exec(arrayOf("python3.10", "--version")).waitFor() == 0) {
                        "python3.10"
                    } else if (Runtime.getRuntime().exec(arrayOf("python3.9", "--version")).waitFor() == 0) {
                        "python3.9"
                    } else {
                        "python3"
                    }
                } catch (e: Exception) {
                    println("DesktopMediaPipeService: Error checking Python versions: ${e.message}")
                    "python3"
                }
            }
        }
        
        println("DesktopMediaPipeService: Using Python command: $pythonCommand")
        
        // Start Python subprocess
        val processBuilder = ProcessBuilder(
            pythonCommand,
            scriptPath
        )
        
        // Set working directory to the resources folder for better model loading
        val resourcesDir = File(scriptPath).parentFile
        processBuilder.directory(resourcesDir)
        println("DesktopMediaPipeService: Working directory set to: ${resourcesDir.absolutePath}")
        
        // Set environment variables to use the virtual environment
        val env = processBuilder.environment()
        val venvPath = resourcesDir.resolve("venv")
        if (venvPath.exists()) {
            env["VIRTUAL_ENV"] = venvPath.absolutePath
            env["PATH"] = "${venvPath.resolve("bin")}:${env["PATH"]}"
            println("DesktopMediaPipeService: Virtual environment activated: ${venvPath.absolutePath}")
        }
        
        // Redirect error stream to output for debugging
        processBuilder.redirectErrorStream(true)
        
        println("DesktopMediaPipeService: Starting Python subprocess...")
        pythonProcess = processBuilder.start()
        
        // Wait for process to start and check if it's alive
        waitForProcessStartup()
    }
    
    private fun waitForProcessStartup() {
        // Check if process started successfully
        if (pythonProcess?.isAlive == true) {
            println("DesktopMediaPipeService: Process started successfully (PID: ${pythonProcess?.pid()})")
        } else {
            throw RuntimeException("Process failed to start")
        }
        
        // Wait a moment for the process to initialize
        println("DesktopMediaPipeService: Waiting for process to initialize...")
        Thread.sleep(2000)
        
        // Check if process is still alive
        if (pythonProcess?.isAlive != true) {
            println("DesktopMediaPipeService: Process died during startup")
            println("DesktopMediaPipeService: Exit code: ${pythonProcess?.exitValue()}")
            
            // Try to read any output from the process
            try {
                val reader = BufferedReader(InputStreamReader(pythonProcess?.inputStream ?: System.`in`))
                var line: String?
                println("DesktopMediaPipeService: Process output:")
                while (reader.readLine().also { line = it } != null) {
                    println("DesktopMediaPipeService: Output: $line")
                }
            } catch (e: Exception) {
                println("DesktopMediaPipeService: Could not read process output: ${e.message}")
            }
            
            throw RuntimeException("Process died during startup")
        }
        
        // Check if PID file was created
        if (!pidFile.exists()) {
            println("DesktopMediaPipeService: Warning: PID file not created by process")
        } else {
            val pid = pidFile.readText().trim()
            println("DesktopMediaPipeService: Process PID file found: $pid")
        }
        
        println("DesktopMediaPipeService: Process is stable and ready")
    }
    
    private fun startResponseWatcher() {
        responseWatcherJob = serviceScope.launch(Dispatchers.IO) {
            try {
                println("DesktopMediaPipeService: Starting response watcher...")
                println("DesktopMediaPipeService: Response watcher - isRunning: $isRunning, pythonProcess alive: ${pythonProcess?.isAlive}")
                
                while (isRunning && pythonProcess?.isAlive == true) {
                    try {
                        // Look for response files
                        val responseFiles = responseDir.listFiles { file ->
                            file.isFile && file.name.endsWith(".json")
                        }
                        
                        responseFiles?.forEach { responseFile ->
                            try {
                                val responseJson = responseFile.readText()
                                println("DesktopMediaPipeService: Found response file: ${responseFile.name}")
                                
                                // Parse and handle response
                                handlePythonResponse(responseJson)
                                
                                // Remove response file
                                responseFile.delete()
                                
                            } catch (e: Exception) {
                                println("DesktopMediaPipeService: Error reading response file ${responseFile.name}: ${e.message}")
                                // Try to remove the problematic file
                                try {
                                    responseFile.delete()
                                } catch (ex: Exception) {
                                    println("DesktopMediaPipeService: Could not delete problematic response file: ${ex.message}")
                                }
                            }
                        }
                        
                        // Small delay to prevent excessive CPU usage
                        delay(100)
                        
                    } catch (e: Exception) {
                        println("DesktopMediaPipeService: Error in response watcher: ${e.message}")
                        e.printStackTrace()
                        delay(1000) // Wait before retrying
                    }
                }
                
                println("DesktopMediaPipeService: Response watcher stopped")
                
            } catch (e: Exception) {
                println("DesktopMediaPipeService: Fatal error in response watcher: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun startHeartbeat() {
        heartbeatJob = serviceScope.launch(Dispatchers.IO) {
            try {
                println("DesktopMediaPipeService: Starting heartbeat...")
                println("DesktopMediaPipeService: Heartbeat - isRunning: $isRunning, pythonProcess alive: ${pythonProcess?.isAlive}")
                
                while (isRunning && pythonProcess?.isAlive == true) {
                    delay(5000) // Send heartbeat every 5 seconds
                    
                    if (isRunning && pythonProcess?.isAlive == true) {
                        val pingCommand = mapOf("type" to "ping")
                        sendCommandToPython(pingCommand)
                        
                        // Wait for pong response
                        delay(1000)
                        
                        // If process is dead after ping, handle it
                        if (pythonProcess?.isAlive != true) {
                            println("DesktopMediaPipeService: Python process died during heartbeat")
                            handleProcessDeath()
                            break
                        }
                        
                        // Also send status command every few heartbeats
                        if (System.currentTimeMillis() % 30000 < 5000) { // Every 30 seconds
                            val statusCommand = mapOf("type" to "status")
                            sendCommandToPython(statusCommand)
                        }
                    }
                }
            } catch (e: Exception) {
                println("DesktopMediaPipeService: Error in heartbeat: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun handleProcessDeath() {
        println("DesktopMediaPipeService: Handling Python process death...")
        isInitialized = false
        isRunning = false
        
        // Try to restart the process if we're still supposed to be running
        if (isRunning) {
            println("DesktopMediaPipeService: Attempting to restart Python process...")
            try {
                startPythonProcess()
                initializeMediaPipe()
                println("DesktopMediaPipeService: Python process restarted successfully")
            } catch (e: Exception) {
                println("DesktopMediaPipeService: Failed to restart Python process: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun handlePythonResponse(responseJson: String) {
        try {
            val root = Json.parseToJsonElement(responseJson).jsonObject
            val responseType = root["type"]?.jsonPrimitive?.contentOrNull
            val data = root["data"]?.jsonObject

            when (responseType) {
                "init_response" -> {
                    val success = data?.get("success")?.jsonPrimitive?.booleanOrNull ?: false
                    isInitialized = success
                    val message = data?.get("message")?.jsonPrimitive?.contentOrNull
                    println("DesktopMediaPipeService: MediaPipe initialization ${if (success) "succeeded" else "failed"}")
                    if (!success && message != null) {
                        println("DesktopMediaPipeService: Initialization error: $message")
                    }
                }

                "detection_result" -> {
                    if (data != null) {
                        val success = data["success"]?.jsonPrimitive?.booleanOrNull ?: false
                        if (success) {
                            val landmarksArray = data["landmarks"]?.jsonArray
                            if (landmarksArray != null) {
                                val newLandmarks = landmarksArray.mapNotNull { lm ->
                                    val obj = lm.jsonObject
                                    val x = obj["x"]?.jsonPrimitive?.floatOrNull
                                    val y = obj["y"]?.jsonPrimitive?.floatOrNull
                                    if (x != null && y != null) Pair(x, y) else null
                                }
                                if (newLandmarks.size == 33) {
                                    landmarks = newLandmarks
                                    _landmarksFlow.value = newLandmarks
                                    updatePostureMetrics(newLandmarks)
                                    listener?.onPoseLandmarkerResult(landmarks, score, postureStatus)
                                }
                            }
                        } else {
                            val message = data["message"]?.jsonPrimitive?.contentOrNull ?: "No pose detected"
                            println("DesktopMediaPipeService: $message")
                        }
                    }
                }

                "pong" -> {
                    val alive = data?.get("alive")?.jsonPrimitive?.booleanOrNull ?: false
                    val mediapipeAvailable = data?.get("mediapipe_available")?.jsonPrimitive?.booleanOrNull ?: false
                    val initialized = data?.get("initialized")?.jsonPrimitive?.booleanOrNull ?: false
                    if (alive) serviceReady = true
                    println("DesktopMediaPipeService: Received heartbeat response (alive: $alive, MediaPipe: $mediapipeAvailable, initialized: $initialized)")
                }

                "status_response" -> {
                    val mediapipeAvailable = data?.get("mediapipe_available")?.jsonPrimitive?.booleanOrNull ?: false
                    val initialized = data?.get("initialized")?.jsonPrimitive?.booleanOrNull ?: false
                    println("DesktopMediaPipeService: Service status - MediaPipe: $mediapipeAvailable, Initialized: $initialized")
                }

                "error" -> {
                    val message = data?.get("message")?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                    println("DesktopMediaPipeService: Python error: $message")
                    listener?.onPoseLandmarkerError(message)
                }

                else -> {
                    println("DesktopMediaPipeService: Unknown response type: $responseType")
                }
            }
        } catch (e: Exception) {
            println("DesktopMediaPipeService: Error parsing Python response: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun parseJsonResponse(jsonString: String): Map<String, Any> {
        // Simple JSON parsing for our specific response format
        // In production, you might want to use kotlinx.serialization or Gson
        return try {
            println("DesktopMediaPipeService: Parsing JSON response: $jsonString")
            
            // This is a simplified parser - you might want to use kotlinx.serialization or Gson
            val cleaned = jsonString.trim().removeSurrounding("{", "}")
            if (cleaned == jsonString) {
                // No braces found, try to parse as is
                println("DesktopMediaPipeService: No braces found in response")
                return emptyMap()
            }
            
            val pairs = cleaned.split(",").associate { pair ->
                val colonIndex = pair.indexOf(":")
                if (colonIndex == -1) {
                    println("DesktopMediaPipeService: Invalid pair format: $pair")
                    return@associate "error" to "Invalid format"
                }
                
                val key = pair.substring(0, colonIndex).trim().removeSurrounding("\"")
                val value = pair.substring(colonIndex + 1).trim().removeSurrounding("\"")
                key to value
            }
            
            println("DesktopMediaPipeService: Parsed response: $pairs")
            pairs
            
        } catch (e: Exception) {
            println("DesktopMediaPipeService: JSON parsing error: ${e.message}")
            emptyMap()
        }
    }
    
    private fun handleDetectionResult(result: Map<*, *>) {
        try {
            val success = result["success"] as? Boolean ?: false
            
            if (success) {
                val landmarksData = result["landmarks"] as? List<*>
                if (landmarksData != null) {
                    // Convert landmarks to our format
                    val newLandmarks = landmarksData.mapNotNull { landmark ->
                        if (landmark is Map<*, *>) {
                            val x = (landmark["x"] as? Number)?.toFloat() ?: 0.5f
                            val y = (landmark["y"] as? Number)?.toFloat() ?: 0.5f
                            Pair(x, y)
                        } else null
                    }
                    
                    if (newLandmarks.size == 33) {
                        landmarks = newLandmarks
                        _landmarksFlow.value = newLandmarks
                        
                        // Calculate posture metrics and update status
                        updatePostureMetrics(newLandmarks)
                        
                        // Notify listener
                        listener?.onPoseLandmarkerResult(landmarks, score, postureStatus)
                    }
                }
            } else {
                val message = result["message"] as? String ?: "No pose detected"
                println("DesktopMediaPipeService: $message")
            }
            
        } catch (e: Exception) {
            println("DesktopMediaPipeService: Error handling detection result: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun initializeMediaPipe() {
        try {
            println("DesktopMediaPipeService: Sending MediaPipe initialization command...")
            
            // Send initialization command to Python
            val initCommand = mapOf(
                "type" to "init",
                "model_path" to "pose_landmarker_full.task"
            )
            
            sendCommandToPython(initCommand)
            
            // Wait for initialization response
            println("DesktopMediaPipeService: Waiting for MediaPipe initialization response...")
            var attempts = 0
            val maxAttempts = 50 // Wait up to 5 seconds (50 * 100ms)
            
            while (!isInitialized && attempts < maxAttempts) {
                Thread.sleep(100)
                attempts++
                if (attempts % 10 == 0) {
                    println("DesktopMediaPipeService: Still waiting for MediaPipe initialization... (attempt $attempts/$maxAttempts)")
                }
            }
            
            if (isInitialized) {
                println("DesktopMediaPipeService: ✅ MediaPipe initialization completed successfully")
            } else {
                println("DesktopMediaPipeService: ❌ MediaPipe initialization timed out")
                throw RuntimeException("MediaPipe initialization timed out")
            }
            
        } catch (e: Exception) {
            println("DesktopMediaPipeService: Error in MediaPipe initialization: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    private fun sendCommandToPython(command: Map<String, Any>) {
        try {
            // Create proper JSON format
            val commandJson = buildString {
                append("{")
                command.entries.forEachIndexed { index, (key, value) ->
                    if (index > 0) append(",")
                    append("\"$key\":")
                    when (value) {
                        is String -> append("\"$value\"")
                        is Boolean -> append(value.toString())
                        is Number -> append(value.toString())
                        else -> append("\"$value\"")
                    }
                }
                append("}")
            }
            
            println("DesktopMediaPipeService: Sending command to Python: $commandJson")
            
            // Check if process is still alive
            if (pythonProcess?.isAlive != true) {
                println("DesktopMediaPipeService: ERROR: Python process is not alive when sending command!")
                handleProcessDeath()
                return
            }
            
            // Create unique request ID
            val requestId = "req_${System.currentTimeMillis()}"
            
            // Write command to file
            val commandFile = File(commandDir, "$requestId.json")
            commandFile.writeText(commandJson)
            
            println("DesktopMediaPipeService: Command written to file: ${commandFile.absolutePath}")
            
            // Wait a moment for Python to process
            Thread.sleep(100)
            
            // Check if process is still alive after sending
            if (pythonProcess?.isAlive != true) {
                println("DesktopMediaPipeService: ERROR: Python process died after sending command!")
                handleProcessDeath()
            } else {
                println("DesktopMediaPipeService: Python process is still alive after sending command")
            }
            
        } catch (e: Exception) {
            println("DesktopMediaPipeService: Error sending command to Python: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun detectAsync(bufferedImage: BufferedImage, timestamp: Long) {
        if (!isInitialized || !isRunning) return
        
        try {
            // Convert BufferedImage to base64
            val base64Frame = convertBufferedImageToBase64(bufferedImage)
            
            // Send detection command to Python
            val detectCommand = mapOf(
                "type" to "detect",
                "frame_data" to base64Frame,
                "timestamp" to timestamp.toString()
            )
            
            sendCommandToPython(detectCommand)
            
        } catch (e: Exception) {
            println("DesktopMediaPipeService: Error in detectAsync: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun convertBufferedImageToBase64(bufferedImage: BufferedImage): String {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "PNG", outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.getEncoder().encodeToString(imageBytes)
    }
    
    private fun updatePostureMetrics(landmarks: List<Pair<Float, Float>>) {
        if (landmarks.size < 33) return
        
        try {
            // Calculate real posture metrics using MediaPipe landmarks
            val metrics = calculateRealMetrics(landmarks)
            updatePostureStatus(metrics)
            
        } catch (e: Exception) {
            println("DesktopMediaPipeService: Error updating posture metrics: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun calculateRealMetrics(landmarks: List<Pair<Float, Float>>): PoseMetrics {
        if (landmarks.size < 33) return PoseMetrics(0.0, 0.0, 0.0, 0.0, 0.0)
        
        // Extract key landmarks (using MediaPipe pose landmark indices)
        val nose = landmarks[0]        // Landmark 0: Nose
        val leftShoulder = landmarks[11]  // Landmark 11: Left Shoulder
        val rightShoulder = landmarks[12] // Landmark 12: Right Shoulder
        val leftEar = landmarks[3]     // Landmark 3: Left Ear
        val rightEar = landmarks[4]    // Landmark 4: Right Ear
        val leftHip = landmarks[23]    // Landmark 23: Left Hip
        val rightHip = landmarks[24]   // Landmark 24: Right Hip
        
        // Calculate torso tilt (angle from vertical)
        val torsoCenterX = (leftHip.first + rightHip.first) / 2
        val torsoCenterY = (leftHip.second + rightHip.second) / 2
        val shoulderCenterX = (leftShoulder.first + rightShoulder.first) / 2
        val shoulderCenterY = (leftShoulder.second + rightShoulder.second) / 2
        
        val torsoTilt = calculateAngleFromVertical(
            shoulderCenterX, shoulderCenterY,
            torsoCenterX, torsoCenterY
        )
        
        // Calculate shoulder tilt (left vs right shoulder height difference)
        val shoulderTilt = calculateShoulderTilt(leftShoulder, rightShoulder)
        
        // Calculate neck flexion (head forward position)
        val headZDelta = (nose.second - shoulderCenterY).toDouble()
        
        // Calculate neck flexion angle
        val neckFlex = calculateAngleFromVertical(
            nose.first, nose.second,
            shoulderCenterX, shoulderCenterY
        )
        
        // Shoulder symmetry (height difference)
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
        return kotlin.math.abs(heightDiff) * 100.0 // Scale for better visibility
    }
    
    private fun updatePostureStatus(metrics: PoseMetrics) {
        // Calculate score based on real metrics
        var newScore = 100
        
        // Deduct points for poor posture
        if (metrics.torsoTilt > 15.0) newScore -= 20
        if (metrics.shoulderTilt > 0.1) newScore -= 15
        if (metrics.neckFlex > 20.0) newScore -= 25
        if (metrics.shoulderAsymY > 0.05) newScore -= 10
        
        // Ensure score is within bounds
        score = newScore.coerceIn(0, 100)
        _scoreFlow.value = score
        
        // Update status based on score
        postureStatus = when {
            score >= 80 -> "EXCELLENT"
            score >= 60 -> "GOOD"
            score >= 40 -> "FAIR"
            score >= 20 -> "POOR"
            else -> "VERY POOR"
        }
        _postureStatusFlow.value = postureStatus
        
        // Update pose data
        poseData = "Score: $score, Status: $postureStatus, Torso Tilt: ${String.format("%.1f", metrics.torsoTilt)}°"
    }
    
    fun startTracking() {
        isRunning = true
        println("DesktopMediaPipeService: Started real MediaPipe pose tracking")
    }
    
    fun stopTracking() {
        isRunning = false
        println("DesktopMediaPipeService: Stopped tracking")
    }
    
    fun close() {
        try {
            isRunning = false
            
            // Check if Python process is still alive before sending close command
            if (pythonProcess?.isAlive == true) {
                // Send close command to Python
                val closeCommand = mapOf("type" to "close")
                sendCommandToPython(closeCommand)
                
                // Wait a bit for Python to close gracefully
                Thread.sleep(1000)
                
                // Force close if still running
                pythonProcess?.destroyForcibly()
            } else {
                println("DesktopMediaPipeService: Python process is already dead, skipping close command")
            }
            
            // Clean up resources
            pythonProcess = null
            
            // Cancel response watcher job
            responseWatcherJob?.cancel()
            responseWatcherJob = null
            
            // Cancel heartbeat job
            heartbeatJob?.cancel()
            heartbeatJob = null
            
            isInitialized = false
            serviceScope.cancel()
            
            // Clean up communication directories
            cleanupCommunicationDirs()
            
            println("DesktopMediaPipeService: Closed successfully")
        } catch (e: Exception) {
            println("DesktopMediaPipeService: Error closing: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun cleanupCommunicationDirs() {
        try {
            // Clean up command files
            commandDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".json")) {
                    file.delete()
                }
            }
            
            // Clean up response files
            responseDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".json")) {
                    file.delete()
                }
            }
            
            // Remove PID file if it exists
            if (pidFile.exists()) {
                pidFile.delete()
            }
            
            println("DesktopMediaPipeService: Communication directories cleaned up")
        } catch (e: Exception) {
            println("DesktopMediaPipeService: Error cleaning up communication directories: ${e.message}")
        }
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    fun checkServiceStatus() {
        if (pythonProcess?.isAlive == true) {
            val statusCommand = mapOf("type" to "status")
            sendCommandToPython(statusCommand)
        } else {
            println("DesktopMediaPipeService: Cannot check status - Python process is not alive")
        }
    }
    
    fun restartService() {
        println("DesktopMediaPipeService: Restarting service...")
        try {
            close()
            Thread.sleep(1000) // Wait for cleanup
            setupPoseLandmarker()
            println("DesktopMediaPipeService: Service restarted successfully")
        } catch (e: Exception) {
            println("DesktopMediaPipeService: Failed to restart service: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // Getter methods for pose data
    fun getPoseData(): String = poseData
    fun getLandmarks(): List<Pair<Float, Float>> = landmarks
    fun getScore(): Int = score
    fun getPostureStatus(): String = postureStatus
}
