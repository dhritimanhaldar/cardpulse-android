package com.cardpulse.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cardpulse.app.model.Transaction
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

    val cardDetail by vm.cardDetail.collectAsState()
    val perkProgress by vm.perkProgressList.collectAsState()
    val milestoneProgress by vm.milestoneProgressList.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val loungeAccess = cardDetail?.loungeAccess

    LaunchedEffect(cardId) {
        vm.loadCard()
    }

    val card = cardDetail?.card ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card.cardName.ifBlank { "Card Details" }) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("add_card/${card.id}") }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Card",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(android.graphics.Color.parseColor(card.color))
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(android.graphics.Color.parseColor(card.color))
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = card.bankName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "•••• ${card.last4Digits}",
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
                                    text = card.cardName,
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
                                    text = "₹${card.currentOutstanding}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                Text(
                    text = "Spend Progress",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (milestoneProgress.isNotEmpty()) {
                items(milestoneProgress) { progress ->
                    ExpandableMilestoneCard(
                        title = progress.milestone.n,
                        subtitle = progress.milestone.rw,
                        cycle = progress.milestone.cy,
                        currentAmount = progress.currentAmount,
                        targetAmount = progress.milestone.ta.toDouble(),
                        isAchieved = progress.isAchieved,
                        transactions = progress.qualifyingTxns
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (perkProgress.isNotEmpty()) {
                item {
                    Text(
                        text = "Perk Progress",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                }

                items(perkProgress) { progress ->
                    ExpandableMilestoneCard(
                        title = progress.perk.n,
                        subtitle = progress.perk.rt,
                        cycle = progress.perk.cy,
                        currentAmount = progress.currentAmount,
                        targetAmount = progress.perk.up?.v?.toDouble() ?: 0.0,
                        isAchieved = progress.isAchieved,
                        transactions = progress.qualifyingTxns
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (milestoneProgress.isEmpty() && perkProgress.isEmpty()) {
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
}

@Composable
fun ExpandableMilestoneCard(
    title: String,
    subtitle: String,
    cycle: String,
    currentAmount: Double,
    targetAmount: Double,
    isAchieved: Boolean,
    transactions: List<Transaction>
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
                        text = "${cycleLabel(cycle)} • ${timeRemainingLabel(cycle)} • $subtitle",
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
                    text = "${formatCurrency(currentAmount)} / ${formatCurrency(targetAmount)}",
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
                    Text(
                        text = "Transactions counting toward this",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                                    transactions = categoryTxns
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
    transactions: List<Transaction>
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
                text = formatCurrency(transactions.sumOf { it.amount }),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        transactions.forEach { transaction ->
            TransactionRow(transaction)
        }
    }
}

@Composable
fun TransactionRow(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            formatCurrency(transaction.amount),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun cycleLabel(cycle: String): String {
    return when (cycle) {
        "m" -> "Monthly"
        "q" -> "Quarterly"
        "a" -> "Annual"
        "o" -> "Lifetime"
        else -> "Spend"
    }
}

private fun timeRemainingLabel(cycle: String): String {
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
        else -> return "No expiry"
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

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
}
