package com.hkaboodvand.finacemobile

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hkaboodvand.finacemobile.ui.theme.FinacemobileTheme
import com.hkaboodvand.finacemobile.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {

    // درخواست مجوز پیامک
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // نتیجه‌ی مجوزها
        permissions.forEach { (permission, granted) ->
            android.util.Log.d("MainActivity", "$permission: $granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // فعال‌سازی RTL
        window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL

        // درخواست مجوز پیامک
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS
            )
        )

        setContent {
            FinacemobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}