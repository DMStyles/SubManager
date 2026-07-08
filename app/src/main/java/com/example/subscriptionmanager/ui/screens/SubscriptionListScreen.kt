package com.example.subscriptionmanager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscriptionmanager.BuildConfig
import com.example.subscriptionmanager.ui.AppRoute
import com.example.subscriptionmanager.ui.SubscriptionViewModel
import com.example.subscriptionmanager.ui.SubscriptionWithRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SLBgDark       = Color(0xFF0D0D0D)
private val SLBgCard       = Color(0xFF161B27)
private val SLAccentGreen  = Color(0xFF1DB954)
private val SLAccentBlue   = Color(0xFF4A9EFF)
private val SLTextWhite    = Color(0xFFFFFFFF)
private val SLTextMuted    = Color(0xFF8A9BB5)
private val SLDividerColor = Color(0xFF2A3347)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionListScreen(
    viewModel: SubscriptionViewModel,
    onLogout: () -> Unit
) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val currentMember by viewModel.currentMember.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()

    var showFab by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateCheckResult by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    // Auto-check for updates each time the home screen opens
    LaunchedEffect(Unit) {
        viewModel.loadSubscriptionList()
        viewModel.checkForUpdate()
    }

    // Update available dialog
    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate() },
            containerColor = Color(0xFF1A2035),
            icon = {
                Text("🎉", fontSize = 32.sp)
            },
            title = {
                Text(
                    "Update Available!",
                    color = SLTextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    "SubManager v${info.latestVersion} is now available. Download it to get the latest features and fixes.",
                    color = SLTextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissUpdate()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SLAccentGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Download", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdate() }) {
                    Text("Later", color = SLTextMuted)
                }
            }
        )
    }

    // Settings bottom sheet
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = Color(0xFF161B27),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header
                Text(
                    "Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = SLTextWhite,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Profile card
                currentMember?.let { member ->
                    Surface(
                        color = Color(0xFF1E2740),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(SLAccentGreen, SLAccentBlue)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    member.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                            Column {
                                Text(member.name, color = SLTextWhite, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text("Subscription Manager", color = SLTextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // App version row
                Surface(
                    color = Color(0xFF1E2740),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = SLAccentBlue, modifier = Modifier.size(20.dp))
                            Text("App Version", color = SLTextWhite, fontSize = 15.sp)
                        }
                        Text("v${BuildConfig.VERSION_NAME}", color = SLTextMuted, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Check for updates button
                Surface(
                    color = if (isCheckingUpdate) Color(0xFF1A2E1A) else Color(0xFF1E2740),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isCheckingUpdate) {
                        if (!isCheckingUpdate) {
                            isCheckingUpdate = true
                            updateCheckResult = null
                            coroutineScope.launch {
                                viewModel.checkForUpdate()
                                // give the coroutine a moment to complete
                                kotlinx.coroutines.delay(1500)
                                isCheckingUpdate = false
                                updateCheckResult = if (viewModel.updateInfo.value != null) {
                                    "🎉 Update available! Tap the download button above."
                                } else {
                                    "✓ You're on the latest version!"
                                }
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SLAccentGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = SLAccentGreen, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                if (isCheckingUpdate) "Checking for updates…" else "Check for Updates",
                                color = SLTextWhite,
                                fontSize = 15.sp
                            )
                            updateCheckResult?.let {
                                Text(it, color = SLAccentGreen, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Logout button
                Button(
                    onClick = {
                        showSettings = false
                        viewModel.logout(onLogout)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1A1A))
                ) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFFF5C5C))
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", color = Color(0xFFFF5C5C), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SLBgDark)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), SLBgDark)))
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("My Subscriptions", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = SLTextWhite)
                                Text("${subscriptions.size} group${if (subscriptions.size != 1) "s" else ""}", color = SLTextMuted, fontSize = 14.sp)
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = SLTextMuted)
                            }
                        }
                    }
                }
            }

            if (subscriptions.isEmpty() && !isLoading) {
                item {
                    SLEmptyState(
                        onCreateClick = { viewModel.navigateTo(AppRoute.CreateSubscription()) },
                        onJoinClick = { viewModel.navigateTo(AppRoute.JoinSubscription) }
                    )
                }
            } else {
                items(subscriptions) { item ->
                    SLSubscriptionCard(item = item, onClick = { viewModel.openSubscription(item.subscription.id) })
                }
            }
        }

        // Error snackbar
        errorMessage?.let { msg ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
                action = { TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") } }
            ) { Text(msg) }
        }

        // FAB
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(visible = showFab) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SLFabOption("Join via invite code", Icons.Filled.Send, SLAccentBlue) {
                        showFab = false; viewModel.navigateTo(AppRoute.JoinSubscription)
                    }
                    SLFabOption("Create new group", Icons.Filled.Add, SLAccentGreen) {
                        showFab = false; viewModel.navigateTo(AppRoute.CreateSubscription())
                    }
                }
            }
            FloatingActionButton(
                onClick = { showFab = !showFab },
                containerColor = if (showFab) SLDividerColor else SLAccentGreen
            ) {
                Icon(if (showFab) Icons.Filled.Close else Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    }
}

@Composable
private fun SLFabOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = SLBgCard, shape = RoundedCornerShape(8.dp), tonalElevation = 4.dp) {
            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = SLTextWhite, fontSize = 13.sp)
        }
        SmallFloatingActionButton(onClick = onClick, containerColor = color) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
    }
}

@Composable
private fun SLSubscriptionCard(item: SubscriptionWithRole, onClick: () -> Unit) {
    val sub = item.subscription
    val state = item.myMemberState
    val isAdmin = item.myRole == "admin"

    val statusColor = when (state?.status) {
        "ACTIVE" -> SLAccentGreen
        "PAYMENT DUE TODAY" -> Color(0xFFFFB347)
        "PAYMENT DUE" -> Color(0xFFFF5C5C)
        else -> SLAccentBlue
    }
    val gradientStart = if (isAdmin) Color(0xFF1A2040) else Color(0xFF1A2A1A)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(gradientStart, SLBgCard)))
                .padding(20.dp)
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sub.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SLTextWhite)
                        sub.description?.let { Text(it, color = SLTextMuted, fontSize = 12.sp, maxLines = 1) }
                    }
                    Surface(
                        color = if (isAdmin) Color(0xFF2A3A6A) else Color(0xFF1A3A2A),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            if (isAdmin) "👑 Admin" else "👤 Member",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = if (isAdmin) SLAccentBlue else SLAccentGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SLStatChip("Rs. ${sub.monthly_cost.toInt()}/mo")
                    SLStatChip("Due ${sub.billing_day}th")
                    SLStatChip("${sub.max_members} max")
                }

                if (state != null) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = SLDividerColor)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Your status", color = SLTextMuted, fontSize = 12.sp)
                        Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                state.status,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (state.totalMonthsPaid.toFloat() / state.monthsUsed.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1.5f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = SLDividerColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SLStatChip(text: String) {
    Surface(color = SLDividerColor, shape = RoundedCornerShape(8.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = SLTextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SLEmptyState(onCreateClick: () -> Unit, onJoinClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("📭", fontSize = 64.sp)
        Text("No subscriptions yet", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SLTextWhite, textAlign = TextAlign.Center)
        Text(
            "Create a new subscription group or join one using an invite code from your admin.",
            color = SLTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SLAccentGreen)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Create a Group", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
        OutlinedButton(
            onClick = onJoinClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SLAccentBlue)
        ) {
            Icon(Icons.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Join via Invite Code", fontWeight = FontWeight.SemiBold)
        }
    }
}
