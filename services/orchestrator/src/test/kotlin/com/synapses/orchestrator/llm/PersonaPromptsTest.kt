package com.synapses.orchestrator.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PersonaPromptsTest {
    @Test
    fun `TEST-PERSONA-001 exposes source persona prompts and builds profile messages`() {
        assertTrue(getPersonaPrompt("devto").contains("Principal Software Engineer"))
        assertTrue(getPersonaPrompt("twitter").contains("Developer Advocate"))
        assertTrue(getPersonaPrompt("slack_summary").contains("Tech Lead"))

        val messages = buildPromptWithProfile("devto", "write about ktor")
        assertEquals("system", messages[0]["role"])
        assertEquals(getPersonaPrompt("devto"), messages[0]["content"])
        assertEquals(mapOf("role" to "user", "content" to "write about ktor"), messages[1])

        val overrideMessages = buildPromptWithProfile("devto", "write", "  custom profile  ")
        assertEquals("custom profile", overrideMessages[0]["content"])
    }

    @Test
    fun `TEST-PERSONA-002 rejects unknown persona contexts`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            getPersonaPrompt("unknown")
        }

        assertEquals("Unknown persona context: unknown", error.message)
    }
}
