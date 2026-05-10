package com.cardpulse.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.SpendRule
import com.cardpulse.app.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.cardpulse.app.viewmodel.DashboardViewModel
import com.cardpulse.app.viewmodel.GmailSyncViewModel
import com.cardpulse.app.viewmodel.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onCardClick: (Int) -> Unit, onAddCard: () -> Unit) {
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(context))
    val cardsWithProgress by viewModel.cardsWithProgress.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val syncVm: GmailSyncViewModel = viewModel()
    val syncState by syncVm.syncState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = PulseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "CardPulse",
                        fontWeight = FontWeight.Bold,
                        color      = PulseOnSurface,
                        fontSize   = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = { syncVm.syncNow() }) {
                        if (syncState is SyncState.Syncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PulseBlue)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync Gmail", tint = PulseSubtext)
                        }
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = PulseSubtext)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PulseBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = onAddCard,
                containerColor   = PulseBlue,
                contentColor     = Color.White,
                shape            = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Card")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (syncState is SyncState.Done) {
                val count = (syncState as SyncState.Done).newCount
                Text(
                    text = if (count > 0) "✓ $count new transactions synced" else "✓ Already up to date",
                    color = PulseSubtext,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                // Refresh dashboard after sync
                LaunchedEffect(count) {
                    if (count > 0) viewModel.refreshDashboard()
                }
            }
            if (syncState is SyncState.Error) {
                Text(
                    text = "Sync failed: ${(syncState as SyncState.Error).message}",
                    color = PulseDanger,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PulseBlue)
                }
            } else if (errorMessage != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage!!, color = PulseDanger, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Summary header
                    item {
                        SummaryHeader(
                            totalCards = cardsWithProgress.size,
                            totalSpent = cardsWithProgress.sumOf { it.totalSpentThisCycle }
                        )
                    }

                    // Card list
                    items(cardsWithProgress) { cwp ->
                        CardProgressItem(
                            card = cwp.card,
                            spentAmount = cwp.totalSpentThisCycle,
                            targetAmount = cwp.spendRules.firstOrNull()?.targetAmount ?: 100000.0,
                            onClick = { onCardClick(cwp.card.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryHeader(totalCards: Int, totalSpent: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = PulseCard)
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Column {
                Text("This Month", fontSize = 12.sp, color = PulseSubtext)
                Text(
                    text       = "₹${"%,.0f".format(totalSpent)}",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color      = PulseOnSurface
                )
                Text("across $totalCards cards", fontSize = 12.sp, color = PulseSubtext)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Active Cards", fontSize = 12.sp, color = PulseSubtext)
                Text(
                    text       = "$totalCards",
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = PulseBlue
                )
            }
        }
    }
}

@Composable
fun CardProgressItem(
    card: Card,
    spentAmount: Double,
    targetAmount: Double,
    onClick: () -> Unit
) {
    val progress    = (spentAmount / targetAmount).coerceIn(0.0, 1.0).toFloat()
    val cardColor   = try { Color(android.graphics.Color.parseColor(card.color)) } catch (e: Exception) { PulseBlue }
    val remaining   = targetAmount - spentAmount
    val progressPct = (progress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.13f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Card header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text       = "${card.bankName} ${card.cardName}",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp,
                        color      = PulseOnSurface
                    )
                    Text(
                        text     = "•••• ${card.last4Digits}  ${card.cardType}",
                        fontSize = 12.sp,
                        color    = PulseSubtext
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardColor.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text      = "$progressPct%",
                        color     = cardColor,
                        fontWeight = FontWeight.Bold,
                        fontSize  = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Spend amounts
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text     = "₹${"%,.0f".format(spentAmount)} spent",
                    fontSize = 13.sp,
                    color    = PulseOnSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text     = "₹${"%,.0f".format(remaining)} to milestone",
                    fontSize = 13.sp,
                    color    = PulseSubtext
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PulseSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(listOf(cardColor, PulseAccent))
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Target label
            Text(
                text     = "Milestone: ₹${"%,.0f".format(targetAmount)} → 10,000 bonus points",
                fontSize = 12.sp,
                color    = PulseSubtext
            )
        }
    }
}
