package com.ZKQWatcher.android
import android.os.Build
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ZKQWatcher.android.ui.theme.Theme
import com.ZKQWatcher.android.ui.AccountScreen
import com.ZKQWatcher.android.data.SettingsRepository
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Theme { AccountScreen(SettingsRepository(applicationContext)) }
        }
    }
}
