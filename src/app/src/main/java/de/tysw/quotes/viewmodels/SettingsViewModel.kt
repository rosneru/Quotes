package de.tysw.quotes.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tysw.quotes.data.SettingsPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Locale
import java.util.Date

sealed class DownloadState {
    data object Idle : DownloadState()
    data object Loading : DownloadState()
    data object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _fileDate = MutableStateFlow<String>("")
    val fileDate: StateFlow<String> = _fileDate

    private val _fileAge = MutableStateFlow<String>("none")
    val fileAge: StateFlow<String> = _fileAge

    private val appContext = application.applicationContext

    init {
        viewModelScope.launch {
            SettingsPrefs.readUrl(getApplication())
                .collect { savedUrl ->
                    _url.value = savedUrl
                }
        }
    }

    fun onUrlChanged(newUrl: String) {
        _url.value = newUrl
    }

    fun retrieveFileDate() {
        viewModelScope.launch {
            val file = File(appContext.filesDir, "quotations.txt")
            if (file.exists()) {
                val lastModifiedMillis = file.lastModified()
                val lastModifiedDate = Date(lastModifiedMillis)
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                _fileDate.value = formatter.format(lastModifiedDate)

                val now = Date()
                val diffMillies = now.time - lastModifiedDate.time
                val fileAge = TimeUnit.MILLISECONDS.toDays(diffMillies).toInt()

                val ageStr = when(fileAge) {
                    0 -> "today"
                    1 -> "1 day ago"
                    else -> "$fileAge days ago"
                }

                _fileAge.value = "($ageStr)"
            }
            else
            {
                _fileDate.value = "No file downloaded yet."
            }
        }
    }

    fun downloadFile() {
        val currentUrl = _url.value

        if (currentUrl.isBlank()) {
            _downloadState.value = DownloadState.Error("URL must not be empty!")
        }

        _downloadState.value = DownloadState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL(currentUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP ${connection.responseCode}")
                }

                val input = BufferedInputStream(connection.inputStream)
                val file = getApplication<Application>().openFileOutput("quotations.txt", 0)
                val output = FileOutputStream(file.fd)

                input.copyTo(output)

                output.flush()
                output.close()
                input.close()

                SettingsPrefs.saveUrl(getApplication(), currentUrl)
                _downloadState.value = DownloadState.Success
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error(e.message ?: "Unknown Download error")
            }
        }
    }
}
