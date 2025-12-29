import de.tysw.quotes.model.Direction
import de.tysw.quotes.util.QuotationParser
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the quotation parser
 *
 * For more info about testing, see:
 * [testing documentation](http://d.android.com/tools/testing).
 */
class QuotationParserTest {

    private val sampleData = """
        [2022-01-29] [Film] Die Waffen der Frauen @ 01:03:18
        Tess McGill: "Ich will nicht den Rest meines Lebens damit verbringen, mich abzurackern, und nichts zu erreichen, nur weil ich Regeln befolgt habe, die ich nicht aufgestellt habe."

        [2024-03-25] [Serie] Parks And Recreation [S5E19] @ 3:08
        Ron Swanson: [de]: "Wenn ich die Wahl habe, etwas zu tun oder nichts zu tun, wähle ich gewöhnlich das Nichts-Tun. ..."
        Ron Swanson: [en]: "Normally, if given a choice between doing something or nothing, I choose nothing. ..."
        
        [2025-11-05] [Serie] The Deuce [S02E09] @ 50:50
        [de] >> "Eileen: Das ist doch nur Geld. ..." << "Assistent: Du warst gestern gut im Fernsehen." >> "Eileen: Nein, ..."
        [en] >> "Eileen: This is just money. ..." << "Assistant: Hey, you were good on TV last night." >> "Eileen: No, ..."

        [2024-12-03] [Serie] House Of The Dragon [S02E07] @ 27:05
        Alyn of Hull [de]: "Mein Bruder war schon immer rastlos. Sehnte sich nach einem Beweis für seinen Wert. Ich bin von Salz und See. Ich sehn mich nach nichts sonst."
        Alyn of Hull [en]: "My brother was ever restless, yearning for some sign of his worth. I am of salt and see. I yearn for nothing else."
    """.trimIndent()

    @Test
    fun `parser should extract quotations and dialogues`() {
        val quotations = QuotationParser.parse(sampleData)

        // Basic check: Number of blocks
        assertEquals(4, quotations.size)

        // Example: first block
        val first = quotations[0]
        assertEquals("2022-01-29", first.date)
        assertEquals("Film", first.medium)
        assertEquals("Die Waffen der Frauen", first.source)
        assertEquals("01:03:18", first.position)
        assertEquals("Tess McGill", first.speaker)
        assertTrue(first.textDe!!.contains("Rest meines Lebens"))

        // Example: third block with dialog
        val third = quotations[2]
        assertEquals("The Deuce [S02E09]", third.source)
        assertTrue(third.dialogueDe.isNotEmpty())
        assertTrue(third.dialogueEn.isNotEmpty())

        // Check speakers in dialogue
        val firstLineDe = third.dialogueDe.first()
        assertEquals("Eileen", firstLineDe.speaker)
        assertEquals(Direction.LEFT, firstLineDe.direction)

        // Example: fourth block
        val fourth = quotations[3]
        assertEquals("House Of The Dragon [S02E07]", fourth.source)
        assertTrue(fourth.dialogueDe.isEmpty())
        assertTrue(fourth.dialogueEn.isEmpty())
        assertNotNull(fourth.dialogueDe)
        assertNotNull(fourth.dialogueEn)
        assertEquals(
            "Mein Bruder war schon immer rastlos. Sehnte sich nach einem Beweis für seinen Wert. Ich bin von Salz und See. Ich sehn mich nach nichts sonst.",
            fourth.textDe
        )
    }
}
