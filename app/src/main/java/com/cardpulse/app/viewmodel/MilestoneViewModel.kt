package com.cardpulse.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.gemini.GeminiService
import com.cardpulse.app.model.SpendRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MilestoneViewModel(
    private val context: Context,
    private val cardId: Int
) : ViewModel() {

    private val repository = CardRepository(context)
    private val geminiService = GeminiService()

    private val _isFetchingMilestones = MutableStateFlow(false)
    val isFetchingMilestones: StateFlow<Boolean> = _isFetchingMilestones

    private val _milestoneError = MutableStateFlow<String?>(null)
    val milestoneError: StateFlow<String?> = _milestoneError

    fun fetchAndStoreMilestones(bankName: String, cardName: String) {
        if (_isFetchingMilestones.value) return
        viewModelScope.launch {
            _isFetchingMilestones.value = true
            _milestoneError.value = null
            try {
                val rawJson = geminiService.getMilestonesForCard(cardName, bankName)
                val rules = parseMilestonesJson(rawJson, cardId)
                if (rules.isNotEmpty()) {
                    // Clear old rules for this card and insert new
                    repository.replaceSpendRules(cardId, rules)
                    Log.d("MilestoneViewModel", "Stored ${rules.size} milestones for cardId=$cardId")
                } else {
                    _milestoneError.value = "No milestones found for this card"
                }
            } catch (e: Exception) {
                Log.e("MilestoneViewModel", "Milestone fetch failed: ${e.message}")
                _milestoneError.value = "Failed to load milestones: ${e.message}"
            } finally {
                _isFetchingMilestones.value = false
            }
        }
    }

    private fun parseMilestonesJson(json: String, cardId: Int): List<SpendRule> {
        val rules = mutableListOf<SpendRule>()
        try {
            // Try to extract JSON array from response (Gemini may wrap in markdown)
            val jsonStr = json.substringAfter("[").let { "[$it" }.substringBefore("]").let { "$it]" }
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj: JSONObject = arr.getJSONObject(i)
                rules.add(
                    SpendRule(
                        cardId = cardId,
                        ruleName = obj.optString("ruleName", "Milestone ${i + 1}"),
                        targetAmount = obj.optDouble("targetAmount", 0.0),
                        currentAmount = 0.0,
                        reward = obj.optString("reward", ""),
                        rewardType = obj.optString("rewardType", "points"),
                        cycleType = obj.optString("cycleType", "monthly"),
                        isAchieved = false,
                        resetDay = obj.optInt("resetDay", 1)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("MilestoneViewModel", "JSON parse error: ${e.message}\nRaw: $json")
        }
        return rules
    }

    companion object {
        fun factory(context: Context, cardId: Int) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MilestoneViewModel(context, cardId) as T
        }
    }
}
