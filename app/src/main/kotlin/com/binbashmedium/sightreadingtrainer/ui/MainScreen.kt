package com.binbashmedium.sightreadingtrainer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.binbashmedium.sightreadingtrainer.R

@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(32.dp))

        Text("Length: ${settings.exerciseLength}")
        Text("Types: ${settings.exerciseTypes.sortedBy { it.ordinal }.joinToString(", ") { it.name.replace('_', ' ') }}")
        Text("Keys: ${settings.selectedKeys.sorted().joinToString(", ") { KEY_NAMES[it] }}")
        Text("Hand: ${settings.handMode.name}")

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { navController.navigate("practice") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.start_practice))
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { navController.navigate("settings") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings))
        }
    }
}
