package com.suldev.pddkz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.suldev.pddkz.data.network.NbkRatesApi
import com.suldev.pddkz.data.repository.RatesRepository
import com.suldev.pddkz.data.storage.BackgroundImageStorage
import com.suldev.pddkz.ui.viewmodel.PddCalculatorViewModel
import androidx.compose.runtime.LaunchedEffect
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
        val backgroundStorage = BackgroundImageStorage(applicationContext, storage)
        val calculatorViewModel: PddCalculatorViewModel by viewModels {
            PddCalculatorViewModel.Factory(RatesRepository(NbkRatesApi(), storage))
        }
        setContent {
            LaunchedEffect(backgroundStorage) {
                backgroundStorage.validateOrFallback()
            }
            // null до первого чтения DataStore: не мигаем дефолтной темой поверх сохранённой.
            val settings by storage.settings.collectAsState(initial = null)
            val backgroundFileName by storage.backgroundFileName.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            settings?.let { loaded: AppSettings ->
                PddAppRoot(
                    settings = loaded,
                    backgroundFileName = backgroundFileName,
                    backgroundImageStorage = backgroundStorage,
                    onUpdateSettings = { transform -> scope.launch { storage.updateSettings(transform) } },
                    viewModel = calculatorViewModel,
                )
            }
        }
    }
}
