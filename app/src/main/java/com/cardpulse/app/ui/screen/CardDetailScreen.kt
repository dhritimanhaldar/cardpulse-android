package com.cardpulse.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardpulse.app.model.SpendRule
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionStatus
import com.cardpulse.app.ui.theme.*
import com.cardpulse.app.viewmodel.CardDetailViewModel
import com.cardpulse.app.viewmodel.MilestoneViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Int,
    onBack: () -> Unit,
    onDeleted: () -> Unit = onBack
) {
    val context = LocalContext.current
    val viewModel: CardDetailViewModel = viewModel(
        factory = CardDetailViewModel.factory(context, cardId)
    )
    val milestoneViewModel: MilestoneViewModel = viewModel(
        factory = MilestoneViewModel.factory(context, cardId)
    )
    val cardDetail by viewModel.cardDetail.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isFetchingMilestones by milestoneViewModel.isFetchingMilestones.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val card = cardDetail?.card
    val spendRules = cardDetail?.spendRules ?: emptyList()
    val totalSpent = cardDetail?.totalSpentThisCycle ?: 0.0
    val loungeAccess = cardDetail?.loungeAccess

    val cardColor = remember(card?.color) {
        card?.color?.let {
            try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { PulseAccent }
        } ?: PulseAccent
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Card") },
            text = { Text("This will permanently delete the card and all its transactions. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCard { onDeleted() }
                    }
                ) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = PulseBackground,
        topBar = {
            TopAppBar(
                title = { Text(card?.cardName ?: "Card Detail", color = PulseOnSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PulseOnSurface)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            card?.let {
                                milestoneViewModel.fetchAndStoreMilestones(it.bankName, it.cardName)
                            }
                        },
                        enabled = !isFetchingMilestones
                    ) {
                        if (isFetchingMilestones) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PulseAccent, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Milestones",
                                tint = PulseAccent
                            )
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = Color(0xFFFF6B6B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PulseBackground)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PulseAccent)
            }
            return@Scaffold
        }

        if (card == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Card not found", color = PulseSubtext)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp)
        ) {
            // ── Card Visual Header ─────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(cardColor, cardColor.copy(alpha = 0.6f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = card.bankName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = card.cardType,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = card.cardNetwork,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "•••• •••• •••• ${card.last4Digits}",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = card.cardName,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Outstanding / Spend Summary ────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryChip(
                        label = "Outstanding",
                        value = "₹%.0f".format(card.currentOutstanding),
                        modifier = Modifier.weight(1f),
                        highlight = card.currentOutstanding > 0,
                        cardColor = cardColor
                    )
                    SummaryChip(
                        label = "Spent This Cycle",
                        value = "₹%.0f".format(totalSpent),
                        modifier = Modifier.weight(1f),
                        cardColor = cardColor
                    )
                    SummaryChip(
                        label = "Credit Limit",
                        value = "₹%.0f".format(card.creditLimit),
                        modifier = Modifier.weight(1f),
                        cardColor = cardColor
                    )
                }
            }

            // ── Due Date ───────────────────────────────────────
            card.paymentDueDate?.let { dueDate ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PulseCard
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Payment Due: ", color = PulseSubtext, fontSize = 13.sp)
                            Text(dueDate, color = Color(0xFFFF6B6B), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Lounge Access ──────────────────────────────────
            loungeAccess?.let { lounge ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PulseCard
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Lounge Access", color = PulseOnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (lounge.visitsUsed.toFloat() / lounge.totalVisitsAllowed.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = cardColor,
                                trackColor = PulseSubtext.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${lounge.visitsUsed} / ${lounge.totalVisitsAllowed} visits used (${lounge.loungeNetwork})",
                                color = PulseSubtext,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Milestones Header ──────────────────────────────
            item {
                Text(
                    text = "Milestones & Rewards",
                    color = PulseOnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (spendRules.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PulseCard
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No milestones loaded yet. Sync card info to load milestones.", color = PulseSubtext, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(spendRules, key = { it.id }) { rule ->
                    MilestoneCard(
                        rule = rule,
                        transactions = transactions.filter { txn ->
                            !txn.isCredit && txn.status == TransactionStatus.CONFIRMED
                        },
                        cardColor = cardColor,
                        totalSpentThisCycle = totalSpent
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    cardColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = PulseCard
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = PulseSubtext, fontSize = 11.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                color = if (highlight) Color(0xFFFF6B6B) else PulseOnSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MilestoneCard(
    rule: SpendRule,
    transactions: List<Transaction>,
    cardColor: Color,
    totalSpentThisCycle: Double
) {
    var expanded by remember { mutableStateOf(false) }

    // Transactions that count toward this milestone (spending in cycle)
    val relevantTxns = transactions.takeLast(200).filter { !it.isCredit }
    val progress = (rule.currentAmount / rule.targetAmount).toFloat().coerceIn(0f, 1f)
    val progressPct = (progress * 100).toInt()
    val remaining = (rule.targetAmount - rule.currentAmount).coerceAtLeast(0.0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PulseCard
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rule.ruleName, color = PulseOnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(rule.reward, color = cardColor, fontSize = 12.sp)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = PulseSubtext
                )
            }

            Spacer(Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (rule.isAchieved) Color(0xFF4CAF50) else cardColor,
                trackColor = PulseSubtext.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "₹%.0f / ₹%.0f".format(rule.currentAmount, rule.targetAmount),
                    color = PulseSubtext,
                    fontSize = 12.sp
                )
                Text(
                    if (rule.isAchieved) "✓ Achieved" else "$progressPct% · ₹%.0f left".format(remaining),
                    color = if (rule.isAchieved) Color(0xFF4CAF50) else PulseSubtext,
                    fontSize = 12.sp,
                    fontWeight = if (rule.isAchieved) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // Expanded transactions
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = PulseSubtext.copy(alpha = 0.1f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Transactions counting toward this milestone",
                        color = PulseSubtext,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    if (relevantTxns.isEmpty()) {
                        Text("No transactions yet", color = PulseSubtext, fontSize = 12.sp)
                    } else {
                        relevantTxns.forEach { txn ->
                            MiniTransactionRow(txn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniTransactionRow(txn: Transaction) {
    val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(txn.merchant, color = PulseOnSurface, fontSize = 13.sp, maxLines = 1)
            Text(fmt.format(txn.date), color = PulseSubtext, fontSize = 11.sp)
        }
        Text(
            "₹%.0f".format(txn.amount),
            color = if (txn.isCredit) Color(0xFF4CAF50) else Color(0xFFFF6B6B),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}
