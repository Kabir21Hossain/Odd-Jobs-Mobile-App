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

import androidx.activity.compose.BackHandler

@Composable
fun ClientHomeScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    var showAll by remember { mutableStateOf(false) }
    val myJobs = Queries.clientJobs(d, me.id)
    val live = myJobs.filter {
        it.status == JobStatus.OPEN || it.isActive || (it.status == JobStatus.COMPLETED && !it.clientReviewed)
    }.take(3)
    val top = Queries.ranked(d, null, me.areaId, me.id, now).take(4)
    val cats = if (showAll) Catalog.categories else Catalog.categories.take(9)

    TabScaffold(nav, d, Routes.HOME, "Odd Jobs", actions = { BellAction(nav, d) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    tr("Hello, ${me.name.substringBefore(' ')} 👋", "আসসালামু আলাইকুম, ${me.name.substringBefore(' ')} 👋"),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold
                )
                Text(tr("What do you need help with today?", "আজ কী কাজে সাহায্য লাগবে?"), color = Muted)
            }
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = Primary) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text(
                            tr("Post a job in 30 seconds", "৩০ সেকেন্ডে কাজ পোস্ট করুন"),
                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tr("Verified workers near you will send offers.", "আপনার কাছের যাচাইকৃত কর্মীরা অফার পাঠাবে।"),
                            color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { nav.navigate(Routes.post()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Post a job", "কাজ পোস্ট করুন"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (live.isNotEmpty()) {
                item { SectionHeader(tr("Your jobs", "আপনার কাজ")) }
                items(live) { j ->
                    val hired = d.user(j.hiredSeekerId)
                    val badge = when {
                        j.status == JobStatus.OPEN -> tr("${j.applications.count { it.status != AppStatus.WITHDRAWN }} offers", "${digitsFor(d.lang, j.applications.count { it.status != AppStatus.WITHDRAWN }.toString())} টি অফার")
                        j.status == JobStatus.COMPLETED -> tr("Rate now", "রেটিং দিন")
                        else -> statusLabel(j.status) + (if (hired != null) " · " + hired.name else "")
                    }
                    JobCard(j, null, null, now, { nav.navigate(Routes.job(j.id)) }, badge = badge)
                }
            }
            item { SectionHeader(tr("Browse by category", "ক্যাটেগরি অনুযায়ী খুঁজুন")) }
            item {
                CategoryGrid(selected = emptySet(), onToggle = { nav.navigate(Routes.workers(it.id)) }, items = cats)
                TextButton(onClick = { showAll = !showAll }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showAll) tr("Show less", "কম দেখুন") else tr("See all categories", "সব ক্যাটেগরি দেখুন"))
                }
            }
            if (top.isNotEmpty()) {
                item { SectionHeader(tr("Top workers near you", "আপনার কাছের সেরা কর্মী")) }
                items(top) { r ->
                    SeekerCard(r, onClick = { nav.navigate(Routes.seeker(r.user.id)) })
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun WorkersScreen(vm: AppViewModel, nav: NavHostController, catId: String) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    var verifiedOnly by remember { mutableStateOf(false) }
    val cat = if (catId == "all") null else Catalog.category(catId)
    val list = Queries.ranked(d, cat?.id, me.areaId, me.id, now, onlyVerified = verifiedOnly)
    ScreenFrame(
        title = if (cat != null) cat.emoji + " " + cat.name(d.lang) else tr("Workers", "কর্মী"),
        onBack = { nav.popBackStack() }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = verifiedOnly, onClick = { verifiedOnly = !verifiedOnly },
                        label = { Text(tr("ID verified only", "শুধু আইডি যাচাইকৃত")) },
                        leadingIcon = { Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Spacer(Modifier.weight(1f))
                    Text(tr("${list.size} workers", "${digitsFor(d.lang, list.size.toString())} জন কর্মী"), color = Muted, fontSize = 13.sp)
                }
                Text(
                    tr("Ranked by trust, distance, experience and price fit.", "ট্রাস্ট, দূরত্ব, অভিজ্ঞতা ও দাম অনুযায়ী সাজানো।"),
                    color = Muted, fontSize = 12.sp
                )
            }
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        "🔍", tr("No workers found", "কোনো কর্মী পাওয়া যায়নি"),
                        tr("Post a job and workers will find you.", "কাজ পোস্ট করুন, কর্মীরা আপনাকে খুঁজে নেবে।"),
                        tr("Post a job", "কাজ পোস্ট করুন"), { nav.navigate(Routes.post(cat?.id ?: "")) }
                    )
                }
            }
            items(list) { r ->
                SeekerCard(
                    r, onClick = { nav.navigate(Routes.seeker(r.user.id)) },
                    favorite = r.user.id in me.favoriteSeekerIds, onFavorite = { vm.toggleFavorite(r.user.id) },
                    actions = {
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton(
                            tr("Invite to a job", "কাজে আমন্ত্রণ জানান"),
                            onClick = { nav.navigate(Routes.post(cat?.id ?: r.user.categories.firstOrNull() ?: "", r.user.id)) }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun PostJobScreen(vm: AppViewModel, nav: NavHostController, presetCat: String, invite: String) {
    val d by vm.state.collectAsState()
    val me = d.me ?: return
    val lang = d.lang
    var step by remember { mutableStateOf(if (presetCat.isNotEmpty()) 1 else 0) }
    var catId by remember { mutableStateOf(presetCat) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf(listOf<String>()) }
    var areaId by remember { mutableStateOf(me.areaId) }
    var landmark by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf(Urgency.TODAY) }
    var budget by remember {
        mutableStateOf(if (presetCat.isNotEmpty()) (((Catalog.category(presetCat).minPrice + Catalog.category(presetCat).maxPrice) / 2 / 50) * 50).toString() else "")
    }
    var negotiable by remember { mutableStateOf(true) }
    var pay by remember { mutableStateOf(PayMethod.CASH) }
    var onlyVerified by remember { mutableStateOf(false) }
    var minRating by remember { mutableStateOf(0.0) }
    var showArea by remember { mutableStateOf(false) }
    val invited = d.user(invite.ifEmpty { null })
    val total = 5
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importPhoto(uri) { p -> if (p != null) photos = (photos + p).take(3) }
    }
    BackHandler(enabled = step > 0) { step -= 1 }

    val budgetInt = budget.toIntOrNull() ?: 0
    val canNext = when (step) {
        0 -> catId.isNotEmpty()
        1 -> desc.trim().length >= 5
        2 -> true
        3 -> budgetInt >= 50
        else -> true
    }
    val stepTitle = listOf(
        tr("What do you need?", "কী কাজ লাগবে?"),
        tr("Describe the job", "কাজের বিবরণ"),
        tr("Where & when?", "কোথায় ও কখন?"),
        tr("Budget & payment", "বাজেট ও পেমেন্ট"),
        tr("Review & post", "দেখে নিয়ে পোস্ট করুন")
    )[step]

    ScreenFrame(
        title = tr("Post a job", "কাজ পোস্ট করুন"),
        onBack = { if (step > 0) step -= 1 else nav.popBackStack() },
        bottomBar = {
            Surface(color = CardColor, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (step > 0) SecondaryButton(tr("Back", "পিছনে"), { step -= 1 }, Modifier.weight(1f))
                    if (step < total - 1) {
                        PrimaryButton(tr("Next", "পরবর্তী"), { step += 1 }, Modifier.weight(1f), enabled = canNext)
                    } else {
                        PrimaryButton(
                            tr("Post job", "কাজ পোস্ট করুন"),
                            onClick = {
                                val out = vm.postJob(
                                    JobDraft(
                                        title = title, description = desc, categoryId = catId, areaId = areaId,
                                        landmark = landmark, photos = photos, urgency = urgency, budget = budgetInt,
                                        negotiable = negotiable, payMethod = pay, onlyVerified = onlyVerified,
                                        minRating = minRating, invitedSeekerId = invited?.id
                                    )
                                )
                                if (out.ok) {
                                    val id = out.value ?: ""
                                    nav.popBackStack()
                                    nav.navigate(Routes.job(id))
                                }
                            },
                            Modifier.weight(1f), icon = Icons.Filled.Check
                        )
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until total) {
                    Box(Modifier.weight(1f).height(5.dp).background(if (i <= step) Primary else Border, RoundedCornerShape(50)))
                }
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    tr("Step ${step + 1} of $total", "ধাপ ${digitsFor(lang, (step + 1).toString())} / ${digitsFor(lang, total.toString())}"),
                    color = Muted, fontSize = 12.sp
                )
                Text(stepTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                if (invited != null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = AccentLight) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(invited.name, invited.photoPath, 30.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Inviting ${invited.name} to apply", "${invited.name} কে আবেদনের আমন্ত্রণ"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                when (step) {
                    0 -> CategoryGrid(
                        selected = setOf(catId),
                        onToggle = { c ->
                            catId = c.id
                            if (budget.isBlank()) budget = (((c.minPrice + c.maxPrice) / 2 / 50) * 50).toString()
                        }
                    )
                    1 -> {
                        AppTextField(title, { title = it }, tr("Job title (optional)", "কাজের শিরোনাম (ঐচ্ছিক)"), maxLength = 60)
                        AppTextField(
                            desc, { desc = it }, tr("What needs to be done?", "কী কাজ করাতে হবে?"),
                            singleLine = false, minLines = 4, maxLength = 500,
                            supporting = tr("Be specific: problem, size, tools needed.", "স্পষ্ট লিখুন: সমস্যা, পরিমাণ, প্রয়োজনীয় সরঞ্জাম।")
                        )
                        Text(tr("Photos (optional, up to 3)", "ছবি (ঐচ্ছিক, সর্বোচ্চ ৩টি)"), fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (p in photos) {
                                Box(Modifier.size(90.dp)) {
                                    PhotoImage(p, Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                                    IconButton(
                                        onClick = { photos = photos - p },
                                        modifier = Modifier.align(Alignment.TopEnd).size(28.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    ) { Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                }
                            }
                            if (photos.size < 3) {
                                Surface(
                                    modifier = Modifier.size(90.dp).clickable {
                                        photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    shape = RoundedCornerShape(12.dp), color = CardColor, border = BorderStroke(1.dp, Border)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Primary)
                                        Text(tr("Add", "যোগ করুন"), fontSize = 11.sp, color = Muted)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        PickerField(tr("Job area", "কাজের এলাকা"), Catalog.area(areaId).full(lang), { showArea = true })
                        AppTextField(
                            landmark, { landmark = it }, tr("House, road & landmark", "বাসা, রাস্তা ও ল্যান্ডমার্ক"),
                            singleLine = false, minLines = 2, maxLength = 120,
                            supporting = tr("Shared only with the worker you hire.", "শুধু নিয়োগ পাওয়া কর্মীর সাথে শেয়ার হবে।")
                        )
                        Text(tr("When do you need it?", "কখন লাগবে?"), fontWeight = FontWeight.Bold)
                        TagChips(
                            Urgency.values().map { it.name }, setOf(urgency.name),
                            { urgency = Urgency.valueOf(it) }, { urgencyLabel(Urgency.valueOf(it)) }
                        )
                    }
                    3 -> {
                        val c = Catalog.category(catId)
                        AppTextField(
                            budget, { budget = it.filter { ch -> ch.isDigit() } }, tr("Your budget (৳)", "আপনার বাজেট (৳)"),
                            keyboard = KeyboardType.Number, maxLength = 7, prefix = "৳ ",
                            supporting = tr("Typical for ${c.en}: ", "${c.bn} এর সাধারণ দাম: ") + money(c.minPrice) + " – " + money(c.maxPrice)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Open to negotiation", "দামাদামি করা যাবে"), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Switch(checked = negotiable, onCheckedChange = { negotiable = it })
                        }
                        Text(tr("How will you pay?", "কীভাবে পেমেন্ট করবেন?"), fontWeight = FontWeight.Bold)
                        TagChips(
                            PayMethod.values().map { it.name }, setOf(pay.name),
                            { pay = PayMethod.valueOf(it) }, { payLabel(PayMethod.valueOf(it)) }
                        )
                        Text(
                            tr(
                                "You pay the worker directly after the job. The app records it for your history.",
                                "কাজ শেষে আপনি সরাসরি কর্মীকে টাকা দেবেন। অ্যাপ আপনার ইতিহাসে তা রেকর্ড রাখবে।"
                            ),
                            color = Muted, fontSize = 12.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Only ID-verified workers", "শুধু আইডি যাচাইকৃত কর্মী"), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Switch(checked = onlyVerified, onCheckedChange = { onlyVerified = it })
                        }
                        Text(tr("Minimum worker rating", "কর্মীর সর্বনিম্ন রেটিং"), fontWeight = FontWeight.Bold)
                        TagChips(
                            listOf("0", "4", "4.5"), setOf(if (minRating == 0.0) "0" else if (minRating == 4.0) "4" else "4.5"),
                            { minRating = it.toDouble() },
                            { if (it == "0") tr("Any", "যেকোনো") else digitsFor(lang, it) + "★+" }
                        )
                    }
                    else -> {
                        val c = Catalog.category(catId)
                        AppCard {
                            Text(c.emoji + " " + (title.ifBlank { c.name(lang) }), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(desc)
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = Border)
                            InfoRow(tr("Area", "এলাকা"), Catalog.area(areaId).full(lang))
                            if (landmark.isNotBlank()) InfoRow(tr("Landmark", "ল্যান্ডমার্ক"), landmark)
                            InfoRow(tr("When", "কখন"), urgencyLabel(urgency))
                            InfoRow(tr("Budget", "বাজেট"), money(budgetInt) + if (negotiable) " (" + tr("negotiable", "আলোচনা সাপেক্ষ") + ")" else "")
                            InfoRow(tr("Payment", "পেমেন্ট"), payLabel(pay))
                            if (onlyVerified) InfoRow(tr("Workers", "কর্মী"), tr("ID verified only", "শুধু আইডি যাচাইকৃত"))
                            if (photos.isNotEmpty()) InfoRow(tr("Photos", "ছবি"), num(photos.size))
                        }
                        Text(
                            tr(
                                "Your exact address is hidden until you hire someone. Workers only see the area.",
                                "আপনি কাউকে নিয়োগ না দেওয়া পর্যন্ত সঠিক ঠিকানা গোপন থাকে। কর্মীরা শুধু এলাকা দেখে।"
                            ),
                            color = Muted, fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (showArea) AreaPickerDialog(onSelect = { areaId = it; showArea = false }, onDismiss = { showArea = false })
}

@Composable
fun MyJobsScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    var tab by remember { mutableStateOf(0) }
    val all = Queries.clientJobs(d, me.id)
    val open = all.filter { it.status == JobStatus.OPEN }
    val active = all.filter { it.isActive }
    val past = all.filter { it.isClosed }
    val list = when (tab) { 0 -> open; 1 -> active; else -> past }
    TabScaffold(nav, d, Routes.MYJOBS, tr("My Jobs", "আমার কাজ"), actions = { BellAction(nav, d) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(selectedTabIndex = tab, containerColor = Canvas, contentColor = Primary) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(tr("Open", "খোলা") + " (" + num(open.size) + ")") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tr("Active", "চলমান") + " (" + num(active.size) + ")") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(tr("Past", "আগের") + " (" + num(past.size) + ")") })
            }
            if (list.isEmpty()) {
                EmptyState(
                    if (tab == 0) "📝" else if (tab == 1) "🛠️" else "🗂️",
                    when (tab) {
                        0 -> tr("No open jobs", "কোনো খোলা কাজ নেই")
                        1 -> tr("Nothing in progress", "কিছু চলমান নেই")
                        else -> tr("No past jobs yet", "এখনো আগের কোনো কাজ নেই")
                    },
                    tr("Post a job and get offers within minutes.", "কাজ পোস্ট করুন, কয়েক মিনিটেই অফার পাবেন।"),
                    tr("Post a job", "কাজ পোস্ট করুন"), { nav.navigate(Routes.post()) },
                    Modifier.padding(top = 40.dp)
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list) { j ->
                        val offers = j.applications.count { it.status != AppStatus.WITHDRAWN }
                        val badge = when {
                            j.status == JobStatus.OPEN -> tr("$offers offers", "${digitsFor(d.lang, offers.toString())} টি অফার")
                            j.status == JobStatus.COMPLETED && !j.clientReviewed -> tr("Rate now", "রেটিং দিন")
                            else -> statusLabel(j.status)
                        }
                        JobCard(
                            j, null, null, now, { nav.navigate(Routes.job(j.id)) },
                            badge = badge, badgeGood = j.status != JobStatus.CANCELLED && j.status != JobStatus.EXPIRED
                        )
                    }
                }
            }
        }
    }
}
