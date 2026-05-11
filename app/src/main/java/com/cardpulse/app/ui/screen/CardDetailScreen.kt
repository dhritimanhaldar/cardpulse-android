package com.cardpulse.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cardpulse.app.viewmodel.CardDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    navController: NavController,
    cardId: Long
) {
    val viewModel: CardDetailViewModel = viewModel()
    val card by viewModel.card.collectAsState()
    val resolvedCard by viewModel.resolvedCard.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    LaunchedEffect(cardId) {
        viewModel.loadCard(cardId)
    }

    card?.let { cardData ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(cardData.cardNickname ?: "Card Details") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Edit Button
                        IconButton(
                            onClick = {
                                navController.navigate("add_card/${cardData.id}")
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Card",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(cardData.color)
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Card Visual
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(cardData.color)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = cardData.bankName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = formatCardNumber(cardData.cardNumber),
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
                                        text = "CARD HOLDER",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = cardData.cardHolderName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                                Column {
                                    Text(
                                        text = "EXPIRES",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "${cardData.expiryMonth}/${cardData.expiryYear}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Milestone Progress Bars
                resolvedCard?.let { resolved ->
                    if (resolved.perks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Milestone Progress",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        items(resolved.perks) { perk ->
                            MilestoneProgressCard(
                                perk = perk,
                                onExpand = { /* Handle expansion to show transactions */ }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Transactions Section
                item {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(transactions) { transaction ->
                    TransactionItem(transaction)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun MilestoneProgressCard(
    perk: Any, // Replace with actual ResolvedPerk type
    onExpand: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onExpand
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Milestone Name", // Replace with perk.name
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = 0.6f, // Replace with actual progress
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "60% complete", // Replace with actual progress text
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransactionItem(transaction: Any) {
    // Implement transaction display
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Transaction", style = MaterialTheme.typography.bodyMedium)
                Text("Date", style = MaterialTheme.typography.bodySmall)
            }
            Text("₹0.00", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatCardNumber(number: String): String {
    return number.chunked(4).joinToString(" ")
}