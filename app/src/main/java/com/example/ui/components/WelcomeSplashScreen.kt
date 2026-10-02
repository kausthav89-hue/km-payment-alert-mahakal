package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun WelcomeSplashScreen(
    isOpen: Boolean,
    onDismiss: (dontShowAgain: Boolean) -> Unit
) {
    if (!isOpen) return

    var startAnimation by remember { mutableStateOf(false) }
    var dontShowAgain by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        startAnimation = true
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    // Fade-in and scale-in animation for logo
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "logoAlpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    // Staggered fade-in and slide-up animation for the main title
    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )
    val titleOffset by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 30f,
        animationSpec = tween(durationMillis = 900, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "titleOffset"
    )

    // Footer animation
    val footerAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "footerAlpha"
    )

    // Pulsing outer halo around the logo
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    Dialog(
        onDismissRequest = { onDismiss(dontShowAgain) },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF020617), // Deepest obsidian midnight
                            Color(0xFF0F172A),
                            Color(0xFF020617)
                        )
                    )
                )
                .testTag("welcome_splash_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: Logo + Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // 1. APP LOGO ON TOP with smooth fade-in and scale
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(logoScale * pulseGlow)
                            .alpha(logoAlpha)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF38BDF8),
                                        Color(0xFF22C55E),
                                        Color(0xFFA855F7),
                                        Color(0xFF06B6D4),
                                        Color(0xFF38BDF8)
                                    )
                                )
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_payment_soundbox),
                            contentDescription = "Kausthav Mahakal Payment Alert Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(118.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // 2. MAIN TITLE IN BOLD PROFESSIONAL FONT: "KAUSTHAV MAHAKAL PAYMENT ALERT"
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .alpha(titleAlpha)
                            .offset { IntOffset(0, titleOffset.toInt()) }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = Color(0xFF22C55E).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "OFFICIAL SOUNDBOX EDITION",
                                    color = Color(0xFF86EFAC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "KAUSTHAV MAHAKAL",
                            color = Color(0xFF38BDF8),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "PAYMENT ALERT",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.5.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Smart UPI Voice Announcer & Lockscreen Soundbox",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Supported Apps Badge Strip
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.alpha(titleAlpha)
                    ) {
                        AppPill(name = "PhonePe", color = Color(0xFF7C3AED))
                        AppPill(name = "Google Pay", color = Color(0xFF0284C7))
                        AppPill(name = "Paytm", color = Color(0xFF059669))
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Feature highlights mini-card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(titleAlpha),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SplashFeatureItem(
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                tint = Color(0xFF22C55E),
                                text = "Loud Bengali & English Voice Alerts: \"Rahul Sharma আপনাকে ৫০০ টাকা পাঠালেন। গ্রহণ করেছেন Kausthav\""
                            )
                            SplashFeatureItem(
                                icon = Icons.Default.Lock,
                                tint = Color(0xFF38BDF8),
                                text = "Instant Heads-Up Window wakes screen on incoming UPI credit"
                            )
                            SplashFeatureItem(
                                icon = Icons.Default.NotificationsActive,
                                tint = Color(0xFFA855F7),
                                text = "24/7 Background Keep-Alive Engine without battery kills"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom section: CTA Button + Checkbox + Footer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(footerAlpha)
                ) {
                    // "Don't show again" Checkbox
                    Row(
                        modifier = Modifier
                            .clickable { dontShowAgain = !dontShowAgain }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF22C55E),
                                uncheckedColor = Color(0xFF64748B),
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.testTag("splash_dont_show_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Skip splash on next launch",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Enter Button
                    Button(
                        onClick = { onDismiss(dontShowAgain) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("splash_enter_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22C55E),
                            contentColor = Color(0xFF0F172A)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Enter Dashboard",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Enter",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. FOOTER TEXT AT THE BOTTOM: "Powered by Kausthav Mahakal"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Verified Creator",
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Powered by Kausthav Mahakal",
                            color = Color(0xFFFDE047),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Version 1.0.0 • All Rights Reserved",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AppPill(name: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SplashFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .offset(y = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
