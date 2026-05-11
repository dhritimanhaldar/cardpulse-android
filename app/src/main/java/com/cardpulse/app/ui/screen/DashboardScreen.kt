package com.cardpulse.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cardpulse.app.model.Card
import com.cardpulse.app.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel()
) {
    val cards by viewModel.cards.collectAsState()
    val hasUnverifiedCards = cards.any { it.isAutoFetched && !it.isVerified }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CardPulse") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_card/-1") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Verification Banner
            if (hasUnverifiedCards) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Verify Your Cards",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Some cards were auto-detected from SMS. Please tap on them to verify and update details for accurate tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Cards List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cards) { card ->
                    CardItem(
                        card = card,
                        onClick = { navController.navigate("card_detail/${card.id}") }
                    )
                }
            }
        }
    }
}

@Composable
fun CardItem(card: Card, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(card.color)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Clean display name
                val displayName = when {
                    !card.cardNickname.isNullOrBlank() -> card.cardNickname
                    else -> "${card.bankName} Card"
                }.let { name ->
                    // Remove duplicate words
                    name.split(" ")
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" ")
                }

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = formatCardNumber(card.cardNumber),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = card.cardHolderName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Verification Indicator
            if (card.isAutoFetched && !card.isVerified) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Needs verification",
                    tint = Color.Yellow,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatCardNumber(number: String): String {
    return number.chunked(4).joinToString(" ")
}