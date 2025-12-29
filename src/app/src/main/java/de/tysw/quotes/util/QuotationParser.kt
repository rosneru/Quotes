package de.tysw.quotes.util

import de.tysw.quotes.model.DialogueLine
import de.tysw.quotes.model.Direction
import de.tysw.quotes.model.Quotation

object QuotationParser {

    private val headerRegex = Regex("""\[(\d{4}-\d{2}-\d{2})] \[(.+?)] (.+?)(?: @ (.+))?$""")
//    private val speakerRegex = Regex("""^(.+?):\s*(.*)$""")

    // Speaker with optional voice marker
    private val speakerRegex = Regex("""^(.+?)(?:\s*\[(de|en)])?:\s*(.*)$""")

    fun parse(content: String): List<Quotation> {
        return content
            .trim()
            .split(Regex("\n\\s*\n"))
            .mapNotNull { block ->
                val lines = block.lines().filter { it.isNotBlank() }
                if (lines.isEmpty()) return@mapNotNull null

                val headerMatch = headerRegex.find(lines.first()) ?: return@mapNotNull null
                val date = headerMatch.groupValues[1]
                val medium = headerMatch.groupValues[2]
                val source = headerMatch.groupValues[3]
                val position = headerMatch.groupValues.getOrNull(4)?.takeIf { it.isNotBlank() }

                var speaker: String? = null
                var textDe: String? = null
                var textEn: String? = null
                var dialogueDe: List<DialogueLine> = emptyList()
                var dialogueEn: List<DialogueLine> = emptyList()

                lines.drop(1).forEach { line ->
                    when {
                        // Dialog block
                        line.contains(">>") || line.contains("<<") -> {
                            if (line.contains("[de]")) {
                                val raw = line.substringAfter("[de]").trim()
                                textDe = raw
                                dialogueDe = parseDialogue(raw)
                            } else if (line.contains("[en]")) {
                                val raw = line.substringAfter("[en]").trim()
                                textEn = raw
                                dialogueEn = parseDialogue(raw)
                            }
                        }
                        // Speaker + text
                        speakerRegex.matches(line) -> {
                            val match = speakerRegex.find(line)!!
                            speaker = match.groupValues[1].trim()
                            val lang = match.groupValues[2] // "de" or "en" or empty
                            val rest = match.groupValues[3].trim()
                            when (lang) {
                                "de" -> textDe = extractQuotedText(rest)
                                "en" -> textEn = extractQuotedText(rest)
                                else -> textDe = extractQuotedText(rest) ?: rest
                            }
                        }
                        // Language markers only
                        line.contains("[de]") -> textDe = extractQuotedText(line)
                        line.contains("[en]") -> textEn = extractQuotedText(line)
                        else -> textDe = line.trim()
                    }
                }

                Quotation(
                    date = date,
                    medium = medium,
                    source = source,
                    position = position,
                    speaker = speaker,
                    textDe = textDe,
                    textEn = textEn,
                    dialogueDe = dialogueDe,
                    dialogueEn = dialogueEn
                )
            }
    }

    private fun extractQuotedText(line: String): String? {
        return Regex("“(.+?)”|\"(.+?)\"")
            .find(line)
            ?.groupValues
            ?.drop(1)
            ?.firstOrNull { it.isNotEmpty() }
    }

    private fun parseDialogue(text: String): List<DialogueLine> {
        val regex = Regex("""(>>|<<)\s*(?:"([^"]+)"|(.+))""")
        return regex.findAll(text).map { match ->
            val direction = when (match.groupValues[1]) {
                ">>" -> Direction.LEFT
                "<<" -> Direction.RIGHT
                else -> Direction.LEFT
            }
            val fullContent = match.groupValues[2].ifBlank { match.groupValues[3] }.trim()
            val (speaker, actualText) = if (':' in fullContent) {
                val parts = fullContent.split(':', limit = 2)
                parts[0].trim().takeIf { it.isNotEmpty() } to parts[1].trim()
            } else {
                null to fullContent
            }
            DialogueLine(text = actualText, direction = direction, speaker = speaker)
        }.toList()
    }
}
