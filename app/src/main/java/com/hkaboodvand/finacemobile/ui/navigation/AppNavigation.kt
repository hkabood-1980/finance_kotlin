package com.hkaboodvand.finacemobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hkaboodvand.finacemobile.ui.screens.HomeScreen
import com.hkaboodvand.finacemobile.ui.screens.AddCardScreen
import com.hkaboodvand.finacemobile.ui.screens.CardListScreen
import com.hkaboodvand.finacemobile.ui.screens.IncomeScreen
import com.hkaboodvand.finacemobile.ui.screens.ExpenseScreen
import com.hkaboodvand.finacemobile.ui.screens.TransferScreen
import com.hkaboodvand.finacemobile.ui.screens.SavingScreen
import com.hkaboodvand.finacemobile.ui.screens.ReportsScreen
import com.hkaboodvand.finacemobile.ui.screens.SmsScreen

object Routes {
    const val HOME = "home"
    const val CARD_LIST = "card_list"
    const val ADD_CARD = "add_card"
    const val INCOME = "income"
    const val EXPENSE = "expense"
    const val TRANSFER = "transfer"
    const val SAVING = "saving"
    const val REPORTS = "reports"
    const val SMS = "sms"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(onNavigate = { route -> navController.navigate(route) })
        }

        composable(Routes.CARD_LIST) {
            CardListScreen(
                onBack = { navController.popBackStack() },
                onAddCard = { navController.navigate(Routes.ADD_CARD) }
            )
        }

        composable(Routes.ADD_CARD) {
            AddCardScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Routes.INCOME) {
            IncomeScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Routes.EXPENSE) {
            ExpenseScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Routes.TRANSFER) {
            TransferScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Routes.SAVING) {
            SavingScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SMS) {
            SmsScreen(onBack = { navController.popBackStack() })
        }
    }
}