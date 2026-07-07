package com.example.subscriptionmanager.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun ManagerDashboardScreen(
    viewModel: SubscriptionViewModel,
    subscriptionId: String,
    onLogout: () -> Unit,
    onMemberClick: (String) -> Unit,
    onEditSubscription: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val members by viewModel.members.collectAsStateWithLifecycle()
    val activeSubscription by viewModel.activeSubscription.collectAsStateWithLifecycle()
    val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()

    LaunchedEffect(subscriptionId) { viewModel.loadData() }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0D0D), Color(0xFF1A1A2E))))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text(activeSubscription?.name ?: "Group Admin", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
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
    val activeCount = members.count { it.status == "ACTIVE" }
    val upcomingCount = members.count { it.status.startsWith("DUE IN") || it.status == "PAYMENT DUE TODAY" }
    val lateCount = members.count { it.status == "PAYMENT DUE" }
    val totalMembers = members.size
    val collectionRate = if (totalMembers > 0) (activeCount.toFloat() / totalMembers.toFloat()) else 0f
    val animatedRate by animateFloatAsState(collectionRate, tween(1000), label = "rate")

    // Build monthly data: for each month since May 2026, figure out who paid
    val months = remember {
        val list = mutableListOf<Triple<String, String, String>>() // label, yearMonth, displayMonth
        val cal = java.util.Calendar.getInstance()
        val startYear = 2026; val startMonth = 4 // May = month index 4 (0-based)
        val curYear = cal.get(java.util.Calendar.YEAR)
        val curMonth = cal.get(java.util.Calendar.MONTH)
        var y = startYear; var m = startMonth
        while (y < curYear || (y == curYear && m <= curMonth)) {
            val monthName = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                .format(java.util.Calendar.getInstance().apply { set(y, m, 1) }.time)
            val key = "$y-${String.format("%02d", m + 1)}"
            list.add(Triple(monthName, key, key))
            if (m == 11) { m = 0; y++ } else m++
        }
        list.reversed() // newest first
    }

    var selectedMonth by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    fun monthIndex(yearMonth: String): Int {
        val parts = yearMonth.split("-")
        if (parts.size != 2) return 1
        val year = parts[0].toIntOrNull() ?: 2026
        val month = parts[1].toIntOrNull() ?: 5
        return (year - 2026) * 12 + (month - 5) + 1
    }

    // Who paid for a given month key
    fun paidForMonth(yearMonth: String): List<MemberState> {
        val targetIdx = monthIndex(yearMonth)
        return members.filter { member ->
            member.totalMonthsPaid >= targetIdx
        }
    }

    // Monthly breakdown bottom sheet
    if (selectedMonth != null) {
        val paid = paidForMonth(selectedMonth!!.second)
        val unpaid = members.filter { m -> paid.none { it.id == m.id } }
        val monthRevenue = paid.sumOf { member ->
            val direct = member.payments.filter { it.payment_for_month == selectedMonth!!.second }.sumOf { it.amount }
            if (direct > 0) direct else 212.0
        }
        Dialog(onDismissRequest = { selectedMonth = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141E30)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A2E20)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DateRange, null, tint = AccentGreen, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(selectedMonth!!.first, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 17.sp)
                            Text("Rs. ${monthRevenue.toInt()} collected", color = AccentGreen, fontSize = 12.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { selectedMonth = null }) {
                            Icon(Icons.Filled.Close, null, tint = TextMuted)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    if (paid.isNotEmpty()) {
                        Text("✓  Paid (${paid.size})", color = AccentGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        paid.forEach { m ->
                            val directAmt = m.payments.filter { it.payment_for_month == selectedMonth!!.second }.sumOf { it.amount }
                            val amt = if (directAmt > 0) directAmt else 212.0
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF1A2E20)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(m.name.first().toString(), color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(m.name, color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text("Rs. ${amt.toInt()}", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (unpaid.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Divider(color = Color(0xFF2D3A55))
                        Spacer(Modifier.height(16.dp))
                        Text("✗  Not Paid (${unpaid.size})", color = AccentRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        unpaid.forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF3D1A1A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(m.name.first().toString(), color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(m.name, color = TextWhite, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text("Not paid", color = AccentRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(24.dp)) }

        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Admin Dashboard", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 26.sp)
                    Text("Spotify Premium · $totalMembers members", color = TextMuted, fontSize = 13.sp)
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AccentGreen, Color(0xFF00BCD4)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                }
            }
        }

        // Hero Revenue Card
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A3A2A), Color(0xFF0D2040))))
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Total Revenue", color = TextMuted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Rs. ${totalRevenue.toInt()}", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 40.sp)
                    Spacer(Modifier.height(16.dp))
                    // Collection rate bar
                    Text("Collection Rate · ${(collectionRate * 100).toInt()}%", color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedRate },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                        color = AccentGreen,
                        trackColor = Color(0xFF2D3A55),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatChip(label = "Paid", value = "$activeCount", color = AccentGreen, modifier = Modifier.weight(1f))
                        StatChip(label = "Upcoming", value = "$upcomingCount", color = Color(0xFFFFC107), modifier = Modifier.weight(1f))
                        StatChip(label = "Late", value = "$lateCount", color = AccentRed, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Monthly History
        item {
            Text("Monthly History", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 18.sp)
            Text("Tap any month to see who paid", color = TextMuted, fontSize = 12.sp)
        }

        items(months) { (label, key, _) ->
            val paidList = paidForMonth(key)
            val unpaidList = members.filter { m -> paidList.none { it.id == m.id } }
            val monthAmt = paidList.sumOf { member ->
                val d = member.payments.filter { it.payment_for_month == key }.sumOf { it.amount }
                if (d > 0) d else 212.0
            }
            val isComplete = unpaidList.isEmpty() && members.isNotEmpty()

            Card(
                onClick = { selectedMonth = Triple(label, key, key) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Month icon
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (isComplete) Color(0xFF1A2E20) else Color(0xFF2D1A1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isComplete) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            null,
                            tint = if (isComplete) AccentGreen else AccentRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(label, fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        // Avatar row of paid members
                        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                            paidList.take(5).forEach { m ->
                                Box(
                                    Modifier.size(22.dp).clip(CircleShape)
                                        .background(AccentGreen.copy(alpha = 0.3f))
                                        .border(1.dp, AccentGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(m.name.first().toString(), color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (paidList.size > 5) {
                                Box(
                                    Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF2D3A55)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+${paidList.size - 5}", color = TextMuted, fontSize = 8.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${paidList.size}/${members.size} paid", color = TextMuted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Rs. ${monthAmt.toInt()}", fontWeight = FontWeight.Bold, color = if (isComplete) AccentGreen else TextWhite, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 20.sp)
            Text(label, color = color.copy(alpha = 0.7f), fontSize = 11.sp)
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
    val pingSuccess by viewModel.successMessage.collectAsStateWithLifecycle()
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
