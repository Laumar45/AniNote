package com.laumar.aninote.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportExportUtilsTest {

    @Test
    fun `formatLine formats single view without comma suffix`() {
        val line = formatLine(1, "Naruto", 1)
        assertEquals("1. Naruto", line)
    }

    @Test
    fun `formatLine formats multiple views with comma suffix`() {
        val line = formatLine(1, "Naruto", 3)
        assertEquals("1. Naruto, 3", line)
    }

    @Test
    fun `formatTxtExport formats full list correctly`() {
        val list = listOf(
            "One Punch Man" to 1,
            "Konosuba" to 2
        )
        val exported = formatTxtExport(list)
        val expected = "1. One Punch Man\n2. Konosuba, 2"
        assertEquals(expected, exported)
    }

    @Test
    fun `parseTxtFile strips position prefix and extracts comma view count`() {
        val content = """
            1. Naruto, 3
            2. Bleach
            3. One Piece, 5
        """.trimIndent()

        val result = parseTxtFile(content)
        assertEquals(3, result.animes.size)
        assertEquals(0, result.ignoredCount)
        assertEquals("Naruto" to 3, result.animes[0])
        assertEquals("Bleach" to 1, result.animes[1])
        assertEquals("One Piece" to 5, result.animes[2])
    }

    @Test
    fun `parseTxtFile handles titles containing inner commas`() {
        val content = "1. Kaguya-sama: Love Is War?, 2"
        val result = parseTxtFile(content)

        assertEquals(1, result.animes.size)
        assertEquals("Kaguya-sama: Love Is War?" to 2, result.animes[0])
    }

    @Test
    fun `parseTxtFile ignores empty lines and invalid entries`() {
        val content = """
            
            1. Naruto
               
            , 4
            2. Attack on Titan, 2
        """.trimIndent()

        val result = parseTxtFile(content)
        assertEquals(2, result.animes.size)
        assertEquals(3, result.ignoredCount)
        assertEquals("Naruto" to 1, result.animes[0])
        assertEquals("Attack on Titan" to 2, result.animes[1])
    }
}
