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
import androidx.compose.foundation.horizontalScroll
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
import androidx.navigation.NavHostController
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

import androidx.compose.foundation.lazy.LazyRow

@Composable
fun ChatsScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    val chats = Queries.chats(d, me.id)
    TabScaffold(nav, d, Routes.CHATS, tr("Messages", "বার্তা"), actions = { BellAction(nav, d) }) { pad ->
        if (chats.isEmpty()) {
            EmptyState(
                "💬", tr("No messages yet", "এখনো কোনো বার্তা নেই"),
                tr("Chats start when you send an offer, receive one, or ask a question on a job.", "অফার পাঠালে, পেলে বা কাজ নিয়ে প্রশ্ন করলে চ্যাট শুরু হয়।"),
                modifier = Modifier.padding(pad).padding(top = 40.dp)
            )
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(chats, key = { it.chatId }) { c ->
                    AppCard(onClick = { nav.navigate(Routes.chat(c.chatId)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(c.other.name, c.other.photoPath, 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(c.other.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(ago(c.last.at, now), color = Muted, fontSize = 11.sp)
                                }
                                Text(c.job.title, color = Primary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    (if (c.last.senderId == me.id) tr("You: ", "আপনি: ") else "") + c.last.text,
                                    color = if (c.unread > 0) MaterialTheme.colorScheme.onSurface else Muted,
                                    fontWeight = if (c.unread > 0) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp
                                )
                            }
                            if (c.unread > 0) {
                                Spacer(Modifier.width(8.dp))
                                Badge { Text(num(c.unread)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(vm: AppViewModel, nav: NavHostController, chatId: String) {
    val d by vm.state.collectAsState()
    val ctx = LocalContext.current
    val me = d.me ?: return
    val job = d.job(jobIdOfChat(chatId))
    val seekerId = seekerIdOfChat(chatId)
    val otherId = if (job != null && me.id == job.clientId) seekerId else job?.clientId
    val other = d.user(otherId)
    if (job == null || other == null) {
        ScreenFrame(title = tr("Chat", "চ্যাট"), onBack = { nav.popBackStack() }) { pad ->
            EmptyState("🤷", tr("Chat not found", "চ্যাট পাওয়া যায়নি"), modifier = Modifier.padding(pad))
        }
        return
    }
    val msgs = d.messages.filter { it.chatId == chatId }.sortedBy { it.at }
    val listState = rememberLazyListState()
    var text by remember { mutableStateOf("") }
    var showReport by remember { mutableStateOf(false) }
    val canCall = job.hiredSeekerId == seekerId && job.status != JobStatus.CANCELLED && job.status != JobStatus.OPEN
    val amSeeker = me.id == seekerId

    LaunchedEffect(msgs.size) {
        vm.markChatRead(chatId)
        if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1)
    }

    val quick = if (amSeeker) listOf(
        tr("I'm on my way", "আমি রওনা দিয়েছি"), tr("Running 10 min late", "১০ মিনিট দেরি হবে"),
        tr("What is the exact address?", "সঠিক ঠিকানা কোথায়?"), tr("Job done", "কাজ শেষ")
    ) else listOf(
        tr("What is your price?", "আপনার দাম কত?"), tr("When can you come?", "কখন আসতে পারবেন?"),
        tr("Please come now", "এখনই আসুন"), tr("Thank you!", "ধন্যবাদ!")
    )

    ScreenFrame(
        title = other.name, onBack = { nav.popBackStack() },
        actions = {
            if (canCall) IconButton(onClick = { dial(ctx, other.phone) }) { Icon(Icons.Filled.Phone, contentDescription = tr("Call", "কল"), tint = Primary) }
            IconButton(onClick = { showReport = true }) { Icon(Icons.Filled.Flag, contentDescription = tr("Report", "রিপোর্ট")) }
        },
        bottomBar = {
            Surface(color = CardColor, shadowElevation = 8.dp) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quick) { q ->
                            SuggestionChip(onClick = { text = q }, label = { Text(q, fontSize = 12.sp) })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = text, onValueChange = { if (it.length <= 1000) text = it },
                            modifier = Modifier.weight(1f), maxLines = 4,
                            placeholder = { Text(tr("Type a message", "বার্তা লিখুন")) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Border)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                val out = vm.send(chatId, text)
                                if (out.ok) text = ""
                            },
                            enabled = text.isNotBlank(),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary)
                        ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = tr("Send", "পাঠান"), tint = Color.White) }
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.job(job.id)) },
                color = PrimaryLight
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Catalog.category(job.categoryId).emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(job.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(money(job.budget), color = Primary, fontWeight = FontWeight.ExtraBold)
                }
            }
            if (msgs.isEmpty()) {
                EmptyState("👋", tr("Say hello", "সালাম দিন"), tr("Ask about price, timing or the work. Keep chats polite.", "দাম, সময় বা কাজ নিয়ে জিজ্ঞাসা করুন। ভদ্রভাবে কথা বলুন।"), modifier = Modifier.padding(top = 30.dp))
            }
            LazyColumn(
                state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(msgs, key = { it.id }) { m ->
                    val mine = m.senderId == me.id
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = if (mine) 16.dp else 4.dp, bottomEnd = if (mine) 4.dp else 16.dp
                            ),
                            color = if (mine) Primary else CardColor,
                            border = if (mine) null else BorderStroke(1.dp, Border),
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(m.text, color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface)
                                Text(
                                    clockOf(m.at, d.lang), fontSize = 10.sp,
                                    color = if (mine) Color.White.copy(alpha = 0.7f) else Muted,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showReport) {
        ReportDialog(onDismiss = { showReport = false }, onSubmit = {
            vm.report("chat", chatId, it); showReport = false
            vm.toast(if (d.lang == "bn") "রিপোর্ট পাঠানো হয়েছে। ধন্যবাদ।" else "Report sent. Thank you.")
        })
    }
}

@Composable
fun NoticesScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    val list = d.notices.filter { it.userId == me.id }
    LaunchedEffect(Unit) {
        delay(1200)
        vm.markNoticesRead()
    }
    ScreenFrame(title = tr("Notifications", "নোটিফিকেশন"), onBack = { nav.popBackStack() }) { pad ->
        if (list.isEmpty()) {
            EmptyState("🔔", tr("You're all caught up", "সব দেখা হয়ে গেছে"), tr("Offers, messages and job updates will show up here.", "অফার, বার্তা ও কাজের আপডেট এখানে দেখা যাবে।"), modifier = Modifier.padding(pad).padding(top = 40.dp))
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { n ->
                    AppCard(onClick = {
                        if (n.chatId != null) nav.navigate(Routes.chat(n.chatId!!))
                        else if (n.jobId != null && d.job(n.jobId) != null) nav.navigate(Routes.job(n.jobId!!))
                    }) {
                        Row(verticalAlignment = Alignment.Top) {
                            if (!n.read) {
                                Box(Modifier.padding(top = 6.dp).size(8.dp).background(Accent, CircleShape))
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(n.title(d.lang), fontWeight = FontWeight.Bold)
                                Text(n.body(d.lang), color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(ago(n.at, now), color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewScreen(vm: AppViewModel, nav: NavHostController, jobId: String) {
    val d by vm.state.collectAsState()
    val me = d.me ?: return
    val job = d.job(jobId) ?: return
    val isClient = job.clientId == me.id
    val target = d.user(if (isClient) job.hiredSeekerId else job.clientId)
    var stars by remember { mutableStateOf(5) }
    var tags by remember { mutableStateOf(setOf<String>()) }
    var comment by remember { mutableStateOf("") }
    val shown = when {
        stars >= 4 -> Ranking.positiveTags
        stars <= 2 -> Ranking.negativeTags
        else -> Ranking.positiveTags + Ranking.negativeTags
    }
    ScreenFrame(title = tr("Rate & review", "রেটিং ও রিভিউ"), onBack = { nav.popBackStack() }) { pad ->
        Column(
            Modifier.padding(pad).padding(20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Avatar(target?.name ?: "?", target?.photoPath, 80.dp)
            Text(
                tr("How was ${target?.name ?: ""}?", "${target?.name ?: ""} কেমন ছিলেন?"),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
            )
            Text(job.title, color = Muted)
            RatingInput(stars) { stars = it; tags = tags.filter { t -> t in (if (it >= 4) Ranking.positiveTags else if (it <= 2) Ranking.negativeTags else Ranking.positiveTags + Ranking.negativeTags) }.toSet() }
            Text(
                when (stars) {
                    5 -> tr("Excellent!", "চমৎকার!")
                    4 -> tr("Good", "ভালো")
                    3 -> tr("Okay", "মোটামুটি")
                    2 -> tr("Poor", "খারাপ")
                    else -> tr("Very bad", "অত্যন্ত খারাপ")
                },
                fontWeight = FontWeight.Bold, color = Accent
            )
            TagChips(shown, tags, { t -> tags = if (t in tags) tags - t else tags + t }, { tagLabel(it) })
            AppTextField(
                comment, { comment = it }, tr("Write a comment (optional)", "মন্তব্য লিখুন (ঐচ্ছিক)"),
                singleLine = false, minLines = 3, maxLength = 500
            )
            Text(
                tr("Your honest review helps others and decides how workers are ranked.", "আপনার সৎ রিভিউ অন্যদের সাহায্য করে এবং কর্মীদের র‍্যাংকিং নির্ধারণ করে।"),
                color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center
            )
            PrimaryButton(tr("Submit review", "রিভিউ জমা দিন"), onClick = {
                val out = vm.review(jobId, stars, tags.toList(), comment)
                if (out.ok) {
                    vm.toast(if (d.lang == "bn") "ধন্যবাদ! রিভিউ জমা হয়েছে।" else "Thanks! Your review was submitted.")
                    nav.popBackStack()
                }
            })
            TextButton(onClick = { nav.popBackStack() }) { Text(tr("Maybe later", "পরে দেব")) }
        }
    }
}

@Composable
fun ReceiptScreen(vm: AppViewModel, nav: NavHostController, jobId: String) {
    val d by vm.state.collectAsState()
    val ctx = LocalContext.current
    val job = d.job(jobId)
    val lang = d.lang
    ScreenFrame(title = tr("Receipt", "রসিদ"), onBack = { nav.popBackStack() }) { pad ->
        if (job == null) {
            EmptyState("🤷", tr("Receipt not found", "রসিদ পাওয়া যায়নি"), modifier = Modifier.padding(pad))
            return@ScreenFrame
        }
        val client = d.user(job.clientId)
        val worker = d.user(job.hiredSeekerId)
        val code = "OJ-" + job.id.takeLast(6).uppercase()
        Column(Modifier.padding(pad).padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard {
                Text("Odd Jobs", color = Primary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text(tr("Job receipt", "কাজের রসিদ") + " · " + code, color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Border)
                InfoRow(tr("Job", "কাজ"), job.title)
                InfoRow(tr("Category", "ক্যাটেগরি"), Catalog.category(job.categoryId).name(lang))
                InfoRow(tr("Area", "এলাকা"), Catalog.area(job.areaId).full(lang))
                InfoRow(tr("Client", "ক্লায়েন্ট"), client?.name ?: "—")
                InfoRow(tr("Worker", "কর্মী"), worker?.name ?: "—")
                InfoRow(tr("Completed", "সম্পন্ন"), if (job.completedAt > 0) clockOf(job.completedAt, lang) else "—")
                InfoRow(tr("Payment method", "পেমেন্ট মাধ্যম"), payLabel(job.payMethod))
                HorizontalDivider(color = Border)
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("Amount paid", "পরিশোধিত"), fontWeight = FontWeight.Bold)
                    Text(money(job.agreedPrice), fontWeight = FontWeight.ExtraBold, color = Primary, fontSize = 22.sp)
                }
                if (job.paidConfirmed) Text("✅ " + tr("Payment recorded", "পেমেন্ট রেকর্ড হয়েছে"), color = Success, fontSize = 13.sp)
            }
            SecondaryButton(tr("Share receipt", "রসিদ শেয়ার করুন"), onClick = {
                val body = "Odd Jobs receipt $code\n${job.title}\n${client?.name ?: ""} → ${worker?.name ?: ""}\n" +
                    "BDT ${job.agreedPrice} (${job.payMethod.name.lowercase()})"
                val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, body) }
                try { ctx.startActivity(Intent.createChooser(i, null)) } catch (_: Throwable) {}
            }, icon = Icons.Filled.Share)
        }
    }
}

@Composable
fun HistoryScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    var filter by remember { mutableStateOf("all") }
    val mine = d.jobs.filter { (it.clientId == me.id || it.hiredSeekerId == me.id) && it.isClosed }
        .sortedByDescending { it.lastEventAt }
    val list = when (filter) {
        "done" -> mine.filter { it.status == JobStatus.COMPLETED }
        "cancelled" -> mine.filter { it.status != JobStatus.COMPLETED }
        else -> mine
    }
    val spent = mine.filter { it.clientId == me.id && it.status == JobStatus.COMPLETED }.sumOf { it.agreedPrice }
    val earned = mine.filter { it.hiredSeekerId == me.id && it.status == JobStatus.COMPLETED }.sumOf { it.agreedPrice }
    ScreenFrame(title = tr("Job history", "কাজের ইতিহাস"), onBack = { nav.popBackStack() }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(tr("Jobs", "কাজ"), num(mine.size), Modifier.weight(1f))
                    StatBox(tr("Spent", "খরচ"), money(spent), Modifier.weight(1f))
                    if (me.isSeeker) StatBox(tr("Earned", "আয়"), money(earned), Modifier.weight(1f))
                }
            }
            item {
                TagChips(
                    listOf("all", "done", "cancelled"), setOf(filter), { filter = it },
                    { when (it) { "all" -> tr("All", "সব"); "done" -> tr("Completed", "সম্পন্ন"); else -> tr("Cancelled", "বাতিল") } }
                )
            }
            if (list.isEmpty()) {
                item { EmptyState("🗂️", tr("No history yet", "এখনো কোনো ইতিহাস নেই"), tr("Completed and cancelled jobs appear here.", "সম্পন্ন ও বাতিল কাজ এখানে দেখা যাবে।")) }
            }
            items(list, key = { it.id }) { j ->
                val role = if (j.clientId == me.id) tr("As client", "ক্লায়েন্ট হিসেবে") else tr("As worker", "কর্মী হিসেবে")
                JobCard(
                    j, null, null, now, { nav.navigate(Routes.job(j.id)) },
                    badge = role + " · " + statusLabel(j.status), badgeGood = j.status == JobStatus.COMPLETED
                )
            }
        }
    }
}

private const val SUPPORT_EMAIL = "support@oddjobs.example" // TODO: replace with your real support address before launch

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val ctx = LocalContext.current
    val me = d.me
    var showLogout by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var legal by remember { mutableStateOf<String?>(null) }
    var faqOpen by remember { mutableStateOf(-1) }
    val faqs = listOf(
        ("How do I hire someone?" to "কীভাবে কাউকে নিয়োগ দেব?") to
            ("Tap Post a job, describe the work and budget. Workers near you send offers. Compare their ratings and prices, then tap Hire." to "কাজ পোস্ট করুন, কাজ ও বাজেট লিখুন। কাছের কর্মীরা অফার পাঠাবে। রেটিং ও দাম দেখে নিয়োগ দিন।"),
        ("How does the arrival code work?" to "আগমন কোড কীভাবে কাজ করে?") to
            ("When you hire someone you get a 4-digit code. Tell it to the worker only when they arrive. This proves they really came." to "নিয়োগ দিলে ৪ সংখ্যার কোড পাবেন। কর্মী পৌঁছালে তবেই কোডটি বলুন। এতে প্রমাণ হয় তিনি সত্যিই এসেছেন।"),
        ("How do I pay?" to "কীভাবে টাকা দেব?") to
            ("Pay the worker directly (cash, bKash or Nagad) after the job. Then confirm completion in the app so it is recorded in your history." to "কাজ শেষে সরাসরি কর্মীকে টাকা দিন (নগদ, বিকাশ বা নগদ)। এরপর অ্যাপে সম্পন্ন নিশ্চিত করুন, ইতিহাসে রেকর্ড থাকবে।"),
        ("How are workers ranked?" to "কর্মীদের র‍্যাংকিং কীভাবে হয়?") to
            ("By a Trust Score from ratings and comments, completed jobs, reply speed, ID verification, distance and price fit." to "রেটিং ও মন্তব্য, সম্পন্ন কাজ, উত্তরের গতি, আইডি যাচাই, দূরত্ব ও দামের মিল থেকে ট্রাস্ট স্কোর তৈরি হয়।"),
        ("Why must workers verify their ID?" to "কর্মীদের কেন আইডি যাচাই করতে হয়?") to
            ("To keep the community safe. Only ID-verified workers can apply for jobs." to "কমিউনিটি নিরাপদ রাখতে। শুধু আইডি যাচাইকৃত কর্মীরা কাজে আবেদন করতে পারেন।")
    )
    ScreenFrame(title = tr("Settings & help", "সেটিংস ও সহায়তা"), onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard {
                Text(tr("Language", "ভাষা"), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                TagChips(listOf("bn", "en"), setOf(d.lang), { vm.chooseLang(it) }, { if (it == "bn") "বাংলা" else "English" })
            }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Demo mode", "ডেমো মোড"), fontWeight = FontWeight.Bold)
                        Text(
                            tr(
                                "Simulates workers, clients, SMS and ID review so you can try everything on one phone. A real backend replaces this.",
                                "এক ফোনেই সব চেষ্টা করতে কর্মী, ক্লায়েন্ট, SMS ও আইডি রিভিউ সিমুলেট করে। আসল ব্যাকএন্ড এটি প্রতিস্থাপন করবে।"
                            ),
                            color = Muted, fontSize = 12.sp
                        )
                    }
                    Switch(checked = d.demoSim, onCheckedChange = { vm.setDemo(it) })
                }
            }
            if (me != null && me.blockedIds.isNotEmpty()) {
                AppCard {
                    Text(tr("Blocked users", "ব্লক করা ব্যবহারকারী"), fontWeight = FontWeight.Bold)
                    for (bid in me.blockedIds) {
                        val u = d.user(bid)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Text(u?.name ?: bid, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.unblock(bid) }) { Text(tr("Unblock", "আনব্লক")) }
                        }
                    }
                }
            }
            AppCard {
                Text(tr("Help & FAQ", "সহায়তা ও সাধারণ প্রশ্ন"), fontWeight = FontWeight.Bold)
                faqs.forEachIndexed { i, (q, a) ->
                    Column(Modifier.fillMaxWidth().clickable { faqOpen = if (faqOpen == i) -1 else i }.padding(vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tr(q.first, q.second), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Icon(if (faqOpen == i) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Muted)
                        }
                        if (faqOpen == i) {
                            Spacer(Modifier.height(4.dp))
                            Text(tr(a.first, a.second), color = Muted, fontSize = 13.sp)
                        }
                    }
                    HorizontalDivider(color = Border)
                }
            }
            AppCard {
                Column {
                    MenuRow(Icons.Filled.Info, tr("Contact support", "সহায়তা নিন"), {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL")))
                        } catch (_: Throwable) {}
                    })
                    HorizontalDivider(color = Border)
                    MenuRow(Icons.Filled.Lock, tr("Terms of Service", "শর্তাবলী"), { legal = "terms" })
                    HorizontalDivider(color = Border)
                    MenuRow(Icons.Filled.Shield, tr("Privacy Policy", "গোপনীয়তা নীতি"), { legal = "privacy" })
                    HorizontalDivider(color = Border)
                    MenuRow(Icons.Filled.Info, tr("About Odd Jobs", "অড জবস সম্পর্কে"), { legal = "about" }, "v1.0.0")
                }
            }
            if (me != null) {
                SecondaryButton(tr("Log out", "লগ আউট"), { showLogout = true }, icon = Icons.Filled.PowerSettingsNew)
                SecondaryButton(tr("Delete my account", "আমার অ্যাকাউন্ট মুছুন"), { showDelete = true }, danger = true, icon = Icons.Filled.Delete)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (showLogout) {
        ConfirmDialog(
            tr("Log out?", "লগ আউট করবেন?"), tr("You can sign back in using your phone, email, or Google account.", "ফোন, ইমেইল বা Google অ্যাকাউন্ট দিয়ে আবার সাইন ইন করতে পারবেন।"),
            tr("Log out", "লগ আউট"), onConfirm = { showLogout = false; vm.logout() }, onDismiss = { showLogout = false }
        )
    }
    if (showDelete) {
        ConfirmDialog(
            tr("Delete your account?", "অ্যাকাউন্ট মুছবেন?"),
            tr("Your profile, jobs, messages and reviews will be permanently removed. This cannot be undone.", "আপনার প্রোফাইল, কাজ, বার্তা ও রিভিউ স্থায়ীভাবে মুছে যাবে। এটি ফেরানো যাবে না।"),
            tr("Delete forever", "চিরতরে মুছুন"), onConfirm = { showDelete = false; vm.deleteAccount() },
            onDismiss = { showDelete = false }, danger = true
        )
    }
    if (legal != null) {
        val which = legal
        AlertDialog(
            onDismissRequest = { legal = null },
            title = {
                Text(when (which) { "terms" -> tr("Terms of Service", "শর্তাবলী"); "privacy" -> tr("Privacy Policy", "গোপনীয়তা নীতি"); else -> "Odd Jobs" })
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        when (which) {
                            "terms" -> tr(
                                "DRAFT — must be reviewed by a Bangladeshi lawyer before launch.\n\n1. Odd Jobs connects clients with independent workers. We are not the employer of any worker.\n2. You must be 18+ and give accurate information.\n3. Be respectful. Fraud, harassment, illegal jobs and off-platform scams are banned and lead to suspension.\n4. Agree the price before work starts. Payment is made directly between client and worker.\n5. Reviews must be honest and based on a real completed job.",
                                "খসড়া — চালুর আগে বাংলাদেশি আইনজীবীর পর্যালোচনা প্রয়োজন।\n\n১. অড জবস ক্লায়েন্টদের স্বাধীন কর্মীদের সাথে যুক্ত করে। আমরা কোনো কর্মীর নিয়োগকর্তা নই।\n২. আপনার বয়স ১৮+ হতে হবে এবং সঠিক তথ্য দিতে হবে।\n৩. ভদ্র আচরণ করুন। প্রতারণা, হয়রানি, বেআইনি কাজ নিষিদ্ধ এবং অ্যাকাউন্ট স্থগিত হতে পারে।\n৪. কাজ শুরুর আগে দাম ঠিক করুন। পেমেন্ট ক্লায়েন্ট ও কর্মীর মধ্যে সরাসরি হয়।\n৫. রিভিউ হতে হবে সৎ এবং সত্যিকারের সম্পন্ন কাজের ভিত্তিতে।"
                            )
                            "privacy" -> tr(
                                "DRAFT — must be reviewed by a Bangladeshi lawyer before launch.\n\nWe collect: phone number, name, area, photos, job and chat data, and ID documents for verification. We use them to run the service, keep people safe and rank workers fairly. Your exact address is shared only with the person you hire. ID documents are visible only to the verification team. You can delete your account and data any time in Settings.",
                                "খসড়া — চালুর আগে বাংলাদেশি আইনজীবীর পর্যালোচনা প্রয়োজন।\n\nআমরা সংগ্রহ করি: ফোন নম্বর, নাম, এলাকা, ছবি, কাজ ও চ্যাটের তথ্য এবং যাচাইয়ের জন্য আইডি কাগজ। সেবা চালাতে, নিরাপত্তা রক্ষা করতে ও কর্মীদের ন্যায্য র‍্যাংকিংয়ে এগুলো ব্যবহার হয়। আপনার সঠিক ঠিকানা শুধু নিয়োগ পাওয়া ব্যক্তির সাথে শেয়ার হয়। আইডি কাগজ শুধু যাচাই টিম দেখে। সেটিংস থেকে যেকোনো সময় অ্যাকাউন্ট ও তথ্য মুছতে পারবেন।"
                            )
                            else -> tr(
                                "Odd Jobs v1.0.0\nTrusted local help for Bangladesh.\n\nThis build runs fully on your phone with demo data. Connect a backend (see docs/ARCHITECTURE.md) to go live.",
                                "অড জবস v১.০.০\nবাংলাদেশের জন্য বিশ্বস্ত স্থানীয় কর্মী।\n\nএই সংস্করণ ডেমো ডেটা নিয়ে সম্পূর্ণ আপনার ফোনেই চলে। লাইভ করতে ব্যাকএন্ড যুক্ত করুন (docs/ARCHITECTURE.md দেখুন)।"
                            )
                        },
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = { TextButton(onClick = { legal = null }) { Text(tr("Close", "বন্ধ করুন")) } }
        )
    }
}
