package com.hkaboodvand.finacemobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hkaboodvand.finacemobile.ui.screens.AddCardScreen
import com.hkaboodvand.finacemobile.ui.screens.BackupScreen
import com.hkaboodvand.finacemobile.ui.screens.CardListScreen
import com.hkaboodvand.finacemobile.ui.screens.ExpenseScreen
import com.hkaboodvand.finacemobile.ui.screens.HomeScreen
import com.hkaboodvand.finacemobile.ui.screens.IncomeScreen
import com.hkaboodvand.finacemobile.ui.screens.ReportsScreen
import com.hkaboodvand.finacemobile.ui.screens.SavingScreen
import com.hkaboodvand.finacemobile.ui.screens.SmsScreen
import com.hkaboodvand.finacemobile.ui.screens.TransferScreen

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
    const val BACKUP = "backup"
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
                onAddCard = { navController.navigate(Routes.ADD_CARD) },
                onEditCard = { accountId ->
                    navController.navigate("edit_card/$accountId")
                }
            )
        }

        composable(
            route = "edit_card/{accountId}",
            arguments = listOf(
                navArgument("accountId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getLong("accountId")
            AddCardScreen(
                accountId = accountId,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(
                        route = Routes.CARD_LIST,
                        inclusive = false
                    )
                }
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

        composable(Routes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
    }
}