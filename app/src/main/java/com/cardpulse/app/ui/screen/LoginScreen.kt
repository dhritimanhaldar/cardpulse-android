package com.cardpulse.app.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardpulse.app.auth.AuthManager
import com.cardpulse.app.auth.AuthResult
import com.cardpulse.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loadingStep by remember { mutableStateOf<LoadingStep?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            isLoading = true
            loadingStep = LoadingStep.AUTHENTICATING
            when (val authResult = authManager.handleSignInResult(result)) {
                is AuthResult.Success -> {
                    loadingStep = LoadingStep.FETCHING_STATEMENTS
                    delay(700)
                    loadingStep = LoadingStep.PARSING_TRANSACTIONS
                    delay(700)
                    loadingStep = LoadingStep.LOADING_REWARDS
                    delay(700)
                    loadingStep = null
                    onLoginSuccess()
                }
                is AuthResult.Error -> {
                    errorMessage = authResult.message
                    isLoading = false
                    loadingStep = null
                }
                AuthResult.Cancelled -> {
                    isLoading = false
                    loadingStep = null
                }
            }
        }
    }

    loadingStep?.let { step ->
        DynamicLoadingScreen(currentStep = step)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PulseBackground,
                        PulseDarkBlue.copy(alpha = 0.3f),
                        PulseBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CreditCard,
                contentDescription = "CardPulse",
                tint = PulseBlue,
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = "CardPulse",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = PulseOnSurface
            )

            Text(
                text = "Track every rupee. Never miss a reward.",
                fontSize = 14.sp,
                color = PulseSubtext,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let {
                Text(
                    text = it,
                    color = PulseDanger,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    if (!isLoading) {
                        errorMessage = null
                        loadingStep = LoadingStep.AUTHENTICATING
                        launcher.launch(authManager.getSignInIntent())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PulseBlue
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = PulseOnSurface,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Text(
                text = "Your data stays on your device.\nGoogle account used for Gmail access only.",
                fontSize = 11.sp,
                color = PulseSubtext,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
