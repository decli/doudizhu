package com.decli.doudizhu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.decli.doudizhu.ui.DouDiZhuApp
import com.decli.doudizhu.ui.theme.DouDiZhuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            DouDiZhuTheme {
                DouDiZhuApp()
            }
        }
    }
}

