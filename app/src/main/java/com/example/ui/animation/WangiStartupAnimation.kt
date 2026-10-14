package com.example.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WangiStartupAnimation(
    onAnimationComplete: () -> Unit
) {
    var animationStage by remember { mutableIntStateOf(1) }

    // Stage 1: Red plus appearance & expansion (0 - 800ms)
    // Stage 2: Plus rotation & pulse, reveals logo (800 - 1600ms)
    // Stage 3: Animated red and gold waves radiate outward (1600 - 2400ms)
    // Stage 4: "WANGI TECH SOLUTIONS" + Tagline fades in (2400 - 3200ms)

    LaunchedEffect(Unit) {
        delay(600)
        animationStage = 2
        delay(700)
        animationStage = 3
        delay(700)
        animationStage = 4
        delay(1000)
        onAnimationComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePulse"
    )

    val plusScale by animateFloatAsState(
        targetValue = when (animationStage) {
            1 -> 1f
            2 -> 1.35f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "plusScale"
    )

    val plusRotation by animateFloatAsState(
        targetValue = when (animationStage) {
            1 -> 0f
            2 -> 45f
            else -> 90f
        },
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "plusRotation"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (animationStage >= 3) 1f else 0f,
        animationSpec = tween(500),
        label = "textAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D0D0D),
                        Color(0xFF1A1A1A),
                        Color(0xFF140707)
                    )
                )
            )
            .clickable { onAnimationComplete() }
            .testTag("startup_animation_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Skip Button top right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.clickable { onAnimationComplete() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Radar / Signal Waves radiating outward
        if (animationStage >= 2) {
            Canvas(modifier = Modifier.size(280.dp)) {
                val center = Offset(size.width / 2, size.height / 2)

                // Outermost Red wave
                drawCircle(
                    color = WangiRed.copy(alpha = (0.4f / wavePulse).coerceIn(0f, 0.4f)),
                    radius = (size.minDimension / 2) * (wavePulse * 0.95f),
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Gold middle wave
                drawCircle(
                    color = WangiGold.copy(alpha = (0.5f / wavePulse).coerceIn(0f, 0.5f)),
                    radius = (size.minDimension / 2) * (wavePulse * 0.7f),
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Inner Red glow wave
                drawCircle(
                    color = WangiRedPulse.copy(alpha = 0.3f),
                    radius = (size.minDimension / 2) * 0.45f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Central Emblem (Plus transitioning to Wi-Fi technology symbol)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(plusScale)
                    .rotate(plusRotation)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                WangiRed,
                                WangiDarkRed,
                                Color(0xFF5A0000)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (animationStage <= 2) {
                    // Bright Red Plus symbol that rotates and expands
                    Canvas(modifier = Modifier.size(44.dp)) {
                        val strokeW = 8.dp.toPx()
                        // Horizontal bar
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = strokeW
                        )
                        // Vertical bar
                        drawLine(
                            color = Color.White,
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = strokeW
                        )
                    }
                } else {
                    // Revealed WANGI Wi-Fi technology symbol with gold accent
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "WANGI Signal Logo",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha)
            ) {
                Text(
                    text = "WANGI TECH SOLUTIONS",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(WangiGold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart Networks. Better Business.",
                        color = WangiGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(WangiGold)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Uganda Flag & Kampala Indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "Universal Business Management Platform",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
