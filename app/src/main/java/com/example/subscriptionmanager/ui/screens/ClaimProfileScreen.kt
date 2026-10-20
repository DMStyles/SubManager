package com.example.subscriptionmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscriptionmanager.data.Member
import com.example.subscriptionmanager.ui.SubscriptionViewModel

@Composable
fun ClaimProfileScreen(viewModel: SubscriptionViewModel) {
    val unclaimedMembers by viewModel.unclaimedMembers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var selectedMember by remember { mutableStateOf<Member?>(null) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D0D0D), Color(0xFF1A1A2E)))
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Text("👤", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Who are you?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold, color = Color.White
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select your name from the plan to link your account",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF8A9BB5)),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            if (unclaimedMembers.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2740)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "All profiles have been claimed.\nContact your manager.",
                            color = Color(0xFF8A9BB5),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(unclaimedMembers) { member ->
                        val isSelected = selectedMember?.id == member.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMember = member }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color(0xFF1DB954) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1A2E20) else Color(0xFF1E2740)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFF1DB954) else Color(0xFF2D3A55),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        member.name.first().toString(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        member.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "Plan Member",
                                        color = Color(0xFF8A9BB5),
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                if (isSelected) {
                                    Text("✓", color = Color(0xFF1DB954), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { selectedMember?.let { viewModel.claimProfile(it.id) } },
                            enabled = selectedMember != null && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("This is me!", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }
    }
}
