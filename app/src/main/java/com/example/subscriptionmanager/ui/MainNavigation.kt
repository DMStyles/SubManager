package com.example.subscriptionmanager.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.subscriptionmanager.ui.screens.*

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val viewModel: SubscriptionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val route by viewModel.route.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkAuthAndRoute()
    }

    when (route) {
        is AppRoute.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AppRoute.Login -> {
            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(onLoginSuccess = {
                        viewModel.checkAuthAndRoute()
                    })
                }
            }
        }
        is AppRoute.ClaimProfile -> {
            NavHost(navController = navController, startDestination = "claim") {
                composable("claim") {
                    ClaimProfileScreen(viewModel = viewModel)
                }
            }
        }
        is AppRoute.Dashboard -> {
            val memberState by viewModel.currentUserState.collectAsStateWithLifecycle()
            if (memberState?.role == "manager") {
                ManagerDashboardScreen(
                    viewModel = viewModel,
                    onLogout = { viewModel.logout {} },
                    onMemberClick = { memberId -> viewModel.navigateTo(AppRoute.MemberDetail(memberId)) }
                )
            } else {
                MemberDashboardScreen(
                    viewModel = viewModel,
                    onLogout = { viewModel.logout {} }
                )
            }
        }
        is AppRoute.MemberDetail -> {
            val memberState by viewModel.currentUserState.collectAsStateWithLifecycle()
            ManagerMemberDetailScreen(
                viewModel = viewModel,
                memberId = (route as AppRoute.MemberDetail).memberId,
                onBack = { viewModel.navigateTo(AppRoute.Dashboard(memberState?.role ?: "manager")) }
            )
        }
    }
}
