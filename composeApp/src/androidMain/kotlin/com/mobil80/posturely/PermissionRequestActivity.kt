package com.mobil80.posturely

import android.app.Activity
import android.os.Bundle
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class PermissionRequestActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PERMISSION = "extra_permission"
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permission = intent.getStringExtra(EXTRA_PERMISSION) ?: Manifest.permission.CAMERA
        launcher.launch(permission)
    }
}


