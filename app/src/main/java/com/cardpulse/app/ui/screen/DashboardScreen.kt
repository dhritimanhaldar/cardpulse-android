package com.cardpulse.app.ui.screen

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.cardpulse.app.model.Card
import com.cardpulse.app.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    val cardsWithProgress by viewModel.cardsWithProgress.collectAsState()
    val cards = cardsWithProgress.map { it.card }
    val unverifiedCards = cards.filter { it.isAutoFetched && !it.isVerified }
    val hasUnverifiedCards = unverifiedCards.isNotEmpty()
    var showOnlyUnverified by remember { mutableStateOf(false) }
    val visibleCards = if (showOnlyUnverified) {
        cardsWithProgress.filter { it.card.isAutoFetched && !it.card.isVerified }
    } else {
        cardsWithProgress
    }

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
            if (hasUnverifiedCards) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            showOnlyUnverified = true
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "You have ${unverifiedCards.size} unverified auto-detected card(s)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap to review.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibleCards) { item ->
                    CardItem(
                        card = item.card,
                        onClick = { navController.navigate("card_detail/${item.card.id}") }
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
            containerColor = Color(android.graphics.Color.parseColor(card.color))
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
                val displayName = cleanCardDisplayName(card)

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "•••• ${card.last4Digits}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (card.isVerified) "Verified" else "Needs verification",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

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

private fun cleanCardDisplayName(card: Card): String {
    val rawName = when {
        card.cardName.isNotBlank() -> {
            if (card.cardName.contains(card.bankName, ignoreCase = true)) {
                card.cardName
            } else {
                "${card.bankName} ${card.cardName}"
            }
        }
        else -> "${card.bankName} Card"
    }

    val words = rawName
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    return words
        .filterIndexed { index, word ->
            index == 0 || !word.equals(words.getOrNull(index - 1), ignoreCase = true)
        }
        .joinToString(" ")
        .trim()
}
