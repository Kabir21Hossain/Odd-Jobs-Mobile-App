package com.oddjobs.app.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oddjobs.app.AppViewModel
import com.oddjobs.app.ui.screens.*
import com.oddjobs.app.ui.theme.OddJobsTheme
import kotlinx.coroutines.delay

@Composable
fun OddJobsApp(vm: AppViewModel) {
    val d by vm.state.collectAsState()
    val ctx = LocalContext.current
    val nav = rememberNavController()

    // The clock used for "5 min ago" labels; refreshed every 20 seconds.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(20_000)
        }
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }

    // Screens decide their start from the saved state (loaded synchronously by the repository).
    val start = remember {
        val me = d.me
        when {
            !d.langChosen -> Routes.WELCOME
            me == null -> Routes.LOGIN
            !me.profileDone -> Routes.SETUP
            else -> Routes.tabFor(d.mode)
        }
    }

    // When the user logs out or deletes the account, return to the login screen.
    LaunchedEffect(d.currentUserId) {
        if (d.currentUserId == null) {
            val route = nav.currentDestination?.route
            if (route != null && route !in setOf(Routes.WELCOME, Routes.LOGIN, Routes.OTP)) nav.resetTo(Routes.LOGIN)
        }
    }

    OddJobsTheme {
        CompositionLocalProvider(LocalLang provides d.lang, LocalNow provides now) {
            NavHost(navController = nav, startDestination = start) {
                composable(Routes.WELCOME) { WelcomeScreen(vm, nav) }
                composable(Routes.LOGIN) { LoginScreen(vm, nav) }
                composable(Routes.OTP) { OtpScreen(vm, nav) }
                composable(Routes.SETUP) { ProfileFormScreen(vm, nav, first = true, startSeeker = false) }
                composable(
                    Routes.EDIT, arguments = listOf(navArgument("seeker") { type = NavType.StringType })
                ) { e ->
                    ProfileFormScreen(vm, nav, first = false, startSeeker = e.arguments?.getString("seeker") == "true")
                }

                composable(Routes.HOME) { ClientHomeScreen(vm, nav) }
                composable(Routes.MYJOBS) { MyJobsScreen(vm, nav) }
                composable(Routes.FEED) { FeedScreen(vm, nav) }
                composable(Routes.MYWORK) { MyWorkScreen(vm, nav) }
                composable(Routes.CHATS) { ChatsScreen(vm, nav) }
                composable(Routes.PROFILE) { ProfileScreen(vm, nav) }

                composable(Routes.JOB, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    JobDetailScreen(vm, nav, e.arguments?.getString("id") ?: "")
                }
                composable(Routes.SEEKER, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    SeekerProfileScreen(vm, nav, e.arguments?.getString("id") ?: "")
                }
                composable(Routes.CHAT, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    ChatScreen(vm, nav, e.arguments?.getString("id") ?: "")
                }
                composable(Routes.WORKERS, listOf(navArgument("cat") { type = NavType.StringType })) { e ->
                    WorkersScreen(vm, nav, e.arguments?.getString("cat") ?: "all")
                }
                composable(
                    Routes.POST,
                    listOf(
                        navArgument("cat") { type = NavType.StringType; defaultValue = "" },
                        navArgument("invite") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { e ->
                    PostJobScreen(vm, nav, e.arguments?.getString("cat") ?: "", e.arguments?.getString("invite") ?: "")
                }
                composable(Routes.REVIEW, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    ReviewScreen(vm, nav, e.arguments?.getString("id") ?: "")
                }
                composable(Routes.RECEIPT, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    ReceiptScreen(vm, nav, e.arguments?.getString("id") ?: "")
                }
                composable(Routes.VERIFY) { VerificationScreen(vm, nav) }
                composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
                composable(Routes.NOTICES) { NoticesScreen(vm, nav) }
                composable(Routes.SAVED) { SavedScreen(vm, nav) }
                composable(Routes.EARNINGS) { EarningsScreen(vm, nav) }
                composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
                composable(Routes.FILTERS) { FiltersScreen(vm, nav) }
            }
        }
    }
}
