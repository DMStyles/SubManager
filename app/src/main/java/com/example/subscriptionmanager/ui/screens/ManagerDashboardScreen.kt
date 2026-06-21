package com.example.subscriptionmanager.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscriptionmanager.ui.MemberState
import com.example.subscriptionmanager.ui.SubscriptionViewModel

@Composable
fun ManagerDashboardScreen(viewModel: SubscriptionViewModel, onLogout: () -> Unit, onMemberClick: (String) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val members by viewModel.members.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadData() }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0D0D), Color(0xFF1A1A2E))))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF111827), tonalElevation = 0.dp) {
                    listOf(
                        Triple(Icons.Filled.Home, "Overview", 0),
                        Triple(Icons.Filled.Person, "Members", 1),
                        Triple(Icons.Filled.Settings, "Settings", 2)
                    ).forEach { (icon, label, index) ->
                        NavigationBarItem(
                            icon = { Icon(icon, null) },
                            label = { Text(label) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentGreen, selectedTextColor = AccentGreen,
                                indicatorColor = Color(0xFF1A2E20)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(Modifier.padding(paddingValues).fillMaxSize()) {
                when (selectedTab) {
                    0 -> ManagerOverviewTab(members)
                    1 -> ManagerMembersTab(viewModel, members, onMemberClick)
                    2 -> AppSettingsTab(onLogout) // Reuse from MemberDashboardScreen
                }
            }
        }
    }
}

@Composable
fun ManagerOverviewTab(members: List<MemberState>) {
    val totalRevenue = members.flatMap { it.payments }.sumOf { it.amount }
    val activeMembers = members.count { it.status == "ACTIVE" }
    val dueMembers = members.count { it.status == "PAYMENT DUE" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Text("Admin Dashboard", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 26.sp)
            Text("Here's your Spotify group's overview", color = TextMuted, fontSize = 14.sp)
        }
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(Modifier.padding(22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Revenue", color = TextMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Rs. ${totalRevenue.toInt()}", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 34.sp)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f).aspectRatio(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2E20)), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$activeMembers", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 36.sp)
                        Text("Active", color = TextWhite, fontSize = 14.sp)
                    }
                }
                Card(modifier = Modifier.weight(1f).aspectRatio(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF3D1A1A)), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$dueMembers", fontWeight = FontWeight.Bold, color = AccentRed, fontSize = 36.sp)
                        Text("Due", color = TextWhite, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ManagerMembersTab(viewModel: SubscriptionViewModel, members: List<MemberState>, onMemberClick: (String) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text("Add New Member", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text("Member Name", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = Color(0xFF2D3A55),
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false; newMemberName = "" }) { Text("Cancel", color = TextMuted) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { if (newMemberName.isNotBlank()) { viewModel.addMember(newMemberName.trim()); showAddDialog = false; newMemberName = "" } },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                        ) { Text("Add", color = Color.White) }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("All Members", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 22.sp)
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.background(AccentGreen, CircleShape).size(38.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Member", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        items(members) { member ->
            ManagerMemberCard(member, onMemberClick)
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun ManagerMemberCard(member: MemberState, onMemberClick: (String) -> Unit) {
    val progress = if (member.totalMonthsPaid > 0) member.monthsUsed.toFloat() / member.totalMonthsPaid.toFloat() else 0f
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(800), label = "p")
    val viewModel: SubscriptionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val pingSuccess by viewModel.pingSuccess.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(pingSuccess) {
        if (pingSuccess != null) {
            android.widget.Toast.makeText(context, pingSuccess, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearPingSuccess()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF2D3A55)), contentAlignment = Alignment.Center) {
                    Text(member.name.first().toString(), fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.name, fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 15.sp)
                    Text("${member.payments.size} payments · Rs. ${member.payments.sumOf { it.amount }.toInt()}", color = TextMuted, fontSize = 11.sp)
                }
                StatusBadge(member.status)
            }

            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = if (animatedProgress > 0.85f) AccentRed else AccentGreen,
                trackColor = Color(0xFF2D3A55), strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Paid: ${member.totalMonthsPaid} mo", color = TextMuted, fontSize = 11.sp)
                Text("Used: ${member.monthsUsed} mo", color = TextMuted, fontSize = 11.sp)
                Text("Left: ${member.remainingBalance} mo", color = if (member.remainingBalance <= 0) AccentRed else AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onMemberClick(member.id) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3A55), contentColor = TextWhite)
                ) { Text("Details", fontSize = 13.sp) }

                Button(
                    onClick = { viewModel.pingMember(member.id, member.name) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D2A10), contentColor = Color(0xFFFFA726))
                ) {
                    Icon(Icons.Filled.Notifications, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ping", fontSize = 13.sp)
                }
            }
        }
    }
}
