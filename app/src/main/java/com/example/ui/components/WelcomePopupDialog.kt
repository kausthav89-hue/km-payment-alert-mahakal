package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

@Composable
fun WelcomePopupDialog(
    isOpen: Boolean,
    onDismiss: (dontShowAgain: Boolean) -> Unit
) {
    if (!isOpen) return

    var dontShowAgain by remember { mutableStateOf(false) }

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
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(32.dp))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEAB308),
                            Color(0xFF38BDF8),
                            Color(0xFF22C55E)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF030712)
                        )
                    )
                )
                .padding(24.dp)
                .testTag("welcome_popup_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Official Brand Logo
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(26.dp),
                            spotColor = Color(0xFFEAB308)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFDE047), Color(0xFFCA8A04), Color(0xFF38BDF8))
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.kausthav_brand_logo),
                        contentDescription = "Kausthav Mahakal Brand Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Brand Headline: "Powered by Kausthav Mahakal"
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = Color(0xFFEAB308).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(50)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Verified Creator",
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Powered by Kausthav Mahakal",
                            color = Color(0xFFFDE047),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Payment Alert Soundbox",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Instant UPI voice alerts & lockscreen heads-up for PhonePe, Google Pay & Paytm",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Feature Highlights List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureRowItem(
                        icon = Icons.Default.NotificationsActive,
                        iconTint = Color(0xFF22C55E),
                        title = "Smart UPI Notification Reader",
                        subtitle = "Extracts sender & amount automatically from PhonePe, GPay, Paytm"
                    )

                    FeatureRowItem(
                        icon = Icons.Default.Headphones,
                        iconTint = Color(0xFF38BDF8),
                        title = "Bengali & English Voice Engine",
                        subtitle = "Rahul Sharma আপনাকে ৫০০ টাকা পাঠালেন। গ্রহণ করেছেন Kausthav"
                    )

                    FeatureRowItem(
                        icon = Icons.Default.Lock,
                        iconTint = Color(0xFFA855F7),
                        title = "Lock Screen Heads-Up Overlay",
                        subtitle = "Wakes phone screen and presents prominent payment banner"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // "Don't show again" toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dontShowAgain = !dontShowAgain }
                        .padding(vertical = 4.dp),
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
                        modifier = Modifier.testTag("welcome_dont_show_checkbox")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Don't show this welcome splash on startup",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main CTA Button
                Button(
                    onClick = { onDismiss(dontShowAgain) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("welcome_continue_button"),
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
                            text = "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continue",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
