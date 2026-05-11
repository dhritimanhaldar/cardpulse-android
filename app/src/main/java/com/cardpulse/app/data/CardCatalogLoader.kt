package com.cardpulse.app.data
import android.content.Context
import android.util.Log
import com.cardpulse.app.model.CardDataRoot
import com.cardpulse.app.model.Milestone
import com.cardpulse.app.model.Perk

object CardCatalogLoader {
    private var cachedRoot: CardDataRoot? = null
    fun loadCardData(context: Context): CardDataRoot {
        if (cachedRoot != null) return cachedRoot!!
        Log.d("CardCatalogLoader", "Loading card_data.json from assets...")
        val jsonString = context.assets.open("card_data.json").bufferedReader().use { it.readText() }
        cachedRoot = CardDataParser.parseJson(jsonString)
        Log.d("CardCatalogLoader", "Loaded ${cachedRoot!!.banks.size} banks")
        return cachedRoot!!
    }

    fun getDefaultPerksAndMilestones(): Pair<List<Perk>, List<Milestone>> {
        val defaultPerks = listOf(
            Perk("default1", "Base Rewards", "p", "m", 150, null, null, null)
        )
        val defaultMilestones = listOf(
            Milestone("default_m1", "Monthly Target", 50000, "1000 points", "p", "m")
        )
        return defaultPerks to defaultMilestones
    }
}
