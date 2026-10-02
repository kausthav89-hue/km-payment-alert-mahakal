package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.PaymentNotification
import com.example.service.PaymentKeepAliveService
import com.example.ui.PaymentAlertViewModel
import com.example.ui.components.BatteryOptimizationDialog
import com.example.ui.components.WelcomePopupDialog
import com.example.ui.theme.PaymentAlertTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PaymentAlertViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start foreground service if enabled
        if (viewModel.isServiceEnabled.value) {
            PaymentKeepAliveService.start(this)
        }

        setContent {
            PaymentAlertTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestNotificationAccess = { openNotificationListenerSettings() },
                    onRequestOverlayPermission = { openOverlaySettings() },
                    onRequestIgnoreBattery = { requestIgnoreBatteryOptimization() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionStates()
    }

    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(fallbackIntent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PaymentAlertViewModel,
    onRequestNotificationAccess: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestIgnoreBattery: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Recheck permissions whenever activity resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Android 13+ Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionStates()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val isServiceEnabled by viewModel.isServiceEnabled.collectAsState()
    val receiverName by viewModel.receiverName.collectAsState()
    val ttsLanguage by viewModel.ttsLanguage.collectAsState()
    val boostVolume by viewModel.boostVolume.collectAsState()
    val showLockscreenOverlay by viewModel.showLockscreenOverlay.collectAsState()
    val showFloatingOverlay by viewModel.showFloatingOverlay.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val hasSeenWelcome by viewModel.hasSeenWelcome.collectAsState()
    val recentPayments by viewModel.recentPayments.collectAsState()

    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsState()
    val isBatteryOptimizationIgnored by viewModel.isBatteryOptimizationIgnored.collectAsState()

    var showWelcomeDialog by remember { mutableStateOf(!hasSeenWelcome) }
    var showBatteryGuideDialog by remember { mutableStateOf(false) }

    // Dialogs
    WelcomePopupDialog(
        isOpen = showWelcomeDialog,
        onDismiss = { dontShowAgain ->
            showWelcomeDialog = false
            if (dontShowAgain) {
                viewModel.setWelcomeSeen(true)
            }
        }
    )

    BatteryOptimizationDialog(
        isOpen = showBatteryGuideDialog,
        onDismiss = { showBatteryGuideDialog = false },
        onRequestIgnoreBattery = onRequestIgnoreBattery
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isServiceEnabled) Color(0xFF22C55E) else Color(0xFF94A3B8)
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Payment Alert",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isServiceEnabled) "Soundbox Active" else "Soundbox Paused",
                                fontSize = 11.sp,
                                color = if (isServiceEnabled) Color(0xFF86EFAC) else Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showBatteryGuideDialog = true },
                        modifier = Modifier.testTag("action_battery_guide")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = "24/7 Keep-Alive Guide",
                            tint = Color(0xFFFBBF24)
                        )
                    }

                    IconButton(
                        onClick = { showWelcomeDialog = true },
                        modifier = Modifier.testTag("action_about_creator")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Powered by Kausthav Mahakal",
                            tint = Color(0xFFFDE047)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0A0F1D)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Master Service Switch Card
            item {
                MasterStatusCard(
                    isServiceEnabled = isServiceEnabled,
                    onToggle = { viewModel.toggleService(it) }
                )
            }

            // 2. Permission & Readiness Checklist
            item {
                ReadinessChecklistCard(
                    isNotificationAccessGranted = isNotificationAccessGranted,
                    isOverlayPermissionGranted = isOverlayPermissionGranted,
                    isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                    onRequestNotificationAccess = onRequestNotificationAccess,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestIgnoreBattery = { showBatteryGuideDialog = true }
                )
            }

            // 3. Soundbox Configuration & Preferences
            item {
                SoundboxSettingsCard(
                    receiverName = receiverName,
                    onReceiverNameChange = { viewModel.updateReceiverName(it) },
                    selectedLanguage = ttsLanguage,
                    onLanguageChange = { viewModel.updateLanguage(it) },
                    boostVolume = boostVolume,
                    onToggleBoostVolume = { viewModel.toggleBoostVolume(it) },
                    showLockscreenOverlay = showLockscreenOverlay,
                    onToggleLockscreen = { viewModel.toggleLockscreenOverlay(it) },
                    showFloatingOverlay = showFloatingOverlay,
                    onToggleFloating = { viewModel.toggleFloatingOverlay(it) },
                    speechRate = speechRate,
                    onSpeechRateChange = { viewModel.updateSpeechRate(it) }
                )
            }

            // 4. Live Payment Notification Simulator
            item {
                PaymentSimulatorCard(
                    receiverName = receiverName,
                    ttsLanguage = ttsLanguage,
                    onSimulatePayment = { sender, amount, appSource ->
                        viewModel.simulatePaymentAlert(sender, amount, appSource)
                    },
                    onTestVoiceOnly = { sender, amount ->
                        viewModel.testVoiceAnnouncement(sender, amount)
                    }
                )
            }

            // 5. Recent Payment Alerts Log
            item {
                RecentAlertsHeader(
                    hasItems = recentPayments.isNotEmpty(),
                    onClear = { viewModel.clearHistory() }
                )
            }

            if (recentPayments.isEmpty()) {
                item {
                    EmptyAlertsPlaceholder()
                }
            } else {
                items(recentPayments, key = { it.id }) { payment ->
                    PaymentHistoryItem(
                        payment = payment,
                        onReplay = { viewModel.replayPayment(payment) }
                    )
                }
            }
        }
    }
}

@Composable
fun MasterStatusCard(
    isServiceEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val cardBg by animateColorAsState(
        targetValue = if (isServiceEnabled) Color(0xFF132A22) else Color(0xFF1E293B),
        label = "bg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("master_status_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isServiceEnabled) Color(0xFF22C55E).copy(alpha = 0.5f) else Color(0xFF475569)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isServiceEnabled) Color(0xFF22C55E).copy(alpha = 0.2f)
                            else Color(0xFF64748B).copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = if (isServiceEnabled) Color(0xFF22C55E) else Color(0xFF94A3B8),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (isServiceEnabled) "Soundbox is ON" else "Soundbox is OFF",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isServiceEnabled)
                            "Ready to speak PhonePe, GPay, Paytm UPI alerts"
                        else
                            "Tap switch to activate 24/7 background listener",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isServiceEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF0F172A),
                    checkedTrackColor = Color(0xFF22C55E),
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF334155)
                ),
                modifier = Modifier.testTag("master_service_switch")
            )
        }
    }
}

@Composable
fun ReadinessChecklistCard(
    isNotificationAccessGranted: Boolean,
    isOverlayPermissionGranted: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    onRequestNotificationAccess: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestIgnoreBattery: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("readiness_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Permission Readiness",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                val allReady = isNotificationAccessGranted && isOverlayPermissionGranted && isBatteryOptimizationIgnored
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (allReady) Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (allReady) "All Ready ✓" else "Action Needed",
                        color = if (allReady) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Notification Listener
            PermissionRow(
                title = "Notification Access",
                subtitle = "Required to read incoming UPI payment alerts",
                isGranted = isNotificationAccessGranted,
                actionLabel = "Enable Access",
                onAction = onRequestNotificationAccess
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Draw Over Apps (Overlay)
            PermissionRow(
                title = "Lock Screen / Floating Overlay",
                subtitle = "Displays instant alert window on locked phone",
                isGranted = isOverlayPermissionGranted,
                actionLabel = "Grant Overlay",
                onAction = onRequestOverlayPermission
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Battery Optimization
            PermissionRow(
                title = "Battery Unrestricted",
                subtitle = "Prevents Android OS from killing soundbox in background",
                isGranted = isBatteryOptimizationIgnored,
                actionLabel = "View Guide",
                onAction = onRequestIgnoreBattery
            )
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.6f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF22C55E) else Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }

        if (!isGranted) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF38BDF8)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(text = actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SoundboxSettingsCard(
    receiverName: String,
    onReceiverNameChange: (String) -> Unit,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    boostVolume: Boolean,
    onToggleBoostVolume: (Boolean) -> Unit,
    showLockscreenOverlay: Boolean,
    onToggleLockscreen: (Boolean) -> Unit,
    showFloatingOverlay: Boolean,
    onToggleFloating: (Boolean) -> Unit,
    speechRate: Float,
    onSpeechRateChange: (Float) -> Unit
) {
    var nameInput by remember(receiverName) { mutableStateOf(receiverName) }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("soundbox_settings_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "Voice Announcement Settings",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Receiver Name Field
            Text(
                text = "Receiver Name (Account Holder):",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    nameInput = it
                    onReceiverNameChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receiver_name_input"),
                placeholder = { Text("e.g. Kausthav") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF22C55E),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selection (Bengali / English)
            Text(
                text = "Voice Language:",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedLanguage == "bn",
                    onClick = { onLanguageChange("bn") },
                    label = { Text("বাংলা (Bengali)", fontSize = 13.sp) },
                    leadingIcon = if (selectedLanguage == "bn") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lang_chip_bn"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF22C55E),
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = selectedLanguage == "en",
                    onClick = { onLanguageChange("en") },
                    label = { Text("English (India)", fontSize = 13.sp) },
                    leadingIcon = if (selectedLanguage == "en") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lang_chip_en"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF22C55E),
                        selectedLabelColor = Color(0xFF0F172A),
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview Format Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Announcement Preview:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val previewText = PaymentNotification.buildAnnouncement(
                        sender = "Rahul Sharma",
                        amount = 500.0,
                        receiver = receiverName.ifBlank { "Kausthav" },
                        language = selectedLanguage
                    )
                    Text(
                        text = "\"$previewText\"",
                        color = Color(0xFF86EFAC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggles
            SettingToggleRow(
                title = "Boost to Max Volume",
                subtitle = "Temporarily sets alarm volume to 100% to override silent mode",
                checked = boostVolume,
                onCheckedChange = onToggleBoostVolume
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingToggleRow(
                title = "Lock Screen Heads-Up Overlay",
                subtitle = "Turns on screen & shows payment dialog when phone is locked",
                checked = showLockscreenOverlay,
                onCheckedChange = onToggleLockscreen
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingToggleRow(
                title = "Floating Banner Overlay",
                subtitle = "Shows a floating payment banner while using other apps",
                checked = showFloatingOverlay,
                onCheckedChange = onToggleFloating
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Speech rate slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Speech Speed:", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                Text(
                    text = String.format(java.util.Locale.US, "%.2fx", speechRate),
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = speechRate,
                onValueChange = onSpeechRateChange,
                valueRange = 0.7f..1.3f,
                steps = 6,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF22C55E),
                    activeTrackColor = Color(0xFF22C55E),
                    inactiveTrackColor = Color(0xFF334155)
                )
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0F172A),
                checkedTrackColor = Color(0xFF22C55E),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}

@Composable
fun PaymentSimulatorCard(
    receiverName: String,
    ttsLanguage: String,
    onSimulatePayment: (sender: String, amount: Double, appSource: String) -> Unit,
    onTestVoiceOnly: (sender: String, amount: Double) -> Unit
) {
    var senderInput by remember { mutableStateOf("Rahul Sharma") }
    var amountInput by remember { mutableStateOf("500") }
    var selectedApp by remember { mutableStateOf("PhonePe") }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("simulator_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Live Payment Simulator",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF38BDF8).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Instant Testing",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Simulate an incoming UPI notification to test voice announcements and lockscreen alert without real money:",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // App selection chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("PhonePe", "Google Pay", "Paytm").forEach { app ->
                    FilterChip(
                        selected = selectedApp == app,
                        onClick = { selectedApp = app },
                        label = { Text(app, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (app) {
                                "PhonePe" -> Color(0xFF7C3AED)
                                "Google Pay" -> Color(0xFF0284C7)
                                else -> Color(0xFF059669)
                            },
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sender and Amount inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = senderInput,
                    onValueChange = { senderInput = it },
                    label = { Text("Sender Name", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("test_sender_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22C55E),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount (₹)", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("test_amount_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22C55E),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trigger Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        val amt = amountInput.toDoubleOrNull() ?: 500.0
                        onTestVoiceOnly(senderInput.ifBlank { "Rahul Sharma" }, amt)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("test_voice_only_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF38BDF8)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Voice Only", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val amt = amountInput.toDoubleOrNull() ?: 500.0
                        onSimulatePayment(senderInput.ifBlank { "Rahul Sharma" }, amt, selectedApp)
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("simulate_payment_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E),
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulate Live Alert", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecentAlertsHeader(
    hasItems: Boolean,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent Payment Alerts",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        if (hasItems) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.testTag("clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear History",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyAlertsPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = Color(0xFF475569),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No Payment Alerts Yet",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Incoming UPI notifications from PhonePe, GPay, and Paytm will be logged here.",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PaymentHistoryItem(
    payment: PaymentNotification,
    onReplay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_${payment.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "₹",
                        color = Color(0xFF22C55E),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${payment.formattedAmount}",
                            color = Color(0xFF22C55E),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = payment.appSource,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "From: ${payment.sender}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = payment.getFormattedTime(),
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onReplay,
                modifier = Modifier.testTag("replay_voice_${payment.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Replay Voice",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
