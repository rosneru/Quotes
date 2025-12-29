package de.tysw.quotes.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tysw.quotes.viewmodels.DownloadState
import de.tysw.quotes.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val url by viewModel.url.collectAsState()
    val fileDate by viewModel.fileDate.collectAsState()
    val fileAge by viewModel.fileAge.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Load the date and age odf the quotations file
    LaunchedEffect(downloadState) {
        viewModel.retrieveFileDate()
    }

    // Display the SnackBar on status change
    LaunchedEffect(downloadState) {
        when (downloadState) {
            is DownloadState.Success ->
                snackbarHostState.showSnackbar("File successfully downloaded.")
            is DownloadState.Error ->
                snackbarHostState.showSnackbar(
                    "Download error: ${(downloadState as DownloadState.Error)
                        .message}"
                )
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                             contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = viewModel::onUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Quotes file download URL") },
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Row {
                Text(text = "File was updated on $fileDate $fileAge.", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = viewModel::downloadFile,
                enabled = downloadState != DownloadState.Loading
            ) {
                if (downloadState == DownloadState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Download")
                }
            }
        }
    }
}
