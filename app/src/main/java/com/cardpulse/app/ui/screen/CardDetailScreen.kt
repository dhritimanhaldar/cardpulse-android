package com.cardpulse.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.parser.LedgerTransactionKind
import com.cardpulse.app.parser.TransactionKindClassifier
import com.cardpulse.app.parser.TransactionTagger
import com.cardpulse.app.viewmodel.CardDetailViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    navController: NavController,
    cardId: Long
) {
    val vm: CardDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = CardDetailViewModel.factory(navController.context, cardId.toInt())
    )

    val card by vm.card.collectAsState()
    val cardDetail by vm.cardDetail.collectAsState()
    val perkProgress by vm.perkProgressList.collectAsState()
    val milestoneProgress by vm.milestoneProgressList.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val isLoadingMilestones by vm.isLoadingMilestones.collectAsState()
    val loungeAccess = cardDetail?.loungeAccess
    var showAllTransactions by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(cardId) {
        vm.loadCard()
    }

    val currentCard = card
    if (currentCard == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentCard.cardName.ifBlank { "Card Details" }) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("add_card/${currentCard.id}") }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Card",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = safeCardColor(currentCard.color)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = safeCardColor(currentCard.color)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = currentCard.bankName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "•••• ${currentCard.last4Digits}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "CARD NAME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = currentCard.cardName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = "OUTSTANDING",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "₹${currentCard.currentOutstanding}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            if (currentCard.isAutoFetched && !currentCard.isVerified) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        text = "Auto-detected card - Please verify details",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Edit to confirm the bank, card variant, and last four digits.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            Button(onClick = { navController.navigate("add_card/${currentCard.id}") }) {
                                Text("Edit")
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showAllTransactions) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { showAllTransactions = false }
                        ) {
                            Text("Milestone")
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showAllTransactions = true }
                        ) {
                            Text("All Transactions")
                        }
                    } else {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { showAllTransactions = false }
                        ) {
                            Text("Milestone")
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { showAllTransactions = true }
                        ) {
                            Text("All Transactions")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            if (showAllTransactions) {
                item {
                    Text(
                        text = "All Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (transactions.isEmpty()) {
                    item {
                        Text(
                            text = "No transactions found for this card.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(transactions) { transaction ->
                        TransactionRow(transaction, onClick = { selectedTransaction = transaction })
                    }
                }
                return@LazyColumn
            }

            item {
                Text(
                    text = "Milestone",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (isLoadingMilestones) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading rewards...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!isLoadingMilestones && milestoneProgress.isNotEmpty()) {
                items(milestoneProgress) { progress ->
                    ExpandableMilestoneCard(
                        title = progress.milestone.n,
                        subtitle = progress.milestone.rw,
                        cycle = progress.milestone.cy,
                        rewardType = progress.milestone.rt,
                        currentAmount = progress.currentAmount,
                        targetAmount = progress.targetAmount,
                        isAchieved = progress.isAchieved,
                        transactions = progress.qualifyingTxns,
                        onTransactionClick = { selectedTransaction = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (!isLoadingMilestones && perkProgress.isNotEmpty()) {
                item {
                    Text(
                        text = "Rewards",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                }

                items(perkProgress) { progress ->
                    ExpandableMilestoneCard(
                        title = progress.perk.n,
                        subtitle = rewardSummary(progress.perk.rt, progress.perk.up?.v, progress.perk.vp),
                        cycle = progress.perk.cy,
                        rewardType = progress.perk.rt,
                        currentAmount = progress.currentAmount,
                        targetAmount = progress.targetAmount,
                        isAchieved = progress.isAchieved,
                        transactions = progress.qualifyingTxns,
                        onTransactionClick = { selectedTransaction = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (!isLoadingMilestones && milestoneProgress.isEmpty() && perkProgress.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "No spend milestones found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (transactions.isEmpty()) {
                                    "Transactions will appear here once spend is detected for this card."
                                } else {
                                    "This card does not have catalog milestones yet."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            loungeAccess?.let { lounge ->
                item {
                    Text(
                        text = "Lounge Access",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = lounge.loungeNetwork,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (lounge.totalVisitsAllowed <= 0) {
                                        0f
                                    } else {
                                        (lounge.visitsUsed.toFloat() / lounge.totalVisitsAllowed).coerceIn(0f, 1f)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${lounge.visitsRemaining} of ${lounge.totalVisitsAllowed} visits remaining • ${lounge.resetPeriod}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    selectedTransaction?.let { txn ->
        TransactionTaggingDialog(
            transaction = txn,
            milestoneRows = milestoneProgress
                .filter { it.qualifyingTxns.any { qualifying -> qualifying.id == txn.id } }
                .map { it.milestone.n to it.progress },
            rewardRows = perkProgress
                .filter { it.qualifyingTxns.any { qualifying -> qualifying.id == txn.id } }
                .map { it.perk.n to it.progress },
            onDismiss = { selectedTransaction = null },
            onSave = { kind, tags, confidence ->
                vm.updateTransactionTagging(txn.id, kind.name, tags, confidence)
                selectedTransaction = null
            }
        )
    }
}

@Composable
fun ExpandableMilestoneCard(
    title: String,
    subtitle: String?,
    cycle: String?,
    rewardType: String? = null,
    currentAmount: Double,
    targetAmount: Double,
    isAchieved: Boolean,
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit = {}
) {
    val progress = if (targetAmount <= 0.0) 0f else (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = listOfNotNull(
                            cycleLabel(cycle),
                            timeRemainingLabel(cycle),
                            subtitle?.takeIf { it.isNotBlank() }
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = progressText(currentAmount, targetAmount, rewardType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isAchieved) "Achieved" else "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAchieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (transactions.isEmpty()) {
                        Text(
                            text = "No qualifying transactions yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        transactions
                            .groupBy { it.category.ifBlank { "Other" } }
                            .forEach { (category, categoryTxns) ->
                                TransactionGroup(
                                    category = category,
                                    transactions = categoryTxns,
                                    onTransactionClick = onTransactionClick
                                )
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionGroup(
    category: String,
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit = {}
) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = formatCurrency(
                    transactions.sumOf { TransactionKindClassifier.signedProgressAmount(it) }.coerceAtLeast(0.0)
                ),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        transactions.forEach { transaction ->
            TransactionRow(transaction, onClick = { onTransactionClick(transaction) })
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit = {}
) {
    val amountColor = if (TransactionKindClassifier.countsTowardSpend(transaction)) {
        MaterialTheme.colorScheme.error
    } else {
        Color(0xFF2E7D32)
    }
    val prefix = if (TransactionKindClassifier.countsTowardSpend(transaction)) "-" else "+"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(transaction.merchant, style = MaterialTheme.typography.bodyMedium)
            Text(
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(transaction.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "$prefix${formatCurrency(transaction.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = amountColor
        )
    }
}

@Composable
fun TransactionTaggingDialog(
    transaction: Transaction,
    milestoneRows: List<Pair<String, Float>>,
    rewardRows: List<Pair<String, Float>>,
    onDismiss: () -> Unit,
    onSave: (LedgerTransactionKind, Set<String>, Double) -> Unit
) {
    var selectedKind by remember(transaction.id) {
        mutableStateOf(TransactionKindClassifier.kindOf(transaction))
    }
    var selectedTags by remember(transaction.id) {
        mutableStateOf(TransactionTagger.tagsOf(transaction))
    }
    var confidence by remember(transaction.id) {
        mutableStateOf(transaction.tagConfidence.coerceIn(0.0, 1.0))
    }
    val commonTags = listOf(
        "fuel", "offline", "online", "travel", "dining", "grocery", "utilities",
        "insurance", "rent", "emi", "wallet_load", "education", "healthcare"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(transaction.merchant) },
        text = {
            Column {
                Text(
                    text = "Confidence ${(confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Transaction type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        LedgerTransactionKind.SPEND,
                        LedgerTransactionKind.PAYMENT,
                        LedgerTransactionKind.REFUND,
                        LedgerTransactionKind.FEE
                    ).forEach { kind ->
                        if (selectedKind == kind) {
                            Button(onClick = { selectedKind = kind }) { Text(kind.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        } else {
                            OutlinedButton(onClick = { selectedKind = kind }) { Text(kind.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Milestone mapping", style = MaterialTheme.typography.labelLarge)
                commonTags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                                confidence = 1.0
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tag in selectedTags,
                            onCheckedChange = {
                                selectedTags = if (it) selectedTags + tag else selectedTags - tag
                                confidence = 1.0
                            }
                        )
                        Text(tag.replace('_', ' '))
                    }
                }
                val rows = milestoneRows + rewardRows
                if (rows.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Contribution preview", style = MaterialTheme.typography.labelLarge)
                    rows.take(4).forEach { (label, progress) ->
                        Text(label, style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedKind, selectedTags, confidence) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun cycleLabel(cycle: String?): String? {
    return when (cycle) {
        "m" -> "Monthly"
        "q" -> "Quarterly"
        "a" -> "Annual"
        "o" -> "Lifetime"
        null, "" -> null
        else -> "Spend"
    }
}

private fun timeRemainingLabel(cycle: String?): String? {
    val now = Calendar.getInstance()
    val end = Calendar.getInstance()
    when (cycle) {
        "m" -> end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        "q" -> {
            val currentMonth = end.get(Calendar.MONTH)
            val quarterEndMonth = (currentMonth / 3) * 3 + 2
            end.set(Calendar.MONTH, quarterEndMonth)
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        "a" -> {
            end.set(Calendar.MONTH, Calendar.DECEMBER)
            end.set(Calendar.DAY_OF_MONTH, 31)
        }
        "o" -> return null
        else -> return null
    }
    end.set(Calendar.HOUR_OF_DAY, 23)
    end.set(Calendar.MINUTE, 59)
    end.set(Calendar.SECOND, 59)

    val daysLeft = ((end.timeInMillis - now.timeInMillis) / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
    return when {
        daysLeft >= 60 -> "${daysLeft / 30} months left"
        daysLeft == 1L -> "1 day left"
        else -> "$daysLeft days left"
    }
}

private fun rewardSummary(rewardType: String?, limit: Int?, visits: Int?): String {
    return when (rewardType) {
        "p" -> limit?.let { "Up to ${formatCurrency(it.toDouble())}" } ?: "Points"
        "rw" -> limit?.let { "Up to ${formatCurrency(it.toDouble())}" } ?: "Rewards"
        "c" -> limit?.let { "Up to ${formatCurrency(it.toDouble())}" } ?: "Cashback"
        "l" -> visits?.let { "Up to $it visits" } ?: "Lounge access"
        "v" -> limit?.let { "Up to ${formatCurrency(it.toDouble())}" } ?: "Voucher"
        "i" -> "Insurance benefits"
        "mi" -> "Miles"
        else -> "Rewards"
    }
}

private fun progressText(currentAmount: Double, targetAmount: Double, rewardType: String?): String {
    if (targetAmount <= 0.0) {
        return when (rewardType) {
            "l" -> "Usage tracked"
            else -> formatCurrency(currentAmount)
        }
    }
    return "${formatCurrency(currentAmount)} / ${formatCurrency(targetAmount)}"
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
}

private fun safeCardColor(color: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(color)) }
        .getOrElse { Color(0xFF1A73E8) }
}
