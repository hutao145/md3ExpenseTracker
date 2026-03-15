package com.example.expensetracker.ui.screen

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.security.PinManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val PIN_LENGTH = 4
private const val MAX_ATTEMPTS = 5
private const val COOLDOWN_SECONDS = 30

@Composable
fun LockScreen(
    sharedPreferences: SharedPreferences,
    onUnlocked: () -> Unit,
    onBiometricClick: () -> Unit,
    biometricEnabled: Boolean
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var cooldownEndTime by remember { mutableLongStateOf(0L) }
    var cooldownRemaining by remember { mutableIntStateOf(0) }

    // Shake animation
    val shakeOffset = remember { Animatable(0f) }

    // Cooldown timer
    LaunchedEffect(cooldownEndTime) {
        if (cooldownEndTime > 0L) {
            while (true) {
                val remaining = ((cooldownEndTime - System.currentTimeMillis()) / 1000).toInt()
                if (remaining <= 0) {
                    cooldownRemaining = 0
                    cooldownEndTime = 0L
                    errorMessage = null
                    break
                }
                cooldownRemaining = remaining
                delay(1000)
            }
        }
    }

    // Auto-verify when PIN reaches target length
    LaunchedEffect(pinInput) {
        if (pinInput.length == PIN_LENGTH) {
            if (PinManager.verifyPin(sharedPreferences, pinInput)) {
                failedAttempts = 0
                onUnlocked()
            } else {
                failedAttempts++
                if (failedAttempts >= MAX_ATTEMPTS) {
                    cooldownEndTime = System.currentTimeMillis() + COOLDOWN_SECONDS * 1000L
                    errorMessage = "尝试次数过多，请等待 ${COOLDOWN_SECONDS}s"
                } else {
                    errorMessage = "密码错误，还剩 ${MAX_ATTEMPTS - failedAttempts} 次机会"
                }
                pinInput = ""
                // Trigger shake
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.3f, stiffness = 800f),
                    initialVelocity = 1200f
                )
            }
        }
    }

    // Auto-trigger biometric on first show
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            delay(300) // Wait for compose to settle
            onBiometricClick()
        }
    }

    val isCoolingDown = cooldownRemaining > 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App icon placeholder
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "\uD83D\uDD12",
                        fontSize = 32.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "输入 PIN 密码",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN dots with shake animation
            Row(
                modifier = Modifier.offset {
                    IntOffset(shakeOffset.value.roundToInt(), 0)
                },
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(PIN_LENGTH) { index ->
                    val filled = index < pinInput.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            AnimatedContent(
                targetState = if (isCoolingDown) "请等待 ${cooldownRemaining}s 后重试" else errorMessage,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "ErrorMessage"
            ) { msg ->
                Text(
                    text = msg ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.height(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Number pad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(
                    if (biometricEnabled) "bio" else "",
                    "0",
                    "del"
                )
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        when (key) {
                            "bio" -> {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = !isCoolingDown) { onBiometricClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "生物识别",
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            "del" -> {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = pinInput.isNotEmpty() && !isCoolingDown) {
                                            pinInput = pinInput.dropLast(1)
                                            errorMessage = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "删除",
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                            "" -> {
                                Spacer(modifier = Modifier.size(72.dp))
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable(enabled = pinInput.length < PIN_LENGTH && !isCoolingDown) {
                                            pinInput += key
                                            errorMessage = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
