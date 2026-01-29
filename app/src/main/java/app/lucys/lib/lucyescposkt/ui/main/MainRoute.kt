package app.lucys.lib.lucyescposkt.ui.main

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lucys.lib.lucyescposkt.core.escpos.EPStreamData
import app.lucys.lib.lucyescposkt.core.printer.PrinterModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainRoute(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val requiredPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(android.Manifest.permission.BLUETOOTH_ADMIN).let { permissions ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions + listOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                )
            } else {
                permissions + listOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    )

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var isLogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = requiredPermissionState) {
        if (!requiredPermissionState.allPermissionsGranted) {
            requiredPermissionState.launchMultiplePermissionRequest()
        }
    }

    val devices by viewModel.devices.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Printers") },
                actions = {
                    TextButton(onClick = viewModel::refresh) {
                        Text("Refresh")
                    }
                    TextButton(onClick = { isLogVisible = !isLogVisible }) {
                        Text("Log")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(all = 12.dp),
        ) {
            if (isLogVisible) {
                items(messages) { message ->
                    LogContent(
                        modifier = Modifier.fillMaxWidth(),
                        message = message,
                    )
                }
            } else {
                items(devices) { device ->
                    PrinterCard(
                        modifier = Modifier.fillMaxWidth(),
                        onTap = { viewModel.stream(device) },
                        model = device,
                    )
                }
            }
        }
    }
}

@Composable
fun LogContent(
    modifier: Modifier = Modifier,
    message: EPStreamData,
) {
    val text = remember {
        when (message) {
            is EPStreamData.Log -> message.value
            is EPStreamData.Result -> message.value.toString()
        }
    }

    Text(
        modifier = modifier,
        text = text,
    )
}

@Composable
fun PrinterCard(
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    model: PrinterModel,
) {
    Card(
        modifier = modifier,
        onClick = onTap,
    ) {
        ListItem(
            modifier = Modifier
                .padding(all = 8.dp)
                .fillMaxWidth(),
            headlineContent = { Text(model.name) },
        )
    }
}
