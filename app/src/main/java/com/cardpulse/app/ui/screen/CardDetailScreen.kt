package com.cardpulse.app.ui.screen

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardpulse.app.model.Transaction
import com.cardpulse.app.ui.theme.*
import com.cardpulse.app.viewmodel.CardDetailViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(cardId: Int, onBack: () -> Unit, onDeleted: () -> Unit = onBack) {
    val context = LocalContext.current
    val viewModel: CardDetailViewModel = viewModel(factory = CardDetailViewModel.factory(context, cardId))
    val cardDetail by viewModel.cardDetail.collectAsStateWithLifecycle()
    val perkProgressList by viewModel.perkProgressList.collectAsStateWithLifecycle()
    val milestoneProgressList by viewModel.milestoneProgressList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val card = cardDetail?.card
    val cardColor = remember(card?.color) {
        card?.color?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { PulseAccent } } ?: PulseAccent
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Card") },
            text = { Text("Permanently delete card and all transactions?") },
            confirmButton = { TextButton(onClick = { showDeleteDialog = false; viewModel.deleteCard { onDeleted() } }) { Text("Delete", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = PulseBackground,
        topBar = {
            TopAppBar(
                title = { Text(card?.cardName ?: "Card Detail", color = PulseText) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = PulseText) } },
                actions = { IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B6B)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PulseSurface)
            )
        }
    ) { padding ->
        if (isLoading || card == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PulseAccent) }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Card Visual
            item {
                Box(
                    Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(cardColor, cardColor.copy(0.6f)))).padding(20.dp)
                ) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(card.bankName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(card.cardType, color = Color.White.copy(0.8f), fontSize = 13.sp)
                        }
                        Text(card.cardNetwork, color = Color.White.copy(0.7f), fontSize = 13.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("•••• •••• •••• ${card.last4Digits}", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 2.sp)
                            Text(card.cardName, color = Color.White.copy(0.85f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Milestones
            if (milestoneProgressList.isNotEmpty()) {
                item { Text("Milestones", color = PulseText, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp)) }
                items(milestoneProgressList) { mp ->
                    MilestoneCard(mp, cardColor)
                }
            }

            // Perks
            if (perkProgressList.isNotEmpty()) {
                item { Text("Perks & Rewards", color = PulseText, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp)) }
                items(perkProgressList) { pp ->
                    PerkCard(pp, cardColor)
                }
            }
        }
    }
}

@Composable
fun MilestoneCard(mp: CardDetailViewModel.MilestoneProgress, cardColor: Color) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = PulseSurface) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(mp.milestone.n, color = PulseText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(if (mp.isAchieved) "✓ Achieved" else "${(mp.progress * 100).toInt()}%", color = if (mp.isAchieved) Color(0xFF4CAF50) else PulseSubtext, fontSize = 12.sp)
            }
            Text(mp.milestone.rw, color = cardColor, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { mp.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (mp.isAchieved) Color(0xFF4CAF50) else cardColor,
                trackColor = PulseSubtext.copy(0.15f)
            )
            Spacer(Modifier.height(4.dp))
            Text("₹%.0f / ₹%.0f".format(mp.currentAmount, mp.milestone.ta.toDouble()), color = PulseSubtext, fontSize = 12.sp)
        }
    }
}

@Composable
fun PerkCard(pp: CardDetailViewModel.PerkProgress, cardColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = PulseSurface) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(getPerkIcon(pp.perk.rt), null, tint = cardColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(pp.perk.n, color = PulseText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    pp.perk.mn?.let { Text("Min: ₹$it", color = PulseSubtext, fontSize = 11.sp) }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = PulseSubtext)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pp.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (pp.isAchieved) Color(0xFF4CAF50) else cardColor,
                trackColor = PulseSubtext.copy(0.15f)
            )
            Spacer(Modifier.height(4.dp))
            Text("${pp.qualifyingTxns.size} txns · ₹%.0f".format(pp.currentAmount), color = PulseSubtext, fontSize = 12.sp)
            
            AnimatedVisibility(expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = PulseSubtext.copy(0.1f))
                    Spacer(Modifier.height(8.dp))
                    if (pp.qualifyingTxns.isEmpty()) {
                        Text("No qualifying transactions", color = PulseSubtext, fontSize = 12.sp)
                    } else {
                        pp.qualifyingTxns.forEach { txn -> MiniTxnRow(txn) }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniTxnRow(txn: Transaction) {
    val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(txn.merchant, color = PulseText, fontSize = 13.sp, maxLines = 1)
            Text(fmt.format(txn.date), color = PulseSubtext, fontSize = 11.sp)
        }
        Text("₹%.0f".format(txn.amount), color = if (txn.isCredit) Color(0xFF4CAF50) else Color(0xFFFF6B6B), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

fun getPerkIcon(rt: String): ImageVector = when (rt) {
    "p" -> Icons.Default.Stars
    "c" -> Icons.Default.Money
    "mi" -> Icons.Default.Flight
    "v" -> Icons.Default.CardGiftcard
    "w" -> Icons.Default.Check
    "l" -> Icons.Default.MeetingRoom
    else -> Icons.Default.Category
}
