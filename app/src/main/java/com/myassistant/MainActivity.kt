package com.myassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private val vm: VM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { Root(vm) } }
    }
}

@Composable
fun Root(vm: VM) {
    val ctx = LocalContext.current
    val voice = remember { Voice(ctx.applicationContext).also { it.init() } }
    DisposableEffect(Unit) { onDispose { voice.shutdown() } }
    var settings by remember { mutableStateOf(false) }
    var inVoice by remember { mutableStateOf(false) }
    BackHandler(settings || inVoice) { if (inVoice) inVoice = false else settings = false }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
        Box(Modifier.fillMaxSize()) {
            Home(vm, voice, openSettings = { settings = true }, openVoice = { inVoice = true })
            AnimatedVisibility(settings, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
                SettingsScreen(vm) { settings = false }
            }
            AnimatedVisibility(inVoice, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                VoiceScreen(vm, voice) { inVoice = false }
            }
        }
    }
}
