package com.example.subscriptionmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscriptionmanager.ui.SubscriptionViewModel
import java.util.*

private val BgDark2     = Color(0xFF0D0D0D)
private val BgCard2     = Color(0xFF161B27)
private val AccGreen2   = Color(0xFF1DB954)
private val AccBlue2    = Color(0xFF4A9EFF)
private val TxtWhite2   = Color(0xFFFFFFFF)
private val TxtMuted2   = Color(0xFF8A9BB5)
private val DivColor2   = Color(0xFF2A3347)

// ─────────────────────────────────────────────
// Create Subscription Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var monthlyCost by remember { mutableStateOf("211") }
    var billingDay by remember { mutableStateOf("8") }
    var maxMembers by remember { mutableStateOf("6") }
    var startDate by remember {
        val cal = Calendar.getInstance()
        mutableStateOf("${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}-01")
    }

    LaunchedEffect(errorMessage) { /* errors shown inline */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), BgDark2)))
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column {
                    IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TxtMuted2)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("➕ Create Group", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = TxtWhite2)
                    Text("Set up a new subscription group", color = TxtMuted2, fontSize = 14.sp)
                }
            }

            // Form
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                FormCard {
                    Text("Group Details", fontWeight = FontWeight.SemiBold, color = TxtWhite2, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    SubField("Subscription Name *", name, { name = it }, "e.g. Spotify Family Plan")
                    Spacer(Modifier.height(12.dp))
                    SubField("Description (optional)", description, { description = it }, "e.g. Shared premium plan")
                }

                FormCard {
                    Text("Billing Settings", fontWeight = FontWeight.SemiBold, color = TxtWhite2, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SubField("Cost/month (Rs.)", monthlyCost, { monthlyCost = it }, "211", KeyboardType.Number)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SubField("Billing day", billingDay, { billingDay = it }, "8", KeyboardType.Number)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SubField("Start Date", startDate, { startDate = it }, "YYYY-MM-DD")
                }

                FormCard {
                    Text("Members", fontWeight = FontWeight.SemiBold, color = TxtWhite2, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("How many members can join this group?", color = TxtMuted2, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))

                    val options = listOf(2, 3, 4, 5, 6, 7, 8, 10, 12, 15, 20)
                    val maxMembersInt = maxMembers.toIntOrNull() ?: 6

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.take(6).forEach { n ->
                            MemberCountChip(n, maxMembersInt == n) { maxMembers = n.toString() }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.drop(6).forEach { n ->
                            MemberCountChip(n, maxMembersInt == n) { maxMembers = n.toString() }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Custom input
                    OutlinedTextField(
                        value = if (options.contains(maxMembersInt)) "" else maxMembers,
                        onValueChange = { maxMembers = it },
                        label = { Text("Custom number", color = TxtMuted2) },
                        placeholder = { Text("Enter any number", color = TxtMuted2) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                errorMessage?.let {
                    Surface(
                        color = Color(0xFF3A1A1A),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(it, color = Color(0xFFFF6B6B), modifier = Modifier.padding(14.dp), fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = {
                        viewModel.clearError()
                        viewModel.createSubscription(
                            name = name.trim(),
                            description = description.trim().ifBlank { null },
                            monthlyCost = monthlyCost.toDoubleOrNull() ?: 211.0,
                            billingDay = billingDay.toIntOrNull() ?: 8,
                            startDate = startDate.trim(),
                            maxMembers = maxMembers.toIntOrNull() ?: 6
                        )
                    },
                    enabled = name.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccGreen2)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Group", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────
// Join Subscription Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinSubscriptionScreen(
    viewModel: SubscriptionViewModel,
    onBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var inviteCode by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark2),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start).offset(x = (-12).dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TxtMuted2)
            }

            Text("🔗", fontSize = 60.sp)
            Text("Join a Group", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = TxtWhite2)
            Text(
                "Enter the invite code your admin shared with you.",
                color = TxtMuted2,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase().take(8) },
                label = { Text("Invite Code", color = TxtMuted2) },
                placeholder = { Text("e.g. ABC-XYZ9", color = TxtMuted2) },
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TxtWhite2
                )
            )

            errorMessage?.let {
                Surface(color = Color(0xFF3A1A1A), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = Color(0xFFFF6B6B), modifier = Modifier.padding(14.dp), fontSize = 13.sp)
                }
            }

            Button(
                onClick = {
                    viewModel.clearError()
                    viewModel.joinSubscription(inviteCode)
                },
                enabled = inviteCode.length >= 7 && !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccBlue2)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Join Group", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Edit Subscription Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubscriptionScreen(
    viewModel: SubscriptionViewModel,
    subscriptionId: String,
    onBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()
    val activeSubscription by viewModel.activeSubscription.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var monthlyCost by remember { mutableStateOf("211") }
    var billingDay by remember { mutableStateOf("8") }
    var startDate by remember { mutableStateOf("2026-05-01") }
    var maxMembers by remember { mutableStateOf("6") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Populate form from active subscription
    LaunchedEffect(activeSubscription) {
        activeSubscription?.let { sub ->
            name = sub.name
            description = sub.description ?: ""
            monthlyCost = sub.monthly_cost.toString()
            billingDay = sub.billing_day.toString()
            startDate = sub.start_date
            maxMembers = sub.max_members.toString()
        }
    }

    Box(Modifier.fillMaxSize().background(BgDark2)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), BgDark2)))
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column {
                    IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TxtMuted2)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("⚙️ Edit Subscription", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = TxtWhite2)
                    Text("Update your group settings", color = TxtMuted2, fontSize = 14.sp)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Invite Code Card
                FormCard {
                    Text("Invite Code", fontWeight = FontWeight.SemiBold, color = TxtWhite2, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth()
                            .background(Color(0xFF0D1117), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            inviteCode ?: "Loading…",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = 4.sp,
                            color = AccGreen2
                        )
                        Row {
                            IconButton(onClick = {
                                val shareText = "Join my subscription group using code: ${inviteCode ?: ""}"
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Invite Code"))
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = AccBlue2)
                            }
                            IconButton(onClick = { viewModel.refreshInviteCode(subscriptionId) }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = TxtMuted2)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("⏰ Invite codes expire after 24 hours. Tap refresh to generate a new one.", color = TxtMuted2, fontSize = 11.sp)
                }

                // Group Details
                FormCard {
                    Text("Group Details", fontWeight = FontWeight.SemiBold, color = TxtWhite2, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    SubField("Subscription Name *", name, { name = it }, "e.g. Spotify Family Plan")
                    Spacer(Modifier.height(12.dp))
                    SubField("Description (optional)", description, { description = it }, "e.g. Shared premium plan")
                }

                FormCard {
                    Text("Billing Settings", fontWeight = FontWeight.SemiBold, color = TxtWhite2, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SubField("Cost/month (Rs.)", monthlyCost, { monthlyCost = it }, "211", KeyboardType.Number)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SubField("Billing day", billingDay, { billingDay = it }, "8", KeyboardType.Number)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    SubField("Start Date", startDate, { startDate = it }, "YYYY-MM-DD")
                }

                FormCard {
                    Text("Max Members", fontWeight = FontWeight.SemiBold, color = TxtWhite2, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Current: ${members.size} member(s) joined", color = TxtMuted2, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    val options = listOf(2, 3, 4, 5, 6, 7, 8, 10, 12, 15, 20)
                    val maxMembersInt = maxMembers.toIntOrNull() ?: 6
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.take(6).forEach { n ->
                            MemberCountChip(n, maxMembersInt == n) { maxMembers = n.toString() }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.drop(6).forEach { n ->
                            MemberCountChip(n, maxMembersInt == n) { maxMembers = n.toString() }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = if (options.contains(maxMembersInt)) "" else maxMembers,
                        onValueChange = { maxMembers = it },
                        label = { Text("Custom number", color = TxtMuted2) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Members list with remove
                if (members.isNotEmpty()) {
                    FormCard {
                        Text("Members", fontWeight = FontWeight.SemiBold, color = TxtWhite2, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))
                        members.forEach { m ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier.size(36.dp).background(Color(0xFF2A3347), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(m.name.first().uppercase(), color = TxtWhite2, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Column {
                                        Text(m.name, color = TxtWhite2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(m.status, color = TxtMuted2, fontSize = 11.sp)
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteMember(m.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color(0xFFFF5C5C), modifier = Modifier.size(20.dp))
                                }
                            }
                            HorizontalDivider(color = DivColor2.copy(alpha = 0.5f))
                        }
                    }
                }

                successMessage?.let {
                    Surface(color = Color(0xFF1A3A2A), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(it, color = AccGreen2, modifier = Modifier.padding(14.dp), fontSize = 13.sp)
                    }
                }
                errorMessage?.let {
                    Surface(color = Color(0xFF3A1A1A), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(it, color = Color(0xFFFF6B6B), modifier = Modifier.padding(14.dp), fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = {
                        viewModel.clearError()
                        viewModel.clearSuccess()
                        viewModel.updateSubscription(
                            subscriptionId = subscriptionId,
                            name = name.trim(),
                            description = description.trim().ifBlank { null },
                            monthlyCost = monthlyCost.toDoubleOrNull() ?: 211.0,
                            billingDay = billingDay.toIntOrNull() ?: 8,
                            startDate = startDate.trim(),
                            maxMembers = maxMembers.toIntOrNull() ?: 6
                        )
                    },
                    enabled = name.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccGreen2)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5C5C)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Group", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        // Delete confirm dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = BgCard2,
                title = { Text("Delete Group?", color = TxtWhite2, fontWeight = FontWeight.Bold) },
                text = { Text("This will permanently delete the subscription and all payment records. This cannot be undone.", color = TxtMuted2) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSubscription(subscriptionId)
                    }) { Text("Delete", color = Color(0xFFFF5C5C), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = TxtMuted2) }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────
// Shared Composable Helpers
// ─────────────────────────────────────────────

@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard2)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TxtMuted2) },
        placeholder = { Text(placeholder, color = TxtMuted2.copy(alpha = 0.5f)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = outlinedFieldColors(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun MemberCountChip(count: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) AccGreen2 else DivColor2,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            "$count",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (selected) Color.White else TxtMuted2,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccGreen2,
    unfocusedBorderColor = DivColor2,
    focusedLabelColor = AccGreen2,
    cursorColor = AccGreen2,
    focusedTextColor = TxtWhite2,
    unfocusedTextColor = TxtWhite2
)
