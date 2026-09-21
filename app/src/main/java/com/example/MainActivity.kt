package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LishFileTheme
import com.example.viewmodel.FileManagerViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: FileManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.preferences.themeMode.collectAsState()

            LishFileTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScreen(
                        viewModel = viewModel,
                        themeMode = themeMode,
                        onThemeToggle = {
                            val nextMode = if (themeMode == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK
                            viewModel.setThemeMode(nextMode)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopP2pServer()
    }
}
