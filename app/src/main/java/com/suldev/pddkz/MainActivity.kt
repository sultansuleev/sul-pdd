package com.suldev.pddkz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.suldev.pddkz.data.storage.PreferencesStorage
import com.suldev.pddkz.data.storage.pddDataStore
import com.suldev.pddkz.domain.model.AppSettings
import com.suldev.pddkz.ui.PddAppRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val storage = PreferencesStorage(applicationContext.pddDataStore)
        setContent {
            // null до первого чтения DataStore: не мигаем дефолтной темой поверх сохранённой.
            val settings by storage.settings.collectAsState(initial = null)
            val backgroundFileName by storage.backgroundFileName.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            settings?.let { loaded: AppSettings ->
                PddAppRoot(
                    settings = loaded,
                    backgroundFileName = backgroundFileName,
                    onUpdateSettings = { transform -> scope.launch { storage.updateSettings(transform) } },
                )
            }
        }
    }
}
