package com.cardpulse.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cardpulse.app.viewmodel.AddCardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    navController: NavController,
    editCardId: Long? = null
) {
    val context = LocalContext.current
    val viewModel: AddCardViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    LaunchedEffect(editCardId) {
        editCardId?.let { cardId ->
            viewModel.loadCardForEdit(context, cardId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editCardId != null) "Edit Card" else "Add New Card") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = viewModel.cardName.value,
                onValueChange = { viewModel.cardName.value = it },
                label = { Text("Card Name") },
                placeholder = { Text("Infinia Metal") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.last4Digits.value,
                onValueChange = { viewModel.last4Digits.value = it },
                label = { Text("Last 4 Digits") },
                placeholder = { Text("1234") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Bank",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            ExposedDropdownMenuBox(
                expanded = viewModel.showBankDropdown.value,
                onExpandedChange = { viewModel.showBankDropdown.value = it }
            ) {
                OutlinedTextField(
                    value = viewModel.selectedBank.value?.name ?: "Select Bank",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = viewModel.showBankDropdown.value
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = viewModel.showBankDropdown.value,
                    onDismissRequest = { viewModel.showBankDropdown.value = false }
                ) {
                    viewModel.availableBanks.value.forEach { bank ->
                        DropdownMenuItem(
                            text = { Text(bank.name) },
                            onClick = { viewModel.onBankSelected(bank) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (viewModel.selectedBank.value != null) {
                Text(
                    text = "Card Variant",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = viewModel.showCardVariantDropdown.value,
                    onExpandedChange = { viewModel.showCardVariantDropdown.value = it }
                ) {
                    OutlinedTextField(
                        value = viewModel.selectedCardVariant.value?.displayName ?: "Select Card",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = viewModel.showCardVariantDropdown.value
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = viewModel.showCardVariantDropdown.value,
                        onDismissRequest = { viewModel.showCardVariantDropdown.value = false }
                    ) {
                        viewModel.availableCardVariants.value.forEach { variant ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = variant.displayName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        variant.groupName?.let { groupName ->
                                            Text(
                                                text = groupName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = { viewModel.onCardVariantSelected(variant) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = viewModel.cardNickname.value,
                onValueChange = { viewModel.cardNickname.value = it },
                label = { Text("Card Nickname (Optional)") },
                placeholder = { Text("My Travel Card") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Card Color",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val colors = listOf(
                    Color(0xFF1976D2),
                    Color(0xFFD32F2F),
                    Color(0xFF388E3C),
                    Color(0xFFF57C00),
                    Color(0xFF7B1FA2),
                    Color(0xFF303F9F)
                )

                colors.forEach { color ->
                    Card(
                        modifier = Modifier
                            .size(48.dp)
                            .weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = color
                        ),
                        onClick = { viewModel.selectedColor = color }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (viewModel.selectedColor == color) {
                                Text(
                                    "✓",
                                    color = Color.White,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            viewModel.errorMessage.value?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = { viewModel.saveCard(context, navController) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isSaving.value &&
                        viewModel.last4Digits.value.isNotBlank() &&
                        viewModel.cardName.value.isNotBlank() &&
                        viewModel.selectedBank.value != null
            ) {
                Text(
                    when {
                        viewModel.isSaving.value -> "Saving..."
                        editCardId != null -> "Update Card"
                        else -> "Add Card"
                    }
                )
            }
        }
    }
}
