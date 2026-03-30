package com.binbashmedium.sightreadingtrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.binbashmedium.sightreadingtrainer.ui.MainScreen
import com.binbashmedium.sightreadingtrainer.ui.PracticeScreen
import com.binbashmedium.sightreadingtrainer.ui.SettingsScreen
import com.binbashmedium.sightreadingtrainer.ui.StatisticsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") { MainScreen(navController) }
                        composable("practice") { PracticeScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                        composable("statistics") { StatisticsScreen(navController) }
                    }
                }
            }
        }
    }
}
