package de.tysw.quotes.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tysw.quotes.model.Quotation
import de.tysw.quotes.util.QuotationParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File



class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var _originalQuotations: List<Quotation> = emptyList()

    private val _filteredQuotations = MutableStateFlow<List<Quotation>>(emptyList())
    val filteredQuotations: StateFlow<List<Quotation>> = _filteredQuotations

    private val appContext = application.applicationContext

    private val _englishToggles = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val englishToggles: StateFlow<Map<String, Boolean>> = _englishToggles

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun loadQuotations() {
        viewModelScope.launch {
            val file = File(appContext.filesDir, "quotations.txt")
            if (file.exists()) {
                val text = file.readText()
                _originalQuotations = QuotationParser.parse(text).sortedByDescending { it.date }

            }
            else
            {
                _originalQuotations = emptyList()
            }

            _filteredQuotations.value = _originalQuotations
        }
    }

    fun filterQuotations(query: String) {
        _searchQuery.value = query
        val q = query.trim().lowercase()

        if (q.isEmpty())
        {
            _filteredQuotations.value = _originalQuotations
            return
        }

        _filteredQuotations.value = _originalQuotations.filter { it.matchesQuery(q) }
    }

    fun resetFilter() {
        _searchQuery.value = ""
        _filteredQuotations.value = _originalQuotations
    }

    fun toggleEnglish(id: String) {
        _englishToggles.update { current ->
            val newValue = !(current[id] ?: false)
            current + (id to newValue)
        }
    }

    fun isEnglishShown(id: String): Boolean {
        return _englishToggles.value[id] ?: false
    }

    private fun Quotation.matchesQuery(q: String): Boolean {
        return listOfNotNull(
            source,
            speaker,
            textDe,
            textEn,
            position,
            date,
            medium
        ).any { it.contains(q, ignoreCase = true) }
                ||
                dialogueDe.any { it.text.contains(q, ignoreCase = true) }
                ||
                dialogueEn.any { it.text.contains(q, ignoreCase = true) }
    }
}
