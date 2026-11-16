package com.vidlynk.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.vidlynk.app.ui.navigation.VidLynkNavGraph
import com.vidlynk.app.ui.theme.VidLynkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VidLynkTheme {
                val nav = rememberNavController()
                navController = nav
                VidLynkNavGraph(
                    navController = nav,
                    exoPlayer = exoPlayer
                )
            }
        }
    }

    // ComponentActivity (Activity 1.9+) signature
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController?.handleDeepLink(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            exoPlayer.release()
        }
    }
}
