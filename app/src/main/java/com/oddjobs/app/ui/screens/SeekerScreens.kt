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

import androidx.compose.foundation.lazy.LazyRow
import kotlinx.coroutines.flow.update

@Composable
fun SeekerBanners(d: AppData, nav: NavHostController) {
    val me = d.me ?: return
    if (!me.isSeeker) {
        AppCard(onClick = { nav.navigate(Routes.edit(true)) }) {
            Text(tr("Create your worker profile", "আপনার কর্মী প্রোফাইল তৈরি করুন"), fontWeight = FontWeight.ExtraBold)
            Text(tr("Add your skills and rates to start applying.", "দক্ষতা ও রেট যোগ করে আবেদন শুরু করুন।"), color = Muted, fontSize = 13.sp)
        }
    } else if (me.verifyStatus != VerifyStatus.APPROVED) {
        Surface(shape = RoundedCornerShape(16.dp), color = WarnBg, modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.VERIFY) }) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Warn)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (me.verifyStatus == VerifyStatus.SUBMITTED) tr("Verification in review", "যাচাই পর্যালোচনায় আছে")
                        else tr("Verify your identity to apply", "আবেদনের জন্য পরিচয় যাচাই করুন"),
                        fontWeight = FontWeight.Bold
                    )
                    Text(tr("Browsing is open. Applying needs ID verification.", "ব্রাউজ করা যাবে। আবেদনের জন্য আইডি যাচাই লাগবে।"), fontSize = 12.sp, color = Muted)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Warn)
            }
        }
    }
}

@Composable
fun FeedScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val f by vm.filter.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    val feed = Queries.feed(d, me, f, now)
    TabScaffold(
        nav, d, Routes.FEED, tr("Find Jobs", "কাজ খুঁজুন"),
        actions = {
            IconButton(onClick = { nav.navigate(Routes.FILTERS) }) {
                BadgedBox(badge = { if (f.activeCount > 0) Badge { Text(num(f.activeCount)) } }) {
                    Icon(Icons.Filled.FilterList, contentDescription = tr("Filters", "ফিল্টার"))
                }
            }
            BellAction(nav, d)
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SeekerBanners(d, nav) }
            item {
                OutlinedTextField(
                    value = f.query, onValueChange = { q -> vm.filter.update { it.copy(query = q) } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text(tr("Search jobs, areas, keywords", "কাজ, এলাকা বা শব্দ খুঁজুন")) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (f.query.isNotEmpty()) IconButton(onClick = { vm.filter.update { it.copy(query = "") } }) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary, unfocusedBorderColor = Border,
                        focusedContainerColor = CardColor, unfocusedContainerColor = CardColor
                    )
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = f.categories.isEmpty(), onClick = { vm.filter.update { it.copy(categories = emptySet()) } },
                            label = { Text(tr("All", "সব")) }
                        )
                    }
                    val mine = if (me.categories.isNotEmpty()) me.categories else Catalog.categories.take(6).map { it.id }
                    items(mine) { cid ->
                        val c = Catalog.category(cid)
                        FilterChip(
                            selected = cid in f.categories,
                            onClick = {
                                vm.filter.update { cur ->
                                    cur.copy(categories = if (cid in cur.categories) cur.categories - cid else cur.categories + cid)
                                }
                            },
                            label = { Text(c.emoji + " " + c.name(d.lang)) }
                        )
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tr("${feed.size} jobs", "${digitsFor(d.lang, feed.size.toString())} টি কাজ") + " · " + when (f.sort) {
                            SortBy.BEST_MATCH -> tr("best match", "সেরা মিল")
                            SortBy.NEAREST -> tr("nearest", "সবচেয়ে কাছে")
                            SortBy.NEWEST -> tr("newest", "নতুন আগে")
                            SortBy.BUDGET -> tr("highest budget", "বেশি বাজেট")
                        },
                        color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f)
                    )
                    if (f.activeCount > 0 || f.query.isNotEmpty()) {
                        TextButton(onClick = { vm.filter.value = JobFilter() }) { Text(tr("Clear", "মুছুন")) }
                    }
                }
            }
            if (feed.isEmpty()) {
                item {
                    EmptyState(
                        "🔎", tr("No jobs match your filters", "ফিল্টারের সাথে মিলে এমন কাজ নেই"),
                        tr("Try a wider area or fewer filters.", "এলাকা বাড়ান বা ফিল্টার কমান।"),
                        tr("Clear filters", "ফিল্টার মুছুন"), { vm.filter.value = JobFilter() }
                    )
                }
            }
            items(feed, key = { it.job.id }) { fi ->
                val job = fi.job
                val applied = fi.myApplication
                val badge = when {
                    applied != null -> tr("You applied · ", "আপনি আবেদন করেছেন · ") + money(applied.price)
                    job.invitedSeekerId == me.id -> tr("Invited you", "আপনাকে আমন্ত্রণ")
                    else -> null
                }
                JobCard(
                    job, fi.client, fi.distanceKm, now, { nav.navigate(Routes.job(job.id)) },
                    saved = job.id in me.savedJobIds, onSave = { vm.toggleSaved(job.id) }, badge = badge
                )
            }
        }
    }
}

@Composable
fun FiltersScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val start = remember { vm.filter.value }
    var cats by remember { mutableStateOf(start.categories) }
    var district by remember { mutableStateOf(start.district) }
    var areaId by remember { mutableStateOf(start.areaId) }
    var maxKm by remember { mutableStateOf(start.maxKm) }
    var minBudget by remember { mutableStateOf(start.minBudget) }
    var urgency by remember { mutableStateOf(start.urgency) }
    var sort by remember { mutableStateOf(start.sort) }
    val lang = d.lang
    ScreenFrame(
        title = tr("Filters", "ফিল্টার"), onBack = { nav.popBackStack() },
        actions = {
            TextButton(onClick = {
                cats = emptySet(); district = null; areaId = null; maxKm = 0; minBudget = 0; urgency = null; sort = SortBy.BEST_MATCH
            }) { Text(tr("Reset", "রিসেট")) }
        },
        bottomBar = {
            Surface(color = CardColor, shadowElevation = 8.dp) {
                Column(Modifier.padding(14.dp)) {
                    PrimaryButton(tr("Show jobs", "কাজ দেখুন"), onClick = {
                        vm.filter.update {
                            it.copy(categories = cats, district = district, areaId = areaId, maxKm = maxKm,
                                minBudget = minBudget, urgency = urgency, sort = sort)
                        }
                        nav.popBackStack()
                    })
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(tr("Job type", "কাজের ধরন"), fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in Catalog.categories) {
                    FilterChip(
                        selected = c.id in cats,
                        onClick = { cats = if (c.id in cats) cats - c.id else cats + c.id },
                        label = { Text(c.emoji + " " + c.name(lang)) }
                    )
                }
            }
            HorizontalDivider(color = Border)
            Text(tr("District", "জেলা"), fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = district == null, onClick = { district = null; areaId = null }, label = { Text(tr("All", "সব")) })
                for ((en, bn) in Catalog.districts) {
                    FilterChip(
                        selected = district == en,
                        onClick = { district = en; areaId = null },
                        label = { Text(if (lang == "bn") bn else en) }
                    )
                }
            }
            if (district != null) {
                Text(tr("Area", "এলাকা"), fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = areaId == null, onClick = { areaId = null }, label = { Text(tr("Whole district", "পুরো জেলা")) })
                    for (a in Catalog.areasOf(district ?: "")) {
                        FilterChip(selected = areaId == a.id, onClick = { areaId = a.id }, label = { Text(a.name(lang)) })
                    }
                }
            }
            HorizontalDivider(color = Border)
            Text(
                tr("Distance from you: ", "আপনার থেকে দূরত্ব: ") +
                    if (maxKm == 0) tr("any", "যেকোনো") else num(maxKm) + " " + tr("km", "কিমি"),
                fontWeight = FontWeight.Bold
            )
            Slider(value = maxKm.toFloat(), onValueChange = { maxKm = it.toInt() }, valueRange = 0f..30f)
            HorizontalDivider(color = Border)
            Text(tr("Minimum budget", "সর্বনিম্ন বাজেট"), fontWeight = FontWeight.Bold)
            TagChips(
                listOf("0", "500", "1000", "3000", "5000"), setOf(minBudget.toString()),
                { minBudget = it.toInt() },
                { if (it == "0") tr("Any", "যেকোনো") else money(it.toInt()) + "+" }
            )
            Text(tr("When", "কখন"), fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = urgency == null, onClick = { urgency = null }, label = { Text(tr("Any", "যেকোনো")) })
                for (u in Urgency.values()) {
                    FilterChip(selected = urgency == u, onClick = { urgency = u }, label = { Text(urgencyLabel(u)) })
                }
            }
            HorizontalDivider(color = Border)
            Text(tr("Sort by", "সাজান"), fontWeight = FontWeight.Bold)
            TagChips(
                SortBy.values().map { it.name }, setOf(sort.name),
                { sort = SortBy.valueOf(it) },
                {
                    when (SortBy.valueOf(it)) {
                        SortBy.BEST_MATCH -> tr("Best match", "সেরা মিল")
                        SortBy.NEAREST -> tr("Nearest", "সবচেয়ে কাছে")
                        SortBy.NEWEST -> tr("Newest", "নতুন আগে")
                        SortBy.BUDGET -> tr("Highest budget", "বেশি বাজেট")
                    }
                }
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun MyWorkScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    var tab by remember { mutableStateOf(0) }
    val mine = Queries.seekerJobs(d, me.id)
    val applied = mine.filter { j ->
        j.status == JobStatus.OPEN && j.applications.any { it.seekerId == me.id && it.status == AppStatus.PENDING }
    }
    val active = mine.filter { it.hiredSeekerId == me.id && it.isActive }
    val done = mine.filter { j -> j !in applied && j !in active }
    val list = when (tab) { 0 -> applied; 1 -> active; else -> done }
    val e = Queries.earnings(d, me.id, now)
    TabScaffold(nav, d, Routes.MYWORK, tr("My Work", "আমার কাজ"), actions = { BellAction(nav, d) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { nav.navigate(Routes.EARNINGS) },
                shape = RoundedCornerShape(18.dp), color = Primary
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("Earned this month", "এই মাসের আয়"), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Text(money(e.month), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        tr("Today ", "আজ ") + money(e.today) + "  ·  " + tr("This week ", "এই সপ্তাহে ") + money(e.week),
                        color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp
                    )
                }
            }
            TabRow(selectedTabIndex = tab, containerColor = Canvas, contentColor = Primary) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(tr("Applied", "আবেদন") + " (" + num(applied.size) + ")") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tr("Active", "চলমান") + " (" + num(active.size) + ")") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(tr("Done", "শেষ") + " (" + num(done.size) + ")") })
            }
            if (list.isEmpty()) {
                EmptyState(
                    "🧰", tr("Nothing here yet", "এখানে এখনো কিছু নেই"),
                    tr("Find a job near you and send your offer.", "কাছের কাজ খুঁজে অফার পাঠান।"),
                    tr("Find jobs", "কাজ খুঁজুন"), { nav.goTab(Routes.FEED, Routes.MYWORK) },
                    Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list) { j ->
                        val myApp = Queries.myApplication(j, me.id)
                        val badge = when {
                            j.hiredSeekerId == me.id && j.status == JobStatus.COMPLETED && !j.seekerReviewed -> tr("Rate client", "ক্লায়েন্টকে রেটিং দিন")
                            j.hiredSeekerId == me.id -> statusLabel(j.status)
                            myApp != null && myApp.status == AppStatus.REJECTED -> tr("Not selected", "নির্বাচিত হননি")
                            myApp != null && myApp.status == AppStatus.WITHDRAWN -> tr("Withdrawn", "প্রত্যাহার")
                            myApp != null -> tr("Offer ", "অফার ") + money(myApp.price)
                            else -> statusLabel(j.status)
                        }
                        JobCard(
                            j, d.user(j.clientId), Geo.between(me.areaId, j.areaId), now,
                            { nav.navigate(Routes.job(j.id)) }, badge = badge,
                            badgeGood = j.status != JobStatus.CANCELLED && myApp?.status != AppStatus.REJECTED
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    val jobs = d.jobs.filter { it.id in me.savedJobIds }.sortedByDescending { it.createdAt }
    ScreenFrame(title = tr("Saved jobs", "সংরক্ষিত কাজ"), onBack = { nav.popBackStack() }) { pad ->
        if (jobs.isEmpty()) {
            EmptyState("🔖", tr("No saved jobs", "কোনো সংরক্ষিত কাজ নেই"), tr("Tap the bookmark on a job to save it.", "কাজে বুকমার্ক চাপলে এখানে সংরক্ষিত হবে।"), modifier = Modifier.padding(pad))
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(jobs) { j ->
                    JobCard(
                        j, d.user(j.clientId), Geo.between(me.areaId, j.areaId), now, { nav.navigate(Routes.job(j.id)) },
                        saved = true, onSave = { vm.toggleSaved(j.id) },
                        badge = if (j.status != JobStatus.OPEN) statusLabel(j.status) else null, badgeGood = false
                    )
                }
            }
        }
    }
}

@Composable
fun EarningsScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    val e = Queries.earnings(d, me.id, now)
    val done = d.jobs.filter { it.hiredSeekerId == me.id && it.status == JobStatus.COMPLETED }.sortedByDescending { it.completedAt }
    ScreenFrame(title = tr("Earnings", "আয়"), onBack = { nav.popBackStack() }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard {
                    Text(tr("Total earned", "মোট আয়"), color = Muted)
                    Text(money(e.total), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
                    Text(tr("${e.jobs} jobs completed", "${digitsFor(d.lang, e.jobs.toString())} টি কাজ সম্পন্ন"), color = Muted)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox(tr("Today", "আজ"), money(e.today), Modifier.weight(1f))
                        StatBox(tr("7 days", "৭ দিন"), money(e.week), Modifier.weight(1f))
                        StatBox(tr("30 days", "৩০ দিন"), money(e.month), Modifier.weight(1f))
                    }
                }
            }
            item { SectionHeader(tr("Completed jobs", "সম্পন্ন কাজ")) }
            if (done.isEmpty()) item { Text(tr("Your earnings will appear here.", "আপনার আয় এখানে দেখা যাবে।"), color = Muted) }
            items(done.take(50)) { j ->
                AppCard(onClick = { nav.navigate(Routes.receipt(j.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Catalog.category(j.categoryId).emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(j.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(clockOf(j.completedAt, d.lang), color = Muted, fontSize = 12.sp)
                        }
                        Text(money(j.agreedPrice), fontWeight = FontWeight.ExtraBold, color = Primary)
                    }
                }
            }
        }
    }
}
