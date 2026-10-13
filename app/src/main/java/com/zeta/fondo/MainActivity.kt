package com.zeta.fondo

import android.content.Intent
import android.graphics.Color as AColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import com.zeta.fondo.ui.Dusk1
import com.zeta.fondo.ui.EditorScreen
import com.zeta.fondo.ui.FondoTheme
import com.zeta.fondo.ui.HomeScreen
import com.zeta.fondo.ui.Ink
import com.zeta.fondo.ui.Night
import com.zeta.fondo.ui.Peach
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            FondoTheme {
                AppRoot(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        vm.saveProject(forceCurrent = true)
    }

    private fun handleIntent(i: Intent?) {
        if (i == null || i.action != Intent.ACTION_SEND) return
        val uri = IntentCompat.getParcelableExtra(i, Intent.EXTRA_STREAM, Uri::class.java) ?: return
        vm.importImage(uri)
    }
}

@Composable
private fun AppRoot(vm: AppViewModel) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Night)
            .then(if (vm.fullscreen && vm.screen == Screen.EDITOR) Modifier else Modifier.systemBarsPadding())
    ) {
        when (vm.screen) {
            Screen.HOME -> HomeScreen(vm)
            Screen.EDITOR -> EditorScreen(vm)
        }

        vm.message?.let { msg ->
            LaunchedEffect(msg) {
                delay(4500)
                if (vm.message == msg) vm.message = null
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Dusk1)
                    .clickable { vm.message = null }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(msg, color = Ink, fontSize = 14.sp)
            }
        }

        vm.busy?.let { text ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Night.copy(alpha = 0.8f))
                    .clickable(enabled = true, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator(color = Peach)
                    Text(text, color = Ink, fontSize = 15.sp)
                }
            }
        }
    }
}
