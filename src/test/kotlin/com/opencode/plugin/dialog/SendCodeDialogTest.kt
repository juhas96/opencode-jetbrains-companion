package com.opencode.plugin.dialog

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

class SendCodeDialogTest {
    
    @Nested
    inner class TemplatesTests {
        
        @Test
        fun `TEMPLATES contains expected number of entries`() {
            assertEquals(9, SendCodeDialog.TEMPLATES.size)
        }
        
        @Test
        fun `first template is custom prompt placeholder`() {
            val (key, display) = SendCodeDialog.TEMPLATES[0]
            
            assertEquals("", key)
            assertEquals("(Custom prompt...)", display)
        }
        
        @Test
        fun `explain template exists`() {
            val template = SendCodeDialog.TEMPLATES.find { it.first == "explain" }
            
            assertTrue(template != null)
            assertEquals("Explain this code", template?.second)
        }
        
        @Test
        fun `refactor template exists`() {
            val template = SendCodeDialog.TEMPLATES.find { it.first == "refactor" }
            
            assertTrue(template != null)
            assertTrue(template?.second?.contains("refactor", ignoreCase = true) == true)
        }
        
        @Test
        fun `tests template exists`() {
            val template = SendCodeDialog.TEMPLATES.find { it.first == "tests" }
            
            assertTrue(template != null)
            assertTrue(template?.second?.contains("test", ignoreCase = true) == true)
        }
        
        @Test
        fun `fix template exists`() {
            val template = SendCodeDialog.TEMPLATES.find { it.first == "fix" }
            
            assertTrue(template != null)
            assertTrue(template?.second?.contains("bug", ignoreCase = true) == true)
        }
        
        @Test
        fun `optimize template exists`() {
            val template = SendCodeDialog.TEMPLATES.find { it.first == "optimize" }
            
            assertTrue(template != null)
            assertTrue(template?.second?.contains("performance", ignoreCase = true) == true)
        }
        
        @Test
        fun `document template exists`() {
            val template = SendCodeDialog.TEMPLATES.find { it.first == "document" }
            
            assertTrue(template != null)
            assertTrue(template?.second?.contains("document", ignoreCase = true) == true)
        }
        
        @Test
        fun `review template exists`() {
            val template = SendCodeDialog.TEMPLATES.find { it.first == "review" }
            
            assertTrue(template != null)
            assertTrue(template?.second?.contains("review", ignoreCase = true) == true)
        }
        
        @Test
        fun `types template exists`() {
            val template = SendCodeDialog.TEMPLATES.find { it.first == "types" }
            
            assertTrue(template != null)
            assertTrue(template?.second?.contains("type", ignoreCase = true) == true)
        }
        
        @Test
        fun `all template keys are unique`() {
            val keys = SendCodeDialog.TEMPLATES.map { it.first }
            val uniqueKeys = keys.toSet()
            
            assertEquals(keys.size, uniqueKeys.size)
        }
        
        @Test
        fun `all template display names are non-empty except custom`() {
            SendCodeDialog.TEMPLATES.forEachIndexed { index, (key, display) ->
                if (index > 0) {
                    assertTrue(display.isNotBlank(), "Template '$key' has blank display name")
                }
            }
        }
        
        @Test
        fun `template keys are lowercase`() {
            SendCodeDialog.TEMPLATES.forEach { (key, _) ->
                if (key.isNotEmpty()) {
                    assertEquals(key.lowercase(), key, "Template key '$key' should be lowercase")
                }
            }
        }
    }
    
    @Nested
    inner class TemplateOrderTests {
        
        @Test
        fun `custom prompt is first option`() {
            assertEquals("", SendCodeDialog.TEMPLATES[0].first)
        }
        
        @Test
        fun `explain comes early in list`() {
            val explainIndex = SendCodeDialog.TEMPLATES.indexOfFirst { it.first == "explain" }
            
            assertTrue(explainIndex in 1..3, "Explain should be near the top")
        }
    }
}
