package com.cardpulse.app.data

import android.content.Context
import com.cardpulse.app.model.*
import com.google.gson.Gson
import java.io.IOException

object CardDataParser {
    private val gson = Gson()
    private var cachedCardData: CardDataRoot? = null

    fun loadCardData(context: Context): CardDataRoot? {
        if (cachedCardData != null) {
            return cachedCardData
        }

        return try {
            val jsonString = context.assets.open("card_data.json")
                .bufferedReader()
                .use { it.readText() }

            cachedCardData = gson.fromJson(jsonString, CardDataRoot::class.java)
            cachedCardData
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun parseJson(json: String): CardDataRoot = gson.fromJson(json, CardDataRoot::class.java)

    fun matchCard(
        root: CardDataRoot,
        cardPrefix: String,
        bankHint: String?,
        nameHint: String?
    ): ResolvedCard? {
        val normalizedBankHint = bankHint?.let(::normalize)
        val normalizedNameHint = nameHint?.let(::normalize)
        val candidates = findCandidateMatches(root, cardPrefix)
            .filter { candidate ->
                val bankMatches = normalizedBankHint == null ||
                    normalize(candidate.bankName).contains(normalizedBankHint) ||
                    normalizedBankHint.contains(normalize(candidate.bankName))
                val nameMatches = normalizedNameHint == null ||
                    normalize(candidate.cardName).contains(normalizedNameHint) ||
                    normalize(candidate.displayName).contains(normalizedNameHint)
                bankMatches && nameMatches
            }

        val bestCandidate = narrowToBestCandidates(candidates).firstOrNull() ?: return null
        return searchCards(root, "${bestCandidate.bankName} ${bestCandidate.cardName}")
            .firstOrNull { resolved ->
                normalize(resolved.bankName) == normalize(bestCandidate.bankName) &&
                    normalize(resolved.cardName) == normalize(bestCandidate.cardName)
            }
            ?: candidates.firstOrNull()?.let { candidate ->
                val bank = root.banks.firstOrNull { normalize(it.b) == normalize(candidate.bankName) } ?: return null
                val group = bank.g.orEmpty().firstOrNull { normalize(it.n) == normalize(candidate.groupName) } ?: return null
                val card = group.c.orEmpty().firstOrNull { normalize(it.n) == normalize(candidate.cardName) }
                    ?: CardEntry(n = candidate.cardName)
                resolveCard(bank, group, card)
            }
    }

    fun findCandidateMatches(root: CardDataRoot, prefix: String): List<ResolvedCardCandidate> {
        val digits = prefix.filter(Char::isDigit)
        if (digits.isBlank()) return emptyList()

        val rawCandidates = mutableListOf<ResolvedCardCandidate>()

        root.banks.forEach { bank ->
            val bankMatchSource = matchSource(bank.bn, digits)

            bank.g.orEmpty().forEach { group ->
                val groupMatchSource = matchSource(group.bn, digits)
                val cards = group.c.orEmpty().ifEmpty {
                    listOf(
                        CardEntry(
                            n = group.n,
                            bn = group.bn,
                            cl = group.cb
                        )
                    )
                }

                cards.forEach { card ->
                    val cardMatchSource = matchSource(card.bn, digits)
                    val specificity = when {
                        cardMatchSource != null -> MatchSpecificity.CARD
                        groupMatchSource != null -> MatchSpecificity.GROUP
                        bankMatchSource != null -> MatchSpecificity.BANK
                        else -> null
                    } ?: return@forEach

                    val matchedSource = cardMatchSource ?: groupMatchSource ?: bankMatchSource ?: return@forEach
                    val resolved = resolveCard(bank, group, card)
                    val lengths = listOfNotNull(card.bn?.ln, group.bn?.ln, bank.bn?.ln)
                        .flatten()
                        .distinct()
                        .sorted()

                    rawCandidates += ResolvedCardCandidate(
                        bankCode = bank.b,
                        bankName = bank.b,
                        groupName = group.n,
                        cardName = resolved.cardName,
                        cardNetwork = resolved.cardNetwork,
                        cardType = resolved.cardType,
                        annualFee = resolved.annualFee,
                        color = resolved.color,
                        specificity = specificity,
                        matchedPrefixSource = matchedSource,
                        supportedLengths = lengths,
                        displayName = buildDisplayName(group.n, resolved.cardName)
                    )
                }
            }
        }

        return rawCandidates
            .groupBy { candidate ->
                listOf(
                    normalize(candidate.bankName),
                    normalize(candidate.groupName),
                    normalize(candidate.cardName)
                ).joinToString("|")
            }
            .values
            .map { duplicates ->
                duplicates.maxByOrNull { specificityRank(it.specificity) } ?: duplicates.first()
            }
            .sortedWith(
                compareByDescending<ResolvedCardCandidate> { specificityRank(it.specificity) }
                    .thenBy { it.bankName.lowercase() }
                    .thenBy { it.displayName.lowercase() }
            )
    }

    fun narrowToBestCandidates(candidates: List<ResolvedCardCandidate>): List<ResolvedCardCandidate> {
        if (candidates.isEmpty()) return emptyList()
        val bestRank = candidates.maxOf { specificityRank(it.specificity) }
        val sameSpecificity = candidates.filter { specificityRank(it.specificity) == bestRank }
        val bestPrefixRank = sameSpecificity.maxOf { matchedPrefixRank(it.matchedPrefixSource) }
        return sameSpecificity.filter { matchedPrefixRank(it.matchedPrefixSource) == bestPrefixRank }
    }

    fun searchCards(root: CardDataRoot, query: String): List<ResolvedCard> {
        val results = mutableListOf<ResolvedCard>()
        val lq = query.lowercase()
        val nq = normalize(query)

        for (bank in root.banks) {
            bank.g.orEmpty().forEach { group ->
                group.c.orEmpty().forEach { card ->
                    val haystack = "${bank.b} ${group.n} ${card.n}".lowercase()
                    val normalizedHaystack = normalize(haystack)
                    if (haystack.contains(lq) ||
                        normalizedHaystack.contains(nq) ||
                        tokenMatch(query, haystack) ||
                        queryMatchesParts(nq, bank, group, card)
                    ) {
                        results.add(resolveCard(bank, group, card))
                    }
                }

                val groupHaystack = "${bank.b} ${group.n}".lowercase()
                if (group.c.isNullOrEmpty() &&
                    (groupHaystack.contains(lq) || normalize(groupHaystack).contains(nq) || tokenMatch(query, groupHaystack))
                ) {
                    val pseudo = CardEntry(
                        n = group.n,
                        bn = group.bn
                    )
                    results.add(resolveCard(bank, group, pseudo))
                }
            }
        }

        return results
    }

    private fun queryMatchesParts(query: String, bank: BankEntry, group: GroupEntry, card: CardEntry): Boolean {
        val bankName = normalize(bank.b)
        val groupName = normalize(group.n)
        val cardName = normalize(card.n)
        val withoutBankWord = query
            .removePrefix(bankName)
            .removePrefix(bankName.removeSuffix("bank"))
        return query.contains(bankName.removeSuffix("bank")) &&
                (cardName.contains(withoutBankWord) ||
                        withoutBankWord.contains(cardName) ||
                        cardName.contains(groupName) ||
                        query.contains(groupName))
    }

    fun resolveCard(bank: BankEntry, group: GroupEntry, card: CardEntry): ResolvedCard {
        val inheritedPerks = mergePerks(emptyList(), bank.p.orEmpty())
        val groupPerks = mergePerks(
            inheritedPerks.filterNot { perk -> group.x.orEmpty().contains(perk.i) },
            group.p.orEmpty()
        )
        val finalPerks = mergePerks(
            groupPerks.filterNot { perk -> card.x.orEmpty().contains(perk.i) },
            card.p.orEmpty()
        )

        val inheritedMilestones = mergeMilestones(emptyList(), bank.m.orEmpty())
        val groupMilestones = mergeMilestones(
            inheritedMilestones.filterNot { milestone -> group.x.orEmpty().contains(milestone.i) },
            group.m.orEmpty()
        )
        val finalMilestones = mergeMilestones(
            groupMilestones.filterNot { milestone -> card.x.orEmpty().contains(milestone.i) },
            card.m.orEmpty()
        )

        val caveats = mutableSetOf<String>()
        card.cv?.let { caveats.addAll(it) }

        return ResolvedCard(
            bankName = bank.b,
            groupName = group.n,
            cardName = card.n,
            cardNetwork = card.nw ?: "Unknown",
            cardType = card.t ?: "Unknown",
            annualFee = card.af ?: 0,
            joiningFee = card.jf ?: 0,
            feeWaiverSpend = card.fs ?: 0,
            foreignMarkup = card.fm ?: 0.0,
            rewardValue = card.rv ?: 1.0,
            color = card.cl ?: "#1A1A2E",
            confidence = decodeConfidence(card.cf),
            perks = finalPerks,
            milestones = finalMilestones,
            caveats = caveats.toList()
        )
    }

    fun matchCardByBIN(
        cardDataRoot: CardDataRoot,
        cardNumber: String
    ): Pair<String, String>? {
        val first6 = cardNumber.take(6)
        val first8 = cardNumber.take(8)

        cardDataRoot.banks.forEach { bank ->
            bank.bn?.let { if (matchesBinMetadata(first6, first8, it)) return Pair(bank.b, "") }

            bank.g.orEmpty().forEach { group ->
                group.bn?.let {
                    if (matchesBinMetadata(first6, first8, it)) {
                        group.c.orEmpty().forEach { card ->
                            card.bn?.let { if (matchesBinMetadata(first6, first8, it)) return Pair(bank.b, card.n) }
                        }
                        return Pair(bank.b, group.n)
                    }
                }

                group.c.orEmpty().forEach { card ->
                    card.bn?.let { if (matchesBinMetadata(first6, first8, it)) return Pair(bank.b, card.n) }
                }
            }
        }
        return null
    }

    private fun matchesBinMetadata(
        first6: String,
        first8: String,
        binMetadata: BinMetadata
    ): Boolean {
        binMetadata.px.orEmpty().forEach { if (first6.startsWith(it) || first8.startsWith(it)) return true }
        binMetadata.rg.orEmpty().forEach { range ->
            val cardPrefix = if (range.f.length <= 6) first6 else first8
            if (cardPrefix >= range.f && cardPrefix <= range.t) return true
        }
        return false
    }

    private fun matchesBin(bn: BinMetadata?, prefix: String): Boolean {
        return matchSource(bn, prefix) != null
    }

    private fun normalize(value: String): String {
        return value.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    private fun tokenMatch(query: String, haystack: String): Boolean {
        val ignored = setOf("bank", "card", "credit", "ltd", "limited")
        val tokens = query.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in ignored }
        if (tokens.isEmpty()) return false
        return tokens.all { haystack.contains(it, ignoreCase = true) }
    }

    private fun matchSource(bn: BinMetadata?, prefix: String): String? {
        val digits = prefix.filter(Char::isDigit)
        if (bn == null || digits.isBlank()) return null

        bn.px.orEmpty().firstOrNull { candidate ->
            prefixMatches(candidate, digits)
        }?.let { return "prefix:$it" }

        bn.rg.orEmpty().firstOrNull { range ->
            rangeMatches(range, digits)
        }?.let { return "range:${it.f}-${it.t}" }

        return null
    }

    private fun prefixMatches(candidate: String, entered: String): Boolean {
        return if (entered.length <= candidate.length) {
            candidate.startsWith(entered)
        } else {
            entered.startsWith(candidate)
        }
    }

    private fun rangeMatches(range: BinRange, entered: String): Boolean {
        val enteredDigits = entered.filter(Char::isDigit)
        if (enteredDigits.isBlank()) return false

        val compareLength = minOf(enteredDigits.length, range.f.length, range.t.length)
        if (compareLength == 0) return false

        val enteredSlice = enteredDigits.take(compareLength)
        val fromSlice = range.f.take(compareLength)
        val toSlice = range.t.take(compareLength)
        return enteredSlice >= fromSlice && enteredSlice <= toSlice
    }

    private fun specificityRank(specificity: MatchSpecificity): Int {
        return when (specificity) {
            MatchSpecificity.CARD -> 3
            MatchSpecificity.GROUP -> 2
            MatchSpecificity.BANK -> 1
        }
    }

    private fun matchedPrefixRank(source: String): Int {
        return source.substringAfter(':', "")
            .substringBefore('-')
            .filter(Char::isDigit)
            .length
    }

    private fun buildDisplayName(groupName: String, cardName: String): String {
        val normalizedGroup = normalize(groupName)
        val normalizedCard = normalize(cardName)
        return when {
            normalizedCard == normalizedGroup -> cardName
            normalizedCard.startsWith(normalizedGroup) -> cardName
            else -> "$groupName $cardName".replace("\\s+".toRegex(), " ").trim()
        }
    }

    private fun mergePerks(base: List<Perk>, additions: List<Perk>): List<Perk> {
        val merged = base.associateBy { it.i }.toMutableMap()
        additions.forEach { perk ->
            val current = merged[perk.i]
            merged[perk.i] = when {
                current == null -> perk
                perk.md != null -> applyPerkModification(current, perk.md, perk)
                else -> perk
            }
        }
        return merged.values.toList()
    }

    private fun mergeMilestones(base: List<Milestone>, additions: List<Milestone>): List<Milestone> {
        val merged = base.associateBy { it.i }.toMutableMap()
        additions.forEach { milestone ->
            merged[milestone.i] = milestone
        }
        return merged.values.toList()
    }

    private fun applyPerkModification(base: Perk, modification: PerkModification, override: Perk): Perk {
        return base.copy(
            n = modification.n ?: override.n.ifBlank { base.n },
            rt = override.rt.ifBlank { base.rt },
            cy = modification.cy ?: override.cy.ifBlank { base.cy },
            mn = modification.mn ?: override.mn ?: base.mn,
            up = modification.up ?: override.up ?: base.up,
            x = modification.x ?: override.x ?: base.x,
            vp = modification.vp ?: override.vp ?: base.vp,
            nw = override.nw ?: base.nw
        )
    }

    private fun decodeConfidence(raw: String?): String {
        return when (raw?.lowercase()) {
            "h" -> "high"
            "m" -> "medium"
            "l" -> "low"
            else -> "matched"
        }
    }
}
