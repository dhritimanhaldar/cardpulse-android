package com.cardpulse.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.model.TransactionStatus
import com.cardpulse.app.ui.theme.*
import com.cardpulse.app.viewmodel.CardDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(cardId: Int, onBack: () -> Unit, onDeleted: () -> Unit = onBack) {
    val context = LocalContext.current
    val viewModel: CardDetailViewModel = viewModel(
        factory = CardDetailViewModel.factory(context, cardId)
    )
    val cardDetail by viewModel.cardDetail.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PulseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = cardDetail?.card?.let { "${it.bankName} ${it.cardName}" } ?: "Card Detail",
                        color = PulseOnSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = PulseOnSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = Color(0xFFE53935))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PulseBackground)
            )
        }
    ) { padding ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Card") },
                text = { Text("This will delete the card and all its transactions. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCard { onDeleted() }
                    }) { Text("Delete", color = Color(0xFFE53935)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PulseBlue)
            }
            return@Scaffold
        }

        val detail = cardDetail
        if (detail == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Card not found", color = PulseSubtext)
            }
            return@Scaffold
        }

        val card = detail.card
        val cardColor = try { Color(android.graphics.Color.parseColor(card.color)) } catch (e: Exception) { PulseBlue }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Card header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PulseCard)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(cardColor, cardColor.copy(alpha = 0.55f)),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text("${card.bankName} ${card.cardName}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PulseOnSurface)
                            Text("•••• ${card.last4Digits}  ${card.cardType}", fontSize = 13.sp, color = PulseSubtext)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Credit Limit", fontSize = 11.sp, color = PulseSubtext)
                                    Text("₹${"%,.0f".format(card.creditLimit)}", fontSize = 15.sp, color = PulseOnSurface, fontWeight = FontWeight.Medium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Annual Fee", fontSize = 11.sp, color = PulseSubtext)
                                    Text("₹${"%,.0f".format(card.annualFee)}", fontSize = 15.sp, color = PulseOnSurface, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // Spend progress
            item {
                val topRule = detail.spendRules.firstOrNull()
                if (topRule != null) {
                    val progress = (detail.totalSpentThisCycle / topRule.targetAmount).coerceIn(0.0, 1.0).toFloat()
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PulseCard)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("This Cycle", fontSize = 12.sp, color = PulseSubtext)
                            Text("₹${"%,.0f".format(detail.totalSpentThisCycle)} spent", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PulseOnSurface)
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(PulseSurface)) {
                                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(cardColor, PulseAccent))))
                            }
                            Text("${(progress * 100).toInt()}% to milestone: ${topRule.ruleName}", fontSize = 12.sp, color = PulseSubtext)
                        }
                    }
                }
            }

            // Lounge access
            detail.loungeAccess?.let { lounge ->
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PulseCard)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Lounge Access", fontWeight = FontWeight.SemiBold, color = PulseOnSurface)
                                Text(lounge.loungeNetwork, fontSize = 12.sp, color = PulseSubtext)
                            }
                            Text("${lounge.visitsRemaining}/${lounge.totalVisitsAllowed} left", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PulseGold)
                        }
                    }
                }
            }

            // Transactions header
            item {
                Text("Transactions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PulseOnSurface)
            }

            if (transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions yet", color = PulseSubtext, fontSize = 14.sp)
                    }
                }
            }

            items(transactions) { txn ->
                TransactionItem(txn = txn, onConfirm = { viewModel.confirmTransaction(txn.id) })
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun TransactionItem(txn: Transaction, onConfirm: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PulseCard)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(txn.merchant, fontWeight = FontWeight.Medium, color = PulseOnSurface, fontSize = 14.sp)
                    Text("${txn.category}  •  ${dateFormat.format(txn.date)}  •  ${txn.source}", fontSize = 11.sp, color = PulseSubtext)
                }
                Text("₹${"%,.0f".format(txn.amount)}", fontWeight = FontWeight.Bold, color = PulseOnSurface, fontSize = 15.sp)
            }
            if (txn.isFlagged && txn.status != TransactionStatus.CONFIRMED) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PulseDanger.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("⚠ ${txn.flagReason}", color = PulseDanger, fontSize = 11.sp)
                    }
                    TextButton(onClick = onConfirm, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Confirm", color = PulseSuccess, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
