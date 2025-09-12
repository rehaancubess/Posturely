import java.io.File

fun main() {
    println("=== Testing Binary Detection Logic ===")
    
    // Test the same logic that's in DesktopMediaPipeService
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
    
    println("OS: $osName")
    println("Possible paths: $possiblePaths")
    println("Current working directory: ${System.getProperty("user.dir")}")
    
    // Check relative to current working directory
    for (path in possiblePaths) {
        val file = File(path)
        println("Checking: ${file.absolutePath} (exists: ${file.exists()}, executable: ${file.canExecute()})")
        if (file.exists() && file.canExecute()) {
            println("✅ FOUND BUNDLED BINARY: ${file.absolutePath}")
            return
        }
    }
    
    // Check relative to resources directory
    val resourcesDir = File(".")
    println("\nChecking resources directory: ${resourcesDir.absolutePath}")
    
    for (path in possiblePaths) {
        val file = File(resourcesDir, path)
        println("Checking: ${file.absolutePath} (exists: ${file.exists()}, executable: ${file.canExecute()})")
        if (file.exists() && file.canExecute()) {
            println("✅ FOUND BUNDLED BINARY: ${file.absolutePath}")
            return
        }
    }
    
    println("❌ No bundled binary found")
}
