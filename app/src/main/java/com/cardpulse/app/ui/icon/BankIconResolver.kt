package com.cardpulse.app.ui.icon

import androidx.annotation.DrawableRes
import com.cardpulse.app.R
import com.cardpulse.app.util.cleanAndNormalizeBankName

data class BankIconInfo(
    val key: String,
    @DrawableRes val drawableRes: Int
)

object BankIconResolver {
    private data class Mapping(
        val key: String,
        @DrawableRes val drawableRes: Int,
        val aliases: Set<String>
    )

    private val mappings = listOf(
        Mapping(
            key = "hdfc",
            drawableRes = R.drawable.bank_hdfc,
            aliases = setOf("hdfc", "hdfc bank")
        ),
        Mapping(
            key = "icici",
            drawableRes = R.drawable.bank_icici,
            aliases = setOf("icici", "icici bank")
        ),
        Mapping(
            key = "axis",
            drawableRes = R.drawable.bank_axis,
            aliases = setOf("axis", "axis bank")
        ),
        Mapping(
            key = "sbi_card",
            drawableRes = R.drawable.bank_sbi_card,
            aliases = setOf("sbi", "sbi card", "state bank of india")
        ),
        Mapping(
            key = "hsbc",
            drawableRes = R.drawable.bank_hsbc,
            aliases = setOf("hsbc", "hsbc india")
        ),
        Mapping(
            key = "amex",
            drawableRes = R.drawable.bank_amex,
            aliases = setOf("amex", "american express")
        ),
        Mapping(
            key = "yes_bank",
            drawableRes = R.drawable.bank_yes_bank,
            aliases = setOf("yes bank", "yes")
        )
    )

    fun resolve(bankCodeOrName: String?): BankIconInfo? {
        val normalized = normalize(bankCodeOrName)
        if (normalized.isBlank()) return null

        return mappings.firstOrNull { mapping ->
            mapping.aliases.any { alias ->
                normalized == alias || normalized.contains(alias) || alias.contains(normalized)
            }
        }?.let { BankIconInfo(it.key, it.drawableRes) }
    }

    private fun normalize(value: String?): String {
        return cleanAndNormalizeBankName(value.orEmpty())
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }
}
