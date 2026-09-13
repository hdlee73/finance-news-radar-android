package io.github.hdlee73.financenewsradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.hdlee73.financenewsradar.ui.FinanceNewsRadarApp
import io.github.hdlee73.financenewsradar.ui.theme.FinanceNewsRadarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceNewsRadarTheme { FinanceNewsRadarApp() }
        }
    }
}
