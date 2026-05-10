package com.cardpulse.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardpulse.app.model.Card
import com.cardpulse.app.ui.theme.*
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    onSave: (Card) -> Unit,
    onBack: () -> Unit
) {
    var bankName       by remember { mutableStateOf("") }
    var cardName       by remember { mutableStateOf("") }
    var last4          by remember { mutableStateOf("") }
    var cardType       by remember { mutableStateOf("VISA") }
    var creditLimit    by remember { mutableStateOf("") }
    var billingCycleDay by remember { mutableStateOf("1") }
    var annualFee      by remember { mutableStateOf("") }
    var selectedColor  by remember { mutableStateOf("#1A73E8") }
    var showCardTypeMenu by remember { mutableStateOf(false) }
    var errorText      by remember { mutableStateOf("") }

    val cardTypes = listOf("VISA", "MASTERCARD", "RUPAY", "AMEX")
    val presetColors = listOf(
        "#1A73E8", "#7B1FA2", "#00897B",
        "#E53935", "#F57C00", "#37474F"
    )

    Scaffold(
        containerColor = PulseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Add New Card", color = PulseOnSurface, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = PulseOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PulseBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            PulseTextField(value = bankName, label = "Bank Name (e.g. HDFC, Axis)",
                onValueChange = { bankName = it })
            PulseTextField(value = cardName, label = "Card Name (e.g. Infinia, Magnus)",
                onValueChange = { cardName = it })
            PulseTextField(value = last4, label = "Last 4 Digits",
                onValueChange = { if (it.length <= 4) last4 = it },
                keyboardType = KeyboardType.Number)
            PulseTextField(value = creditLimit, label = "Credit Limit (₹)",
                onValueChange = { creditLimit = it }, keyboardType = KeyboardType.Number)
            PulseTextField(value = billingCycleDay, label = "Billing Cycle Day (1-28)",
                onValueChange = { billingCycleDay = it }, keyboardType = KeyboardType.Number)
            PulseTextField(value = annualFee, label = "Annual Fee (₹)",
                onValueChange = { annualFee = it }, keyboardType = KeyboardType.Number)

            // Card type dropdown
            Text("Card Type", fontSize = 13.sp, color = PulseSubtext)
            Box {
                OutlinedButton(
                    onClick = { showCardTypeMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PulseOnSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PulseSubtext.copy(alpha = 0.4f))
                ) {
                    Text(cardType, color = PulseOnSurface)
                }
                DropdownMenu(
                    expanded = showCardTypeMenu,
                    onDismissRequest = { showCardTypeMenu = false },
                    modifier = Modifier.background(PulseCard)
                ) {
                    cardTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, color = PulseOnSurface) },
                            onClick = { cardType = type; showCardTypeMenu = false }
                        )
                    }
                }
            }

            // Color picker
            Text("Card Color", fontSize = 13.sp, color = PulseSubtext)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                presetColors.forEach { hex ->
                    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { PulseBlue }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selectedColor == hex) 3.dp else 0.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex }
                    )
                }
            }

            if (errorText.isNotEmpty()) {
                Text(errorText, color = PulseDanger, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    when {
                        bankName.isBlank() -> errorText = "Bank name is required"
                        cardName.isBlank() -> errorText = "Card name is required"
                        last4.length != 4  -> errorText = "Enter exactly 4 digits"
                        creditLimit.toDoubleOrNull() == null -> errorText = "Enter a valid credit limit"
                        billingCycleDay.toIntOrNull()?.let { it < 1 || it > 28 } != false -> errorText = "Billing day must be 1-28"
                        annualFee.toDoubleOrNull() == null -> errorText = "Enter a valid annual fee"
                        else -> {
                            errorText = ""
                            onSave(
                                Card(
                                    bankName        = bankName.trim(),
                                    cardName        = cardName.trim(),
                                    last4Digits     = last4.trim(),
                                    cardType        = cardType,
                                    cardNetwork     = "$bankName $cardName",
                                    creditLimit     = creditLimit.toDouble(),
                                    billingCycleDay = billingCycleDay.toInt(),
                                    statementDay    = billingCycleDay.toInt(),
                                    dueDateOffset   = 20,
                                    annualFee       = annualFee.toDouble(),
                                    addedOn         = Date(),
                                    color           = selectedColor
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PulseBlue)
            ) {
                Text("Save Card", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PulseTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, color = PulseSubtext, fontSize = 13.sp) },
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedTextColor    = PulseOnSurface,
            unfocusedTextColor  = PulseOnSurface,
            focusedBorderColor  = PulseBlue,
            unfocusedBorderColor = PulseSubtext.copy(alpha = 0.4f),
            cursorColor         = PulseBlue
        ),
        singleLine = true
    )
}
