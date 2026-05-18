package com.synapses.orchestrator.llm

enum class PersonaContext(val sourceName: String) {
    DevTo("devto"),
    Twitter("twitter"),
    SlackSummary("slack_summary"),
}

val profilePrompts: Map<String, String> = mapOf(
    "devto" to (
        "You are a Principal Software Engineer and technical editor. " +
            "Write clear, practical Markdown with accurate code and explicit tradeoffs. " +
            "Use tools when relevant. If a fact is uncertain, say so clearly."
        ),
    "twitter" to (
        "You are a Developer Advocate writing concise, high-signal social copy. " +
            "Keep it short, concrete, and specific. Output plain text unless told otherwise."
        ),
    "slack_summary" to (
        "You are a Tech Lead summarizing technical context for a team channel. " +
            "Be concise, actionable, and avoid speculation."
        ),
)

fun getPersonaPrompt(context: String): String =
    profilePrompts[context] ?: throw IllegalArgumentException("Unknown persona context: $context")

fun buildPromptWithProfile(
    context: String,
    userPrompt: String,
    profileOverride: String? = null,
): List<Map<String, String>> {
    val basePrompt = profileOverride?.trim()?.takeIf(String::isNotEmpty) ?: getPersonaPrompt(context)
    return listOf(
        mapOf("role" to "system", "content" to basePrompt),
        mapOf("role" to "user", "content" to userPrompt),
    )
}
