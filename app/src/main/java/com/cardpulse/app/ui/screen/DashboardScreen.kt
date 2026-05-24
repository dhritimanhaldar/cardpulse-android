package com.cardpulse.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.cardpulse.app.auth.AuthManager
import com.cardpulse.app.model.Card
import com.cardpulse.app.model.CardWithProgress
import com.cardpulse.app.util.cleanAndNormalizeBankName
import com.cardpulse.app.viewmodel.DashboardViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch

private sealed class DashboardFilter(val label: String) {
    object None : DashboardFilter("All Cards")
    object Unverified : DashboardFilter("Unverified Cards")
    object PaymentDueSoon : DashboardFilter("Payment Due Soon")
    data class Bank(val bank: String) : DashboardFilter(bank)
    data class Type(val type: String) : DashboardFilter("$type Cards")
    data class Network(val network: String) : DashboardFilter(network)
}

private data class DashboardNotification(
    val title: String,
    val message: String,
    val filter: DashboardFilter,
    val isRead: Boolean = false
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
    val filters = buildAvailableFilters(cards)
    val notifications = buildDashboardNotifications(cards)
    val unreadCount = notifications.count { !it.isRead }
    val visibleCards = applyDashboardFilter(cardsWithProgress, activeFilter)
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val account = remember { GoogleSignIn.getLastSignedInAccount(context) }
    val authManager = remember { AuthManager(context) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_card/-1") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { innerPadding ->
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        AppDrawerContent(
                            userName = account?.displayName ?: "User",
                            userEmail = account?.email.orEmpty(),
                            notifications = notifications,
                            filters = filters,
                            activeFilter = activeFilter,
                            onFilterClick = { filter -> activeFilter = filter },
                            onNotificationClick = { notification ->
                                activeFilter = notification.filter
                                scope.launch { drawerState.close() }
                            },
                            onSignOut = {
                                scope.launch {
                                    authManager.signOut()
                                    drawerState.close()
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (activeFilter !is DashboardFilter.None) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { activeFilter = DashboardFilter.None },
                            label = { Text("${activeFilter.label}  x") }
                        )
                    }
                }

                if (hasUnverifiedCards) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { activeFilter = DashboardFilter.Unverified },
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
                    items(visibleCards, key = { it.card.id }) { item ->
                        CardItem(
                            card = item.card,
                            onClick = { navController.navigate("card_detail/${item.card.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawerContent(
    userName: String,
    userEmail: String,
    notifications: List<DashboardNotification>,
    filters: List<DashboardFilter>,
    activeFilter: DashboardFilter,
    onFilterClick: (DashboardFilter) -> Unit,
    onNotificationClick: (DashboardNotification) -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileSection(userName = userName, userEmail = userEmail)
        Divider()
        DrawerSection(title = "Notifications") {
            if (notifications.isEmpty()) {
                Text(
                    text = "No notifications",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                notifications.forEach { notification ->
                    NotificationItem(notification = notification, onClick = { onNotificationClick(notification) })
                }
            }
        }
        Divider()
        DrawerSection(title = "Filters") {
            filters.filterNot { it is DashboardFilter.None }.forEach { filter ->
                FilterCheckboxItem(
                    filter = filter,
                    isChecked = filter == activeFilter,
                    onToggle = { onFilterClick(if (filter == activeFilter) DashboardFilter.None else filter) }
                )
            }
        }
        Divider()
        DrawerSection(title = "Settings") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSignOut)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sign Out", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ProfileSection(userName: String, userEmail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.firstOrNull()?.toString() ?: "U",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(userName, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            Text(
                userEmail,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DrawerSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun NotificationItem(
    notification: DashboardNotification,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (notification.isRead) Color.Gray else MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
            )
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterCheckboxItem(
    filter: DashboardFilter,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isChecked, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(12.dp))
        Text(filter.label)
    }
}

@Composable
fun CardItem(card: Card, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = safeCardColor(card.color)
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
                Text(
                    text = cleanCardDisplayName(card),
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

private fun buildAvailableFilters(cards: List<Card>): List<DashboardFilter> {
    val filters = mutableListOf<DashboardFilter>(DashboardFilter.None)
    if (cards.any { it.isAutoFetched && !it.isVerified }) filters += DashboardFilter.Unverified
    cards.map { cleanAndNormalizeBankName(it.bankName) }
        .distinctBy { it.lowercase() }
        .filter { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
        .sorted()
        .forEach { filters += DashboardFilter.Bank(it) }
    cards.map { decodeCardType(it.cardType) }
        .distinctBy { it.lowercase() }
        .filter { it.isNotBlank() && it != "Unknown" }
        .sorted()
        .forEach { filters += DashboardFilter.Type(it) }
    cards.map { decodeCardNetwork(it.cardNetwork) }
        .distinctBy { it.lowercase() }
        .filter { it.isNotBlank() && it != "Unknown" }
        .sorted()
        .forEach { filters += DashboardFilter.Network(it) }
    if (cards.any { !it.paymentDueDate.isNullOrBlank() }) filters += DashboardFilter.PaymentDueSoon
    return filters
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
    cards.filter { !it.paymentDueDate.isNullOrBlank() }.take(3).forEach { card ->
        notifications += DashboardNotification(
            title = "${cleanAndNormalizeBankName(card.bankName)} payment due",
            message = "Due date: ${card.paymentDueDate}",
            filter = DashboardFilter.PaymentDueSoon
        )
    }
    cards.filter { it.currentOutstanding > 0.0 }
        .sortedByDescending { it.currentOutstanding }
        .take(2)
        .forEach { card ->
            notifications += DashboardNotification(
                title = "${cleanAndNormalizeBankName(card.bankName)} outstanding balance",
                message = "Outstanding: ₹${card.currentOutstanding}",
                filter = DashboardFilter.Bank(cleanAndNormalizeBankName(card.bankName))
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
        DashboardFilter.PaymentDueSoon -> cards.filter { !it.card.paymentDueDate.isNullOrBlank() }
        is DashboardFilter.Bank -> cards.filter { cleanAndNormalizeBankName(it.card.bankName) == filter.bank }
        is DashboardFilter.Type -> cards.filter { decodeCardType(it.card.cardType) == filter.type }
        is DashboardFilter.Network -> cards.filter { decodeCardNetwork(it.card.cardNetwork) == filter.network }
    }
}

private fun decodeCardType(rawType: String): String {
    return when (rawType.lowercase().trim()) {
        "sp", "premium", "pr" -> "Premium"
        "cb", "cashback" -> "Cashback"
        "en", "entry" -> "Entry Level"
        "p" -> "Points/Rewards"
        "v" -> "Travel/Voucher"
        "credit", "credit card" -> "Credit Card"
        "debit", "debit card" -> "Debit Card"
        "" -> "Unknown"
        else -> rawType.replaceFirstChar { it.uppercase() }
    }
}

private fun decodeCardNetwork(rawNetwork: String): String {
    return when (rawNetwork.lowercase().trim()) {
        "v", "visa" -> "Visa"
        "mc", "mastercard" -> "Mastercard"
        "rupay" -> "RuPay"
        "amex", "american express", "americanexpress" -> "American Express"
        "" -> "Unknown"
        else -> rawNetwork.replaceFirstChar { it.uppercase() }
    }
}

private fun cleanCardDisplayName(card: Card): String {
    val rawName = when {
        card.cardName.isNotBlank() -> {
            if (card.cardName.contains(card.bankName, ignoreCase = true)) card.cardName else "${card.bankName} ${card.cardName}"
        }
        else -> "${card.bankName} Card"
    }
    val words = rawName.split(Regex("\\s+")).filter { it.isNotBlank() }
    return words.filterIndexed { index, word ->
        index == 0 || !word.equals(words.getOrNull(index - 1), ignoreCase = true)
    }.joinToString(" ").trim()
}

private fun safeCardColor(color: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(color)) }
        .getOrElse { Color(0xFF1A73E8) }
}
