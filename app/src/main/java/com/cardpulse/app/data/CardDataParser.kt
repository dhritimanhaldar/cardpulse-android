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
        val candidates = mutableListOf<Triple<CardEntry, BankEntry, GroupEntry>>()

        for (bank in root.banks) {
            if (bankHint != null &&
                !bank.b.contains(bankHint, ignoreCase = true) &&
                !bankHint.contains(bank.b, ignoreCase = true)
            ) continue
            val bankBin = matchesBin(bank.bn, cardPrefix)

            bank.g.orEmpty().forEach { group ->
                val groupBin = matchesBin(group.bn, cardPrefix)

                group.c.orEmpty().forEach { card ->
                    val cardBin = matchesBin(card.bn, cardPrefix)
                    if (cardBin || groupBin || bankBin) {
                        candidates.add(Triple(card, bank, group))
                    }
                }

                if (group.c.isNullOrEmpty() && (groupBin || bankBin)) {
                    val pseudo = CardEntry(
                        n = group.n,
                        bn = group.bn
                    )
                    candidates.add(Triple(pseudo, bank, group))
                }
            }
        }

        return candidates.firstOrNull()?.let { (card, bank, group) ->
            resolveCard(bank, group, card)
        }
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
        val gx = group.x.orEmpty()
        val cx = card.x.orEmpty()

        val perks = mutableListOf<Perk>()
        val milestones = mutableListOf<Milestone>()
        val caveats = mutableSetOf<String>()

        // Add bank-level perks and milestones
        bank.p?.let { perks.addAll(it) }
        bank.m?.let { milestones.addAll(it) }

        // Add group-level perks and milestones
        group.p?.let { perks.addAll(it) }
        group.m?.let { milestones.addAll(it) }

        // Apply group exclusions
        gx.forEach { exclusionId ->
            perks.removeAll { it.i == exclusionId }
            milestones.removeAll { it.i == exclusionId }
        }

        // Add card-level perks and milestones
        card.p?.let { perks.addAll(it) }
        card.m?.let { milestones.addAll(it) }
        card.cv?.let { caveats.addAll(it) }

        // Apply card exclusions
        cx.forEach { exclusionId ->
            perks.removeAll { it.i == exclusionId }
            milestones.removeAll { it.i == exclusionId }
        }

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
            confidence = "matched",
            perks = perks.distinctBy { it.i },
            milestones = milestones.distinctBy { it.i },
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
        if (bn == null) return false
        bn.px.orEmpty().forEach { if (prefix.startsWith(it)) return true }
        bn.rg.orEmpty().forEach { if (prefix >= it.f && prefix <= it.t) return true }
        return false
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
}
