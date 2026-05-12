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
