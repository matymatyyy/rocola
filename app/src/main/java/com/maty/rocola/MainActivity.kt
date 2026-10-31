package com.maty.rocola

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.maty.rocola.ui.LibraryViewModel
import com.maty.rocola.ui.NavViewModel
import com.maty.rocola.ui.PlayerViewModel
import com.maty.rocola.ui.RocolaRoot
import com.maty.rocola.ui.theme.RocolaTheme

class MainActivity : ComponentActivity() {

    private val libraryVm: LibraryViewModel by viewModels()
    private val playerVm: PlayerViewModel by viewModels()
    private val navVm: NavViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            RocolaTheme {
                RocolaRoot(libraryVm, playerVm, navVm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_PLAYER -> navVm.playerExpanded = true
            ACTION_SHUFFLE_ALL -> {
                navVm.playerExpanded = true
                playerVm.shuffleAllWhenReady()
            }
        }
    }

    companion object {
        const val ACTION_OPEN_PLAYER = "com.maty.rocola.action.OPEN_PLAYER"
        const val ACTION_SHUFFLE_ALL = "com.maty.rocola.action.SHUFFLE_ALL"
    }
}
