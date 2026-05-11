package com.cardpulse.app.model

import com.google.gson.annotations.SerializedName

data class CardDataRoot(val v: Int, val banks: List<BankEntry>)
data class BankEntry(val b: String, val bn: BinMetadata?, val p: List<Perk>?, val m: List<Milestone>?, val g: List<GroupEntry>?)
data class GroupEntry(val n: String, val bn: BinMetadata?, val p: List<Perk>?, val m: List<Milestone>?, val c: List<CardEntry>?, val x: List<String>?)
data class CardEntry(val n: String, val nw: String?, val t: String?, val af: Int?, val jf: Int?, val fs: Int?, val fm: Double?, val rv: Double?, val cl: String?, val cf: String?, val bn: BinMetadata?, val p: List<Perk>?, val m: List<Milestone>?, val cv: List<String>?, val x: List<String>?)
data class BinMetadata(val px: List<String>?, val rg: List<BinRange>?, val ln: List<Int>?)
data class BinRange(val f: String, val t: String)
data class Perk(val i: String, val n: String, val rt: String, val cy: String, val mn: Int?, val up: UptoLimit?, val md: PerkModification?, val x: List<String>?)
data class UptoLimit(val t: String, val v: Int)
data class PerkModification(val mn: Int?, val up: UptoLimit?, val x: List<String>?)
data class Milestone(
    val i: String,
    val n: String,
    @SerializedName(value = "ta", alternate = ["tgt"]) val ta: Int,
    val rw: String,
    val rt: String,
    val cy: String
)
data class ResolvedCard(val bankName: String, val groupName: String, val cardName: String, val cardNetwork: String, val cardType: String, val annualFee: Int, val joiningFee: Int, val feeWaiverSpend: Int, val foreignMarkup: Double, val rewardValue: Double, val color: String, val confidence: String, val perks: List<Perk>, val milestones: List<Milestone>, val caveats: List<String>)
