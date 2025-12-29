package de.tysw.quotes.model

import java.util.UUID

data class Quotation(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val medium: String,         // movie, series, book, ...
    val source: String,         // Title, i.a. with season/episode/page
    val position: String?,      // @ 01:03:18 or @ p.213
    val speaker: String?,       // for simple quotations
    val textDe: String?,        // German quotation text
    val textEn: String?,        // English quotation text
    val dialogueDe: List<DialogueLine> = emptyList(),
    val dialogueEn: List<DialogueLine> = emptyList()
) {
    val hasEnglish: Boolean
        get() = !textEn.isNullOrBlank() || dialogueEn.isNotEmpty()

    val isDialogue: Boolean
        get() = dialogueDe.isNotEmpty()

}
