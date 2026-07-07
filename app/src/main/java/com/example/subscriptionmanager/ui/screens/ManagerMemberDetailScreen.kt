package com.example.subscriptionmanager.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscriptionmanager.ui.MemberState
import com.example.subscriptionmanager.ui.SubscriptionViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerMemberDetailScreen(viewModel: SubscriptionViewModel, memberId: String, subscriptionId: String = "", onBack: () -> Unit) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val member = members.find { it.id == memberId }
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var amountText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(todayDateString()) }
    var paymentForMonth by remember { mutableStateOf(currentMonthString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (member == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Member not found", color = TextWhite) }
        return
    }

    val progress = if (member.totalMonthsPaid > 0) member.monthsUsed.toFloat() / member.totalMonthsPaid.toFloat() else 0f
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(800), label = "p")

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, day ->
            selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
            showDatePicker = false
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        showDatePicker = false
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            currentMonth = paymentForMonth,
            onMonthSelected = { paymentForMonth = it; showMonthPicker = false },
            onDismiss = { showMonthPicker = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CardBg,
            title = { Text("Remove ${member.name}?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove this member and all their data.", color = TextMuted) },
            confirmButton = {
                Button(onClick = { viewModel.deleteMember(member.id); showDeleteConfirm = false; onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Remove", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = TextMuted) } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0D0D0D), Color(0xFF1A1A2E))))) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(member.name, color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = TextWhite) }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Filled.Delete, "Delete", tint = AccentRed) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                StatusBadge(member.status)
                                Text("Total Paid: Rs. ${member.payments.sumOf { it.amount }.toInt()}", color = AccentGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                                color = if (animatedProgress > 0.85f) AccentRed else AccentGreen,
                                trackColor = Color(0xFF2D3A55), strokeCap = StrokeCap.Round
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Paid: ${member.totalMonthsPaid} mo", color = TextMuted, fontSize = 12.sp)
                                Text("Used: ${member.monthsUsed} mo", color = TextMuted, fontSize = 12.sp)
                                Text("Left: ${member.remainingBalance} mo", color = if (member.remainingBalance <= 0) AccentRed else AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Add Payment", fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 16.sp)
                            Spacer(Modifier.height(14.dp))
                            
                            OutlinedTextField(
                                value = amountText, onValueChange = { amountText = it },
                                label = { Text("Amount (Rs)", color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentGreen, unfocusedBorderColor = Color(0xFF2D3A55),
                                    focusedTextColor = TextWhite, unfocusedTextColor = TextWhite
                                )
                            )
                            Spacer(Modifier.height(10.dp))
                            
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = selectedDate, onValueChange = {}, readOnly = true,
                                    label = { Text("Date", color = TextMuted) },
                                    trailingIcon = { Icon(Icons.Filled.DateRange, null, tint = AccentGreen, modifier = Modifier.clickable { showDatePicker = true }) },
                                    modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentGreen, unfocusedBorderColor = Color(0xFF2D3A55),
                                        disabledTextColor = TextWhite, disabledBorderColor = Color(0xFF2D3A55), disabledLabelColor = TextMuted
                                    )
                                )
                                OutlinedTextField(
                                    value = paymentForMonth, onValueChange = {}, readOnly = true,
                                    label = { Text("For Month", color = TextMuted) },
                                    trailingIcon = { Icon(Icons.Filled.List, null, tint = AccentGreen, modifier = Modifier.clickable { showMonthPicker = true }) },
                                    modifier = Modifier.weight(1f).clickable { showMonthPicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentGreen, unfocusedBorderColor = Color(0xFF2D3A55),
                                        disabledTextColor = TextWhite, disabledBorderColor = Color(0xFF2D3A55), disabledLabelColor = TextMuted
                                    )
                                )
                            }
                            
                            Spacer(Modifier.height(10.dp))
                            val amountNum = amountText.toDoubleOrNull()
                            if (amountNum != null && amountNum > 212) {
                                val extraMonths = (amountNum / 212).toInt()
                                Text("💡 Rs. ${amountNum.toInt()} automatically covers $extraMonths months.", color = AccentGreen, fontSize = 12.sp)
                                Spacer(Modifier.height(8.dp))
                            }

                            Button(
                                onClick = {
                                    if (amountNum != null) { viewModel.addPayment(member.id, amountNum, selectedDate, paymentForMonth); amountText = "" }
                                },
                                enabled = amountNum != null && !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) { Text("Submit Payment", color = Color.White, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }

                item {
                    Text("Transaction History", fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                }

                if (member.payments.isEmpty()) {
                    item { Text("No payments recorded.", color = TextMuted, modifier = Modifier.padding(vertical = 16.dp)) }
                } else {
                    items(member.payments) { payment ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Delete Payment") },
                                text = { Text("Are you sure you want to delete this payment of Rs. ${payment.amount.toInt()}?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        payment.id?.let { viewModel.deletePayment(it) }
                                        showDeleteConfirm = false
                                    }) { Text("Delete", color = AccentRed) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                                },
                                containerColor = CardBg, titleContentColor = TextWhite, textContentColor = TextMuted
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Payment for ${payment.payment_for_month ?: "Unknown"}", fontWeight = FontWeight.SemiBold, color = TextWhite, fontSize = 14.sp)
                                    Text("Paid on ${payment.payment_date ?: payment.created_at?.take(10) ?: "-"}", color = TextMuted, fontSize = 12.sp)
                                }
                                Text("Rs. ${payment.amount.toInt()}", fontWeight = FontWeight.Bold, color = AccentGreen, fontSize = 16.sp)
                                Spacer(Modifier.width(12.dp))
                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
