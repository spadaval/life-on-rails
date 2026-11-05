package com.spadaval.lor.t3

/**
 * Answering a pending question, ported from T3's `packages/client-runtime/src/state/pendingUserInput.ts`
 * (`resolvePendingUserInputAnswer` and its helpers) and `seedUserInputDraftAnswers` (`threadRequests.ts`).
 * The launcher attaches no files, so the attachment cases are left out. Pure.
 */
object UserInput {
    /** What has been chosen or typed for one question so far (T3's `PendingUserInputDraftAnswer`). */
    data class Draft(val selected: List<String> = emptyList(), val custom: String? = null)

    /** Drafts for questions with an [Question.initialAnswer], unless they have one already. */
    fun seed(questions: List<Question>, drafts: Map<String, Draft>): Map<String, Draft> {
        var seeded = drafts
        for (q in questions) {
            if (q.initialAnswer == null || !q.allowCustomAnswer || q.id in seeded) continue
            seeded = seeded + (q.id to Draft(custom = q.initialAnswer))
        }
        return seeded
    }

    /** The answer to send for [q]: a string, or a list for a multi-select; null while unanswered. */
    fun resolve(q: Question, draft: Draft?): Any? {
        val custom = when {
            !q.allowCustomAnswer -> null
            q.initialAnswer != null -> draft?.custom
            else -> trimmed(draft?.custom)
        }
        if (!custom.isNullOrEmpty()) return custom
        val selected = selected(q, draft?.selected)
        return if (q.multiSelect) selected.ifEmpty { null } ?: custom else selected.firstOrNull() ?: custom
    }

    /** Every question's answer by id, or null while any is unanswered (`buildPendingUserInputAnswers`). */
    fun build(questions: List<Question>, drafts: Map<String, Draft>): Map<String, Any>? {
        val answers = LinkedHashMap<String, Any>()
        for (q in questions) answers[q.id] = resolve(q, drafts[q.id]) ?: return null
        return answers
    }

    /** Typed text; it clears the chosen options once it has content (`setPendingUserInputCustomAnswer`). */
    fun setCustom(q: Question, draft: Draft?, text: String): Draft {
        if (!q.allowCustomAnswer) return draft ?: Draft()
        val keep = if (q.initialAnswer != null || text.trim().isNotEmpty()) emptyList() else selected(q, draft?.selected)
        return Draft(keep, text)
    }

    /** Whether [optionValue] shows as chosen; typed text wins over every option (`isPendingUserInputOptionSelected`). */
    fun isSelected(q: Question, draft: Draft?, optionValue: String): Boolean {
        if (q.allowCustomAnswer && trimmed(draft?.custom) != null) return false
        val value = optionValue(q, optionValue) ?: return false
        return value in selected(q, draft?.selected)
    }

    /** Picks an option, or for a multi-select toggles it; either clears typed text (`togglePendingUserInputOptionSelection`). */
    fun toggle(q: Question, draft: Draft?, optionValue: String): Draft {
        val value = optionValue(q, optionValue) ?: return draft ?: Draft()
        if (!q.multiSelect) return Draft(listOf(value), "")
        val selected = selected(q, draft?.selected)
        return Draft(if (value in selected) selected - value else selected + value, "")
    }

    /** The chosen values that name an offered option, deduplicated, in order (`normalizeSelectedOptionValues`). */
    private fun selected(q: Question, values: List<String>?): List<String> =
        values.orEmpty().mapNotNull { optionValue(q, it) }.distinct()

    /** Option values match exactly; an option without a value matches by its trimmed label. */
    private fun optionValue(q: Question, value: String): String? {
        if (q.options.any { it.value == value }) return value
        val label = value.trim()
        return label.takeIf { it.isNotEmpty() && q.options.any { o -> o.value == null && o.label.trim() == it } }
    }

    private fun trimmed(s: String?): String? = s?.trim()?.ifEmpty { null }
}
