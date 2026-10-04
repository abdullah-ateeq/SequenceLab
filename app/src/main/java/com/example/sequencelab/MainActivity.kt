package com.example.sequencelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sequencelab.ui.SequenceLabApp
import com.example.sequencelab.ui.SequenceLabViewModel
import com.example.sequencelab.ui.screens.SplashScreen
import com.example.sequencelab.ui.theme.SequenceLabTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SequenceLabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            var showSplash by rememberSaveable { mutableStateOf(true) }
            val loaded = settings ?: return@setContent
            SequenceLabTheme(themeMode = loaded.theme, dynamicColor = loaded.dynamicColor) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Crossfade(targetState = showSplash, label = "splash") { splash ->
                        if (splash) {
                            SplashScreen(onFinished = { showSplash = false })
                        } else {
                            SequenceLabApp(viewModel, loaded)
                        }
                    }
                }
            }
        }
    }
}
