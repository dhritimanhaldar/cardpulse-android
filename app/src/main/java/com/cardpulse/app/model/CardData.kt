package com.cardpulse.app.model

import com.google.gson.annotations.SerializedName

data class CardDataRoot(
    val v: Int,
    val banks: List<BankEntry>
)

data class BankEntry(
    val b: String,
    val bn: BinMetadata? = null,
    val p: List<Perk>? = null,
    val m: List<Milestone>? = null,
    val g: List<GroupEntry>? = null
)

data class GroupEntry(
    val n: String,
    val bn: BinMetadata? = null,
    val p: List<Perk>? = null,
    val m: List<Milestone>? = null,
    val c: List<CardEntry>? = null,
    val x: List<String>? = null,
    val cb: String? = null
)

data class CardEntry(
    val n: String,
    val nw: String? = null,
    val t: String? = null,
    val af: Int? = null,
    val jf: Int? = null,
    val fs: Int? = null,
    val fm: Double? = null,
    val rv: Double? = null,
    val cl: String? = null,
    val cf: String? = null,
    val bn: BinMetadata? = null,
    val p: List<Perk>? = null,
    val m: List<Milestone>? = null,
    val cv: List<String>? = null,
    val x: List<String>? = null,
    val cb: String? = null,
    val fl: String? = null
)

data class BinMetadata(
    val px: List<String>? = null,
    val rg: List<BinRange>? = null,
    val ln: List<Int>? = null
)

data class BinRange(
    val f: String,
    val t: String
)

data class Perk(
    val i: String,
    val n: String,
    val rt: String,
    val cy: String,
    val mn: Int? = null,
    val up: UptoLimit? = null,
    @SerializedName("md") val md: PerkModification? = null,
    val x: List<String>? = null,
    val vp: Int? = null,
    val nw: String? = null
)

data class UptoLimit(
    val t: String,
    val v: Int
)

data class PerkModification(
    val mn: Int? = null,
    val up: UptoLimit? = null,
    val x: List<String>? = null,
    val n: String? = null,
    val vp: Int? = null,
    val cy: String? = null
)

data class Milestone(
    val i: String,
    val n: String,
    @SerializedName(value = "ta", alternate = ["tgt"]) val ta: Int,
    val rw: String,
    val rt: String,
    val cy: String
)

data class ResolvedCard(
    val bankName: String,
    val groupName: String,
    val cardName: String,
    val cardNetwork: String,
    val cardType: String,
    val annualFee: Int,
    val joiningFee: Int,
    val feeWaiverSpend: Int,
    val foreignMarkup: Double,
    val rewardValue: Double,
    val color: String,
    val confidence: String,
    val perks: List<Perk>,
    val milestones: List<Milestone>,
    val caveats: List<String>
)