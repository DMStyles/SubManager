package com.example.subscriptionmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscriptionmanager.ui.screens.*

@Composable
fun MainNavigation() {
    val viewModel: SubscriptionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val route by viewModel.route.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkAuthAndRoute()
    }

    when (val r = route) {
        is AppRoute.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
            }
        }

        is AppRoute.Login -> {
            LoginScreen(onLoginSuccess = { viewModel.checkAuthAndRoute() })
        }

        is AppRoute.ClaimProfile -> {
            ClaimProfileScreen(viewModel = viewModel)
        }

        is AppRoute.SubscriptionList -> {
            SubscriptionListScreen(
                viewModel = viewModel,
                onLogout = { viewModel.logout {} }
            )
        }

        is AppRoute.CreateSubscription -> {
            CreateSubscriptionScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(AppRoute.SubscriptionList) }
            )
        }

        is AppRoute.JoinSubscription -> {
            JoinSubscriptionScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(AppRoute.SubscriptionList) }
            )
        }

        is AppRoute.EditSubscription -> {
            EditSubscriptionScreen(
                viewModel = viewModel,
                subscriptionId = r.subscriptionId,
                onBack = { viewModel.navigateTo(AppRoute.Dashboard(r.subscriptionId, "admin")) }
            )
        }

        is AppRoute.Dashboard -> {
            val memberState by viewModel.currentUserState.collectAsStateWithLifecycle()
            if (r.role == "admin") {
                ManagerDashboardScreen(
                    viewModel = viewModel,
                    subscriptionId = r.subscriptionId,
                    onLogout = { viewModel.logout {} },
                    onMemberClick = { memberId -> viewModel.navigateTo(AppRoute.MemberDetail(memberId, r.subscriptionId)) },
                    onEditSubscription = { viewModel.navigateTo(AppRoute.EditSubscription(r.subscriptionId)) },
                    onBack = { viewModel.navigateTo(AppRoute.SubscriptionList) }
                )
            } else {
                MemberDashboardScreen(
                    viewModel = viewModel,
                    subscriptionId = r.subscriptionId,
                    onLogout = { viewModel.logout {} },
                    onBack = { viewModel.navigateTo(AppRoute.SubscriptionList) }
                )
            }
        }

        is AppRoute.MemberDetail -> {
            ManagerMemberDetailScreen(
                viewModel = viewModel,
                memberId = r.memberId,
                subscriptionId = r.subscriptionId,
                onBack = { viewModel.navigateTo(AppRoute.Dashboard(r.subscriptionId, "admin")) }
            )
        }
    }
}
