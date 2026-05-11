package com.cardpulse.app.data
import android.content.Context
import android.util.Log
import com.cardpulse.app.model.CardDataRoot

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
}
