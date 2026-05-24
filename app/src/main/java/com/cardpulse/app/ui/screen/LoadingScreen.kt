package com.cardpulse.app.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class DetailedLoadingStep(
    val title: String,
    val description: String,
    val percentage: Int,
    val stepNumber: Int,
    val totalSteps: Int
)

@Composable
fun DynamicLoadingScreen(
    currentStep: LoadingStep,
    modifier: Modifier = Modifier
) {
    DynamicLoadingScreen(
        currentStep = DetailedLoadingStep(
            title = currentStep.title,
            description = currentStep.description,
            percentage = currentStep.percentage,
            stepNumber = currentStep.ordinal + 1,
            totalSteps = LoadingStep.entries.size
        ),
        modifier = modifier
    )
}

@Composable
fun DynamicLoadingScreen(
    currentStep: DetailedLoadingStep,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { currentStep.percentage / 100f },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp
                )
                Text(
                    text = "${currentStep.percentage}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = currentStep.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentStep.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(currentStep.totalSteps) { index ->
                    StepIndicator(
                        isActive = index + 1 == currentStep.stepNumber,
                        isCompleted = index + 1 < currentStep.stepNumber
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Step ${currentStep.stepNumber} of ${currentStep.totalSteps}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun StepIndicator(isActive: Boolean, isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .size(if (isActive) 12.dp else 8.dp)
            .clip(CircleShape)
            .background(
                when {
                    isActive -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                }
            )
            .animateContentSize()
    )
}

enum class LoadingStep(
    val title: String,
    val description: String,
    val percentage: Int
) {
    AUTHENTICATING(
        title = "Connecting Securely",
        description = "Authenticating your Gmail account.",
        percentage = 25
    ),
    FETCHING_STATEMENTS(
        title = "Reading Statements",
        description = "Looking for credit card statements in Gmail.",
        percentage = 50
    ),
    PARSING_TRANSACTIONS(
        title = "Processing Transactions",
        description = "Extracting transactions from SMS and email.",
        percentage = 75
    ),
    LOADING_REWARDS(
        title = "Setting Up Rewards",
        description = "Preparing milestones, perks, and reward tracking.",
        percentage = 100
    )
}
