package de.tysw.quotes.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import de.tysw.quotes.model.DialogueLine
import de.tysw.quotes.model.Direction
import de.tysw.quotes.model.Quotation
import de.tysw.quotes.viewmodels.MainViewModel
import de.tysw.quotes.R


@Composable
fun MainScreen(viewModel: MainViewModel, onSettingsClick: () -> Unit) {
    val quotations by viewModel.filteredQuotations.collectAsState()
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadQuotations()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        SearchBar(
                            inputField = {
                                SearchBarDefaults.InputField(
                                    query = searchQuery,
                                    onQueryChange = {
                                        searchQuery = it
                                        viewModel.filterQuotations(searchQuery)
                                    },
                                    expanded = isSearching,
                                    onExpandedChange = {},
                                    onSearch = { isSearching = false },
                                    placeholder = { Text("Search..") }
                                )
                            },
                            expanded = false,       // Never display a suggestion list
                            onExpandedChange = {}   // So no change behavior needed
                        ) {
                            // No body needed
                            // This would be the place to define the suggestions
                            // and/or search results
                        }
                    } else {
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    if (isSearching) {
                       IconButton(onClick = {
                           isSearching = false
                           searchQuery = ""
                           viewModel.resetFilter()
                       }) {
                           Icon(Icons.Default.Close, contentDescription = "Close search")
                       }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
            })
        }) { padding ->
        Box(Modifier.padding(padding)) {
            if (quotations.isEmpty()) {
                EmptyState()
            } else {
                QuotationList(
                    quotations = quotations,
                    viewModel = viewModel
                )
            }
        }
    }
}


@Composable
private fun QuotationList(
    quotations: List<Quotation>,
    viewModel: MainViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(quotations, key = {it.id}) { quotation ->
            QuotationListItem(
                quotation = quotation,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun QuotationListItem(
    quotation: Quotation,
    viewModel: MainViewModel
) {
    val query by viewModel.searchQuery.collectAsState()
    val toggles by viewModel.englishToggles.collectAsState()

    val showEnglish = toggles[quotation.id] ?: false
    val hasEnglish = quotation.hasEnglish
    val isDialogue = quotation.isDialogue


    val rawText = if (showEnglish && hasEnglish) quotation.textEn.orEmpty()
                  else quotation.textDe.orEmpty()

    val speaker = quotation.speaker

    val dialogueLines = if (showEnglish && hasEnglish) quotation.dialogueEn
                        else quotation.dialogueDe

    val backgroundColor = if (hasEnglish) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.secondary

    val foregroundColor = if (hasEnglish) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = hasEnglish) {
                viewModel.toggleEnglish(quotation.id)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            if(isDialogue) {
                DialogueView(
                    lines = dialogueLines,
                    speakerColor = foregroundColor,
                    viewModel
                ) } else {
                Text(
                    text = buildAnnotatedString {
                        // 1) rawText (including highlighting)
                        appendHighlighted(
                            text = rawText,
                            query = query,
                            normalColor = foregroundColor,
                            highlightColor = Color.Yellow.copy(alpha = 0.4f)
                        )

                        // 2) attach speaker (regardless of highlighting)
                        speaker?.let {
                            append(" — ")
                            appendHighlighted(
                                text = it,
                                query = query,
                                normalColor = foregroundColor,
                                highlightColor = Color.Yellow.copy(alpha = 0.4f)
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = foregroundColor
                )
            }

            Spacer(Modifier.height(8.dp))

            Row {
                Text(
                    text = highlightMatches(
                        text = "${quotation.medium} — ${quotation.source}",
                        query = query,
                        normalColor = foregroundColor,
                        highlightColor = Color.Yellow.copy(alpha = 0.4f)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor
                )

                quotation.position?.let {
                    Text(
                        text = " @ $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor
                    )
                }
            }

            Text(
                text = quotation.date,
                style = MaterialTheme.typography.bodySmall,
                color = foregroundColor
            )
        }
    }
}

@Composable
fun DialogueView(
    lines: List<DialogueLine>,
    speakerColor: Color,
    viewModel: MainViewModel
) {
    val query by viewModel.searchQuery.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (line.direction == Direction.LEFT) Arrangement.Start
                                        else Arrangement.End
            ) {
                line.speaker?.let {
                    Text(
                        text = buildAnnotatedString {
                            appendHighlighted(
                                text = it,
                                query = query,
                                normalColor = speakerColor,
                                highlightColor = Color.Yellow.copy(alpha = 0.4f)
                            )
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = speakerColor,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                val backgroundColor = if (line.direction == Direction.LEFT) MaterialTheme.colorScheme.surface
                                      else MaterialTheme.colorScheme.surfaceVariant
                val foregroundColor = if (line.direction == Direction.LEFT) MaterialTheme.colorScheme.onSurface
                                      else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(
                    color = backgroundColor,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = highlightMatches(
                            line.text,
                            query,
                            normalColor = foregroundColor,
                            highlightColor = Color.Yellow.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = foregroundColor
                    )
                }
            }
        }
    }
}

@Composable
fun highlightMatches(
    text: String,
    query: String,
    normalColor: Color,
    highlightColor: Color
): AnnotatedString {
    if (query.isBlank()) {
        return AnnotatedString(
            text,
            spanStyle = SpanStyle(color = normalColor)
        )
    }

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()

    return buildAnnotatedString {
        var start = 0
        while (true) {
            val index = lowerText.indexOf(lowerQuery, start)
            if (index < 0) {
                withStyle(style = SpanStyle(color = normalColor)) {
                    append(text.substring(start))
                }
                break
            }

            // normal part
            withStyle(style = SpanStyle(color = normalColor)) {
                append(text.substring(start, index))
            }

            // highlighted match
            withStyle(
                style = SpanStyle(
                    color = normalColor,
                    background = highlightColor,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(text.substring(index, index + query.length))
            }

            start = index + query.length
        }
    }
}

fun AnnotatedString.Builder.appendHighlighted(
    text: String,
    query: String,
    normalColor: Color,
    highlightColor: Color
) {
    if (query.isBlank()) {
        withStyle(SpanStyle(color = normalColor)) {
            append(text)
        }
        return
    }

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()

    var start = 0

    while (true) {
        val index = lowerText.indexOf(lowerQuery, start)
        if (index < 0) {
            withStyle(SpanStyle(color = normalColor)) {
                append(text.substring(start))
            }
            break
        }

        // normal segment
        withStyle(SpanStyle(color = normalColor)) {
            append(text.substring(start, index))
        }

        // highlighted match
        withStyle(
            SpanStyle(
                color = Color.Black,
                background = highlightColor,
                fontWeight = FontWeight.SemiBold
            )
        ) {
            append(text.substring(index, index + query.length))
        }

        start = index + query.length
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Text(
            "No quotes available.\n" +
                 "Please configure the URL in the settings screen."
        )
    }
}

