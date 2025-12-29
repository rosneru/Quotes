package de.tysw.quotes.model

enum class Direction { LEFT, RIGHT }

data class DialogueLine (
    val text: String,
    val direction: Direction,
    val speaker: String? = null
)
