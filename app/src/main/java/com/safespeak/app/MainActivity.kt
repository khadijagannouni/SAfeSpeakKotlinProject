package com.safespeak.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.safespeak.app.ui.navigation.SafeSpeakNavHost
import com.safespeak.app.ui.theme.SafeSpeakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SafeSpeakTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SafeSpeakNavHost()
                }
            }
        }
    }
}
