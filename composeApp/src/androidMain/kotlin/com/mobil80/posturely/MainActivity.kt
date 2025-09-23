package com.mobil80.posturely

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import android.view.WindowManager

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handled silently
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        
        // Request camera permission
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        
        // Set up Android posture tracking with context and lifecycle
        try {
            setupAndroidPostureTracking(this, this)
        } catch (e: Exception) {
            // Handle error silently
        }
        
        // Set app context for scan bridge
        setAppContext(this.applicationContext)

        setContent {
            App()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup will be handled by PostureTrackingManager
    }

    override fun onResume() {
        super.onResume()
        // Prevent screen from sleeping while app is in foreground
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        // Allow normal screen behavior when app is not in foreground
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}