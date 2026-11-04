package com.vidlynk.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.compose.rememberNavController
import com.vidlynk.app.ui.navigation.VidLynkNavGraph
import com.vidlynk.app.ui.theme.VidLynkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VidLynkTheme {
                val navController = rememberNavController()
                VidLynkNavGraph(
                    navController = navController,
                    exoPlayer = exoPlayer
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Passes new deep link intent when app is already in the backstack
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            exoPlayer.release()
        }
    }
}
