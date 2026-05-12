package com.cardpulse.app.data

import com.cardpulse.app.model.*
import com.google.gson.Gson

object CardDataParser {
    private val gson = Gson()

    fun parseJson(json: String): CardDataRoot = gson.fromJson(json, CardDataRoot::class.java)

    fun matchCard(
        root: CardDataRoot,
        cardPrefix: String,
        bankHint: String?,
        nameHint: String?
    ): ResolvedCard? {
        val candidates = mutableListOf<Triple<CardEntry, BankEntry, GroupEntry>>()

        for (bank in root.banks) {
            if (bankHint != null && !bank.b.contains(bankHint, ignoreCase = true)) continue
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

        for (bank in root.banks) {
            bank.g.orEmpty().forEach { group ->
                group.c.orEmpty().forEach { card ->
                    if ("${bank.b} ${group.n} ${card.n}".lowercase().contains(lq)) {
                        results.add(resolveCard(bank, group, card))
                    }
                }

                if (group.c.isNullOrEmpty() && "${bank.b} ${group.n}".lowercase().contains(lq)) {
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

    fun resolveCard(bank: BankEntry, group: GroupEntry, card: CardEntry): ResolvedCard {
        val gx = group.x.orEmpty()
        val cx = card.x.orEmpty()

        val perks = mutableListOf<Perk>()
        perks.addAll(bank.p.orEmpty().filter { it.i !in gx && it.i !in cx })
        perks.addAll(group.p.orEmpty().filter { it.i !in cx })
        perks.addAll(card.p.orEmpty())

        val milestones = mutableListOf<Milestone>()
        milestones.addAll(bank.m.orEmpty())
        milestones.addAll(group.m.orEmpty())
        milestones.addAll(card.m.orEmpty())

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
            perks = perks,
            milestones = milestones,
            caveats = card.cv.orEmpty()
        )
    }

    private fun matchesBin(bn: BinMetadata?, prefix: String): Boolean {
        if (bn == null) return false
        bn.px.orEmpty().forEach { if (prefix.startsWith(it)) return true }
        bn.rg.orEmpty().forEach { if (prefix >= it.f && prefix <= it.t) return true }
        return false
    }
}