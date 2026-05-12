package com.cardpulse.app.util

fun cleanCardName(rawName: String, bankName: String): String {
    val bankWords = bankName
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    val words = rawName
        .replace("•", " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .toMutableList()

    while (bankWords.isNotEmpty() && words.size >= bankWords.size) {
        val startsWithBank = bankWords.indices.all { index ->
            words[index].equals(bankWords[index], ignoreCase = true)
        }
        if (!startsWithBank) break
        repeat(bankWords.size) { words.removeAt(0) }
    }

    while (words.firstOrNull()?.equals(bankWords.firstOrNull().orEmpty(), ignoreCase = true) == true) {
        words.removeAt(0)
    }

    if (words.firstOrNull()?.equals("bank", ignoreCase = true) == true) {
        words.removeAt(0)
    }

    val deduped = words.filterIndexed { index, word ->
        index == 0 || !word.equals(words.getOrNull(index - 1), ignoreCase = true)
    }

    return deduped
        .joinToString(" ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { "Card" }
}

fun cleanAndNormalizeBankName(rawName: String): String {
    val cleaned = rawName
        .trim()
        .replace(Regex("\\s+Ltd\\.?\\s*$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+Limited\\s*$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    val words = cleaned
        .split(" ")
        .filter { it.isNotBlank() }
        .filterIndexed { index, word ->
            index == 0 || !word.equals(cleaned.split(" ").getOrNull(index - 1), ignoreCase = true)
        }

    return words.joinToString(" ").trim().ifBlank { rawName.trim() }
}
