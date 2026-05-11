package com.cardpulse.app.data
import android.util.Log
import com.cardpulse.app.model.*
import com.google.gson.Gson

object CardDataParser {
    private val gson = Gson()
    fun parseJson(json: String): CardDataRoot = gson.fromJson(json, CardDataRoot::class.java)
    
    fun matchCard(root: CardDataRoot, cardPrefix: String, bankHint: String?, nameHint: String?): ResolvedCard? {
        val candidates = mutableListOf<Triple<CardEntry, BankEntry, GroupEntry>>()
        for (bank in root.banks) {
            if (bankHint != null && !bank.b.contains(bankHint, true)) continue
            val bankBin = matchesBin(bank.bn, cardPrefix)
            bank.g?.forEach { group ->
                val groupBin = matchesBin(group.bn, cardPrefix)
                group.c?.forEach { card ->
                    val cardBin = matchesBin(card.bn, cardPrefix)
                    if (cardBin || groupBin || bankBin) candidates.add(Triple(card, bank, group))
                }
                if (group.c == null && (groupBin || bankBin)) {
                    val pseudo = CardEntry(group.n, null, null, null, null, null, null, null, null, null, group.bn, null, null, null, null)
                    candidates.add(Triple(pseudo, bank, group))
                }
            }
        }
        return candidates.firstOrNull()?.let { (card, bank, group) -> resolveCard(bank, group, card) }
    }
    
    fun searchCards(root: CardDataRoot, query: String): List<ResolvedCard> {
        val results = mutableListOf<ResolvedCard>()
        val lq = query.lowercase()
        for (bank in root.banks) {
            bank.g?.forEach { group ->
                group.c?.forEach { card ->
                    if ("${bank.b} ${group.n} ${card.n}".lowercase().contains(lq))
                        results.add(resolveCard(bank, group, card))
                }
                if (group.c == null && "${bank.b} ${group.n}".lowercase().contains(lq)) {
                    val pseudo = CardEntry(group.n, null, null, null, null, null, null, null, null, null, group.bn, null, null, null, null)
                    results.add(resolveCard(bank, group, pseudo))
                }
            }
        }
        return results
    }
    
    private fun resolveCard(bank: BankEntry, group: GroupEntry, card: CardEntry): ResolvedCard {
        val gx = group.x ?: emptyList()
        val cx = card.x ?: emptyList()
        val perks = mutableListOf<Perk>()
        perks.addAll((bank.p ?: emptyList()).filter { it.i !in gx && it.i !in cx })
        perks.addAll((group.p ?: emptyList()).filter { it.i !in cx })
        perks.addAll(card.p ?: emptyList())
        val milestones = mutableListOf<Milestone>()
        milestones.addAll(bank.m ?: emptyList())
        milestones.addAll(group.m ?: emptyList())
        milestones.addAll(card.m ?: emptyList())
        return ResolvedCard(bank.b, group.n, card.n, card.nw ?: "Unknown", card.t ?: "Unknown", card.af ?: 0, card.jf ?: 0, card.fs ?: 0, card.fm ?: 0.0, card.rv ?: 1.0, card.cl ?: "#1A1A2E", card.cf ?: "m", perks, milestones, card.cv ?: emptyList())
    }
    
    private fun matchesBin(bn: BinMetadata?, prefix: String): Boolean {
        if (bn == null) return false
        bn.px?.forEach { if (prefix.startsWith(it)) return true }
        bn.rg?.forEach { if (prefix >= it.f && prefix <= it.t) return true }
        return false
    }
}
