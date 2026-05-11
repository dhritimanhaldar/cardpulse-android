package com.cardpulse.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val viewModel: AddCardViewModel = viewModel()
    val context = LocalContext.current

    // Initialize repository
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    // Load card if editing
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Card Number
            OutlinedTextField(
                value = viewModel.cardNumber.value,
                onValueChange = { viewModel.cardNumber.value = it },
                label = { Text("Card Number") },
                placeholder = { Text("1234 5678 9012 3456") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card Holder Name
            OutlinedTextField(
                value = viewModel.cardHolderName.value,
                onValueChange = { viewModel.cardHolderName.value = it },
                label = { Text("Card Holder Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bank Selection Dropdown
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

            // Card Variant Dropdown (only show if bank selected)
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

            // Card Nickname (optional)
            OutlinedTextField(
                value = viewModel.cardNickname.value,
                onValueChange = { viewModel.cardNickname.value = it },
                label = { Text("Card Nickname (Optional)") },
                placeholder = { Text("My Travel Card") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expiry Date
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = viewModel.expiryMonth.value,
                    onValueChange = { viewModel.expiryMonth.value = it },
                    label = { Text("Month") },
                    placeholder = { Text("MM") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = viewModel.expiryYear.value,
                    onValueChange = { viewModel.expiryYear.value = it },
                    label = { Text("Year") },
                    placeholder = { Text("YY") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Color Picker (simplified)
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
                    Color(0xFF1976D2), // Blue
                    Color(0xFFD32F2F), // Red
                    Color(0xFF388E3C), // Green
                    Color(0xFFF57C00), // Orange
                    Color(0xFF7B1FA2), // Purple
                    Color(0xFF303F9F)  // Indigo
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

            // Save Button
            Button(
                onClick = { viewModel.saveCard(context, navController) },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.cardNumber.value.isNotBlank() &&
                        viewModel.cardHolderName.value.isNotBlank() &&
                        viewModel.selectedBank.value != null
            ) {
                Text(if (editCardId != null) "Update Card" else "Add Card")
            }
        }
    }
}