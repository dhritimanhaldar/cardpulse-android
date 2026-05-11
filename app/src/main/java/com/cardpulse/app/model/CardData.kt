package com.cardpulse.app.model

// Root structure
data class CardDataRoot(
    val v: Int,
    val banks: List<BankEntry>
)

data class BankEntry(
    val b: String,                    // bankName
    val bn: BinMetadata?,             // BIN metadata
    val p: List<Perk>?,               // bank-level perks
    val m: List<Milestone>?,          // bank-level milestones
    val g: List<GroupEntry>?          // groups
)

data class GroupEntry(
    val n: String,                    // group/card family name
    val bn: BinMetadata?,
    val p: List<Perk>?,
    val m: List<Milestone>?,
    val c: List<CardEntry>?,          // cards in this group
    val x: List<String>?              // exclusions
)

data class CardEntry(
    val n: String,                    // card name
    val nw: String?,                  // cardNetwork
    val t: String?,                   // cardType
    val af: Int?,                     // annualFee
    val jf: Int?,                     // joiningFee
    val fs: Int?,                     // feeWaiverSpend
    val fm: Double?,                  // foreignMarkup
    val rv: Double?,                  // rewardValue
    val cl: String?,                  // color
    val cf: String?,                  // confidence
    val bn: BinMetadata?,
    val p: List<Perk>?,
    val m: List<Milestone>?,
    val cv: List<String>?,            // caveats
    val x: List<String>?              // exclusions
)

data class BinMetadata(
    val px: List<String>?,            // exact prefixes
    val rg: List<BinRange>?,          // ranges
    val ln: List<Int>?                // valid card lengths
)

data class BinRange(
    val f: String,                    // from
    val t: String                     // to
)

data class Perk(
    val i: String,                    // perk id
    val n: String,                    // name
    val rt: String,                   // rewardType
    val cy: String,                   // cycleType
    val mn: Int?,                     // minimum transaction
    val up: UptoLimit?,               // upto cap
    val md: PerkModification?,        // modifications
    val x: List<String>?              // exclusions
)

data class UptoLimit(
    val t: String,                    // type: "sp" = spend, "rw" = reward
    val v: Int                        // value
)

data class PerkModification(
    val mn: Int?,
    val up: UptoLimit?,
    val x: List<String>?
)

data class Milestone(
    val i: String,
    val n: String,
    val ta: Int,                      // targetAmount
    val rw: String,                   // reward
    val rt: String,                   // rewardType
    val cy: String                    // cycleType
)
