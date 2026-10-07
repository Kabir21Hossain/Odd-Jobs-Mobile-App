@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.oddjobs.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oddjobs.app.AppViewModel
import com.oddjobs.app.domain.*
import com.oddjobs.app.ui.*
import com.oddjobs.app.ui.components.*
import com.oddjobs.app.ui.theme.Accent
import com.oddjobs.app.ui.theme.AccentLight
import com.oddjobs.app.ui.theme.Border
import com.oddjobs.app.ui.theme.Canvas
import com.oddjobs.app.ui.theme.CardColor
import com.oddjobs.app.ui.theme.ErrorBg
import com.oddjobs.app.ui.theme.ErrorRed
import com.oddjobs.app.ui.theme.Muted
import com.oddjobs.app.ui.theme.Primary
import com.oddjobs.app.ui.theme.PrimaryDark
import com.oddjobs.app.ui.theme.PrimaryLight
import com.oddjobs.app.ui.theme.Success
import com.oddjobs.app.ui.theme.SuccessBg
import com.oddjobs.app.ui.theme.Warn
import com.oddjobs.app.ui.theme.WarnBg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController

object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val OTP = "otp"
    const val SETUP = "setup"
    const val HOME = "home"
    const val MYJOBS = "myjobs"
    const val FEED = "feed"
    const val MYWORK = "mywork"
    const val CHATS = "chats"
    const val PROFILE = "profile"
    const val JOB = "job/{id}"
    const val SEEKER = "seeker/{id}"
    const val CHAT = "chat/{id}"
    const val POST = "post?cat={cat}&invite={invite}"
    const val WORKERS = "workers/{cat}"
    const val REVIEW = "review/{id}"
    const val RECEIPT = "receipt/{id}"
    const val EDIT = "editprofile/{seeker}"
    const val VERIFY = "verify"
    const val SETTINGS = "settings"
    const val NOTICES = "notices"
    const val SAVED = "saved"
    const val EARNINGS = "earnings"
    const val HISTORY = "history"
    const val FILTERS = "filters"

    fun job(id: String) = "job/$id"
    fun seeker(id: String) = "seeker/$id"
    fun chat(id: String) = "chat/$id"
    fun post(cat: String = "", invite: String = "") = "post?cat=$cat&invite=$invite"
    fun workers(cat: String) = "workers/$cat"
    fun review(id: String) = "review/$id"
    fun receipt(id: String) = "receipt/$id"
    fun edit(seeker: Boolean) = "editprofile/$seeker"

    fun tabFor(mode: Mode) = if (mode == Mode.SEEKER) FEED else HOME
}

fun NavHostController.goTab(route: String, from: String?) {
    navigate(route) {
        if (from != null) popUpTo(from) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavHostController.resetTo(route: String) {
    navigate(route) { popUpTo(graph.id) { inclusive = true } }
}

private class TabDef(val route: String, val icon: ImageVector, val en: String, val bn: String)

@Composable
fun AppBottomBar(nav: NavHostController, d: AppData, current: String) {
    val me = d.me
    val tabs = if (d.mode == Mode.CLIENT) listOf(
        TabDef(Routes.HOME, Icons.Filled.Home, "Home", "হোম"),
        TabDef(Routes.MYJOBS, Icons.Filled.Work, "My Jobs", "আমার কাজ"),
        TabDef(Routes.CHATS, Icons.Filled.Chat, "Messages", "বার্তা"),
        TabDef(Routes.PROFILE, Icons.Filled.Person, "Profile", "প্রোফাইল")
    ) else listOf(
        TabDef(Routes.FEED, Icons.Filled.Search, "Find Jobs", "কাজ খুঁজুন"),
        TabDef(Routes.MYWORK, Icons.Filled.Work, "My Work", "আমার কাজ"),
        TabDef(Routes.CHATS, Icons.Filled.Chat, "Messages", "বার্তা"),
        TabDef(Routes.PROFILE, Icons.Filled.Person, "Profile", "প্রোফাইল")
    )
    val unread = if (me != null) Queries.unreadChats(d, me.id) else 0
    NavigationBar(containerColor = CardColor) {
        for (t in tabs) {
            NavigationBarItem(
                selected = current == t.route,
                onClick = { if (current != t.route) nav.goTab(t.route, current) },
                icon = {
                    BadgedBox(badge = {
                        if (t.route == Routes.CHATS && unread > 0) Badge { Text(num(unread)) }
                    }) { Icon(t.icon, contentDescription = null) }
                },
                label = { Text(tr(t.en, t.bn), fontSize = 11.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary, selectedTextColor = Primary, indicatorColor = PrimaryLight,
                    unselectedIconColor = Muted, unselectedTextColor = Muted
                )
            )
        }
    }
}

@Composable
fun TabScaffold(
    nav: NavHostController,
    d: AppData,
    current: String,
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    ScreenFrame(
        title = title, onBack = null, actions = actions,
        bottomBar = { AppBottomBar(nav, d, current) }, content = content
    )
}

@Composable
fun BellAction(nav: NavHostController, d: AppData) {
    val n = Queries.unreadNotices(d, d.currentUserId ?: "")
    IconButton(onClick = { nav.navigate(Routes.NOTICES) }) {
        BadgedBox(badge = { if (n > 0) Badge { Text(num(n)) } }) {
            Icon(Icons.Filled.Notifications, contentDescription = tr("Notifications", "নোটিফিকেশন"))
        }
    }
}

@Composable
fun JobTimeline(job: Job) {
    val lang = LocalLang.current
    val reached = job.events.associate { it.status to it.at }
    Column {
        for (s in JobFlow.steps) {
            val at = reached[s]
            val done = at != null
            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(22.dp).background(if (done) Primary else Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    statusLabel(s),
                    fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                    color = if (done) MaterialTheme.colorScheme.onSurface else Muted,
                    modifier = Modifier.weight(1f)
                )
                if (at != null) Text(clockOf(at, lang), color = Muted, fontSize = 12.sp)
            }
        }
    }
}

fun dial(context: android.content.Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    } catch (_: Throwable) {
    }
}

fun statusColors(s: JobStatus): Pair<Color, Color> = when (s) {
    JobStatus.OPEN -> PrimaryLight to Primary
    JobStatus.COMPLETED -> SuccessBg to Success
    JobStatus.CANCELLED, JobStatus.EXPIRED -> ErrorBg to ErrorRed
    JobStatus.AWAITING_CONFIRMATION -> WarnBg to Warn
    else -> AccentLight to Accent
}
