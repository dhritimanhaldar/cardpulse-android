package com.cardpulse.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cardpulse.app.data.BankOption
import com.cardpulse.app.data.CardVariantOption
import com.cardpulse.app.model.CardMatchState
import com.cardpulse.app.ui.icon.BankIconResolver
import com.cardpulse.app.viewmodel.AddCardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    navController: NavController,
    editCardId: Long? = null
) {
    val context = LocalContext.current
    val viewModel: AddCardViewModel = viewModel()
    val focusRequesters = remember { List(4) { FocusRequester() } }

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    LaunchedEffect(editCardId) {
        editCardId?.let { cardId ->
            viewModel.loadCardForEdit(context, cardId)
        }
    }

    val helperText = when (val state = viewModel.matchState.value) {
        CardMatchState.Empty -> if (editCardId != null) {
            "Update the first digits to re-verify this card, or keep the saved card selection."
        } else {
            "Enter at least the first 6 digits to detect the bank and card variant."
        }
        CardMatchState.NotEnoughDigits -> "Enter at least the first 6 digits to start matching."
        CardMatchState.Matching -> "Checking card_data.json for matching banks and variants..."
        is CardMatchState.Exact -> "Exact match found. Bank and variant are locked from the catalog."
        is CardMatchState.Multiple -> if (state.fixedBank != null) {
            "Bank identified. Choose the correct variant to finish verification."
        } else {
            "Multiple matches found. Choose the bank and card variant explicitly."
        }
        CardMatchState.NoMatch -> "No catalog match yet. Select the bank and card variant manually."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editCardId != null) "Edit Card" else "Add Card") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Card Digits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                DigitGroupsRow(
                    groups = viewModel.digitGroups,
                    lastGroupEditable = viewModel.lastGroupEditable.value,
                    focusRequesters = focusRequesters,
                    onGroupChange = { index, value ->
                        viewModel.onDigitGroupChanged(index, value)
                        val digitsOnly = value.filter(Char::isDigit)
                        if (digitsOnly.length == 4 && index < focusRequesters.lastIndex) {
                            if (index + 1 != 3 || viewModel.lastGroupEditable.value) {
                                focusRequesters[index + 1].requestFocus()
                            }
                        }
                    },
                    onPaste = { index, value ->
                        viewModel.onDigitsPasted(value, index)
                    }
                )
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            BankSection(viewModel = viewModel)
            VariantSection(viewModel = viewModel)

            OutlinedTextField(
                value = viewModel.cardNickname.value,
                onValueChange = viewModel::onNicknameChanged,
                label = { Text("Nickname") },
                placeholder = { Text("Travel Card") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.cardHolderName.value,
                onValueChange = { viewModel.cardHolderName.value = it },
                label = { Text("Cardholder Name") },
                placeholder = { Text("Optional") },
                modifier = Modifier.fillMaxWidth()
            )

            ColorSection(
                selectedColor = viewModel.selectedColor,
                onColorSelected = { viewModel.selectedColor = it }
            )

            viewModel.errorMessage.value?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { viewModel.saveCard(context, navController) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isSaving.value && viewModel.canSave()
            ) {
                Text(
                    when {
                        viewModel.isSaving.value -> "Saving..."
                        editCardId != null -> "Save Changes"
                        else -> "Save Card"
                    }
                )
            }
        }
    }
}

@Composable
private fun DigitGroupsRow(
    groups: List<String>,
    lastGroupEditable: Boolean,
    focusRequesters: List<FocusRequester>,
    onGroupChange: (Int, String) -> Unit,
    onPaste: (Int, String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEachIndexed { index, value ->
            val editable = index != 3 || lastGroupEditable
            OutlinedTextField(
                value = value,
                onValueChange = { updated ->
                    if (updated.filter(Char::isDigit).length > 4) {
                        onPaste(index, updated)
                    } else {
                        onGroupChange(index, updated)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequesters[index]),
                enabled = editable,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                label = { Text(if (index == 3) "Last 4" else "XXXX") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (index == 3 || (!lastGroupEditable && index == 2)) {
                        ImeAction.Done
                    } else {
                        ImeAction.Next
                    }
                ),
                trailingIcon = if (!editable) {
                    {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Saved last 4 digits",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun BankSection(viewModel: AddCardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Bank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        when (val state = viewModel.matchState.value) {
            is CardMatchState.Exact -> {
                val bank = viewModel.matchedBank.value
                BankDisplayRow(
                    bankName = bank?.name.orEmpty(),
                    readOnly = true
                )
            }
            is CardMatchState.Multiple -> {
                if (state.fixedBank != null) {
                    BankDisplayRow(
                        bankName = state.fixedBank,
                        readOnly = true
                    )
                } else {
                    BankDropdownField(
                        label = "Select Bank",
                        selected = viewModel.selectedBank.value?.name,
                        options = viewModel.candidateBanks.value,
                        expanded = viewModel.showBankDropdown.value,
                        onExpandedChange = { viewModel.showBankDropdown.value = it },
                        onSelected = viewModel::onCandidateBankSelected
                    )
                }
            }
            CardMatchState.NoMatch -> {
                BankDropdownField(
                    label = "Select Bank",
                    selected = viewModel.selectedBank.value?.name,
                    options = viewModel.availableBanks.value,
                    expanded = viewModel.showBankDropdown.value,
                    onExpandedChange = { viewModel.showBankDropdown.value = it },
                    onSelected = viewModel::onFallbackBankSelected
                )
            }
            else -> {
                val bank = viewModel.matchedBank.value ?: viewModel.selectedBank.value
                BankDisplayRow(
                    bankName = bank?.name ?: "Waiting for card digits",
                    readOnly = true,
                    isPlaceholder = bank == null
                )
            }
        }
    }
}

@Composable
private fun VariantSection(viewModel: AddCardViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Card Variant",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        when (viewModel.matchState.value) {
            is CardMatchState.Exact -> {
                VariantReadOnlyField(
                    value = viewModel.selectedCardVariant.value?.displayName ?: "Variant locked"
                )
            }
            is CardMatchState.Multiple -> {
                VariantDropdownField(
                    label = "Choose Variant",
                    selected = viewModel.selectedCardVariant.value?.displayName,
                    options = viewModel.candidateVariants.value,
                    expanded = viewModel.showCardVariantDropdown.value,
                    enabled = viewModel.selectedBank.value != null,
                    onExpandedChange = { viewModel.showCardVariantDropdown.value = it },
                    onSelected = viewModel::onResolvedVariantSelected
                )
            }
            CardMatchState.NoMatch -> {
                VariantDropdownField(
                    label = "Choose Variant",
                    selected = viewModel.selectedCardVariant.value?.displayName,
                    options = viewModel.availableCardVariants.value,
                    expanded = viewModel.showCardVariantDropdown.value,
                    enabled = viewModel.selectedBank.value != null,
                    onExpandedChange = { viewModel.showCardVariantDropdown.value = it },
                    onSelected = viewModel::onFallbackVariantSelected
                )
            }
            else -> {
                VariantReadOnlyField(
                    value = viewModel.selectedCardVariant.value?.displayName ?: "Waiting for card detection",
                    isPlaceholder = viewModel.selectedCardVariant.value == null
                )
            }
        }
    }
}

@Composable
private fun BankDisplayRow(
    bankName: String,
    readOnly: Boolean,
    isPlaceholder: Boolean = false
) {
    val iconInfo = BankIconResolver.resolve(bankName)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BankIcon(iconInfo = iconInfo)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bankName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isPlaceholder) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (readOnly && !isPlaceholder) {
                    Text(
                        text = "Detected from entered digits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (readOnly && !isPlaceholder) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked bank",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankDropdownField(
    label: String,
    selected: String?,
    options: List<BankOption>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (BankOption) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) }
    ) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            placeholder = { Text(label) },
            readOnly = true,
            leadingIcon = {
                BankIcon(iconInfo = selected?.let(BankIconResolver::resolve))
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            options.forEach { bank ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BankIcon(iconInfo = BankIconResolver.resolve(bank.name))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(bank.name)
                        }
                    },
                    onClick = { onSelected(bank) }
                )
            }
        }
    }
}

@Composable
private fun VariantReadOnlyField(
    value: String,
    isPlaceholder: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Card Variant") },
        readOnly = true,
        trailingIcon = {
            if (!isPlaceholder) {
                Icon(Icons.Default.Lock, contentDescription = "Locked variant")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariantDropdownField(
    label: String,
    selected: String?,
    options: List<CardVariantOption>,
    expanded: Boolean,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (CardVariantOption) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = {
            if (enabled) {
                onExpandedChange(!expanded)
            }
        }
    ) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            placeholder = { Text(label) },
            enabled = enabled,
            readOnly = true,
            trailingIcon = {
                if (enabled) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            options.forEach { variant ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(variant.displayName)
                            variant.groupName?.takeIf { it.isNotBlank() }?.let { group ->
                                Text(
                                    text = group,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = { onSelected(variant) }
                )
            }
        }
    }
}

@Composable
private fun ColorSection(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Color",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Color(0xFF1976D2),
                Color(0xFFD32F2F),
                Color(0xFF388E3C),
                Color(0xFFF57C00),
                Color(0xFF7B1FA2),
                Color(0xFF303F9F)
            ).forEach { color ->
                Card(
                    modifier = Modifier
                        .size(48.dp)
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = color),
                    onClick = { onColorSelected(color) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == color) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .padding(2.dp)
                            ) {
                                Text("✓", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BankIcon(
    iconInfo: com.cardpulse.app.ui.icon.BankIconInfo?,
    modifier: Modifier = Modifier
) {
    if (iconInfo != null) {
        Image(
            painter = painterResource(id = iconInfo.drawableRes),
            contentDescription = null,
            modifier = modifier
                .size(36.dp)
                .padding(2.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier
                .size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
