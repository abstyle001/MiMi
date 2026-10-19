package org.example.project.pet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PetStateTest {

    @Test
    fun sanitizePetNameTrimsSurroundingWhitespace() {
        assertEquals("咪咪", sanitizePetName("  咪咪 "))
        assertEquals("咪咪", sanitizePetName("\t咪咪\t"))
    }

    @Test
    fun sanitizePetNameStripsInnerControlChars() {
        assertEquals("小咪", sanitizePetName("小\n咪"))
        assertEquals("小咪", sanitizePetName("小\r\n咪"))
    }

    @Test
    fun sanitizePetNameRejectsBlank() {
        assertNull(sanitizePetName(""))
        assertNull(sanitizePetName("   "))
        assertNull(sanitizePetName("\n\t"))
    }

    @Test
    fun sanitizePetNameEnforcesMaxLength() {
        assertEquals(MAX_PET_NAME_LENGTH, sanitizePetName("a".repeat(MAX_PET_NAME_LENGTH))?.length)
        assertNull(sanitizePetName("a".repeat(MAX_PET_NAME_LENGTH + 1)))
    }

    @Test
    fun sanitizePetNameAllowsChineseEmojiAndMixed() {
        assertEquals("小咪", sanitizePetName("小咪"))
        assertEquals("咪🐱", sanitizePetName("咪🐱"))
        assertEquals("Mimi", sanitizePetName("Mimi"))
    }
}
