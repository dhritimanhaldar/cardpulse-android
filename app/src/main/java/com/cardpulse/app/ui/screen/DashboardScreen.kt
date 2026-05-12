package com.cardpulse.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.CardWithProgress
import com.cardpulse.app.viewmodel.DashboardViewModel

private sealed class DashboardFilter(val label: String) {
    object None : DashboardFilter("All cards")
    object Unverified : DashboardFilter("Needs verification")
    data class Bank(val bank: String) : DashboardFilter(bank)
    data class Type(val type: String) : DashboardFilter(type)
    data class Network(val network: String) : DashboardFilter(network)
}

private data class DashboardNotification(
    val title: String,
    val message: String,
    val filter: DashboardFilter
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current
    val cardsWithProgress by viewModel.cardsWithProgress.collectAsState()
    val cards = cardsWithProgress.map { it.card }
    val unverifiedCards = cards.filter { it.isAutoFetched && !it.isVerified }
    val hasUnverifiedCards = unverifiedCards.isNotEmpty()
    var activeFilter by remember { mutableStateOf<DashboardFilter>(DashboardFilter.None) }
    var showNotifications by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    val notifications = buildDashboardNotifications(cards)
    val visibleCards = applyDashboardFilter(cardsWithProgress, activeFilter)
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    Scaffold(
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter cards")
                    }
                    DropdownMenu(
                        expanded = showFilters,
                        onDismissRequest = { showFilters = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Needs verification") },
                            onClick = {
                                activeFilter = DashboardFilter.Unverified
                                showFilters = false
                            }
                        )
                        cards.map { it.bankName }.distinct().sorted().forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank) },
                                onClick = {
                                    activeFilter = DashboardFilter.Bank(bank)
                                    showFilters = false
                                }
                            )
                        }
                        cards.map { it.cardType }.distinct().sorted().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    activeFilter = DashboardFilter.Type(type)
                                    showFilters = false
                                }
                            )
                        }
                        cards.map { it.cardNetwork }.distinct().sorted().forEach { network ->
                            DropdownMenuItem(
                                text = { Text(network) },
                                onClick = {
                                    activeFilter = DashboardFilter.Network(network)
                                    showFilters = false
                                }
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showNotifications = !showNotifications }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    if (notifications.isNotEmpty()) {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-6).dp, y = 6.dp)
                        ) {
                            Text(notifications.size.toString())
                        }
                    }
                }
            }

            if (showNotifications) {
                NotificationsSection(
                    notifications = notifications,
                    onNotificationClick = { notification ->
                        activeFilter = notification.filter
                        showNotifications = false
                    }
                )
            }

            if (activeFilter !is DashboardFilter.None) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { activeFilter = DashboardFilter.None },
                        label = { Text("${activeFilter.label}  ×") }
                    )
                }
            }

            if (hasUnverifiedCards) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            activeFilter = DashboardFilter.Unverified
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
private fun NotificationsSection(
    notifications: List<DashboardNotification>,
    onNotificationClick: (DashboardNotification) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (notifications.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No new notifications",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            notifications.forEach { notification ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNotificationClick(notification) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(notification.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                notification.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

private fun buildDashboardNotifications(cards: List<Card>): List<DashboardNotification> {
    val notifications = mutableListOf<DashboardNotification>()
    val unverifiedCount = cards.count { it.isAutoFetched && !it.isVerified }
    if (unverifiedCount > 0) {
        notifications += DashboardNotification(
            title = "$unverifiedCount card(s) need verification",
            message = "Review auto-detected cards before tracking milestones.",
            filter = DashboardFilter.Unverified
        )
    }
    val dueSoon = cards.filter { !it.paymentDueDate.isNullOrBlank() }.take(3)
    dueSoon.forEach { card ->
        notifications += DashboardNotification(
            title = "${card.bankName} payment due",
            message = "Due date: ${card.paymentDueDate}",
            filter = DashboardFilter.Bank(card.bankName)
        )
    }
    val highOutstanding = cards.filter { it.currentOutstanding > 0.0 }
        .sortedByDescending { it.currentOutstanding }
        .take(2)
    highOutstanding.forEach { card ->
        notifications += DashboardNotification(
            title = "${card.bankName} outstanding balance",
            message = "Outstanding: ₹${card.currentOutstanding}",
            filter = DashboardFilter.Bank(card.bankName)
        )
    }
    return notifications
}

private fun applyDashboardFilter(
    cards: List<CardWithProgress>,
    filter: DashboardFilter
): List<CardWithProgress> {
    return when (filter) {
        DashboardFilter.None -> cards
        DashboardFilter.Unverified -> cards.filter { it.card.isAutoFetched && !it.card.isVerified }
        is DashboardFilter.Bank -> cards.filter { it.card.bankName == filter.bank }
        is DashboardFilter.Type -> cards.filter { it.card.cardType == filter.type }
        is DashboardFilter.Network -> cards.filter { it.card.cardNetwork == filter.network }
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
