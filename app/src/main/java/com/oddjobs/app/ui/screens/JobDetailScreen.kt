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

private val cancelReasons = listOf(
    "Changed my mind" to "মত পরিবর্তন করেছি",
    "Found someone else" to "অন্য কাউকে পেয়েছি",
    "Price problem" to "দামের সমস্যা",
    "Emergency" to "জরুরি অবস্থা",
    "Other" to "অন্যান্য"
)

@Composable
fun JobDetailScreen(vm: AppViewModel, nav: NavHostController, id: String) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val ctx = LocalContext.current
    val me = d.me ?: return
    val job = d.job(id)
    if (job == null) {
        ScreenFrame(title = tr("Job", "কাজ"), onBack = { nav.popBackStack() }) { pad ->
            EmptyState("🤷", tr("Job not found", "কাজটি পাওয়া যায়নি"), modifier = Modifier.padding(pad))
        }
        return
    }
    val lang = d.lang
    val isClient = job.clientId == me.id
    val isHired = job.hiredSeekerId == me.id
    val client = d.user(job.clientId)
    val hired = d.user(job.hiredSeekerId)
    val cat = Catalog.category(job.categoryId)
    val area = Catalog.area(job.areaId)
    val myApp = Queries.myApplication(job, me.id)?.takeIf { it.status != AppStatus.WITHDRAWN }
    val (stBg, stFg) = statusColors(job.status)

    var showApply by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }
    var showCode by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    ScreenFrame(
        title = tr("Job details", "কাজের বিবরণ"), onBack = { nav.popBackStack() },
        actions = {
            if (!isClient && !isHired) {
                IconButton(onClick = { vm.toggleSaved(job.id) }) {
                    Icon(
                        if (job.id in me.savedJobIds) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = null, tint = if (job.id in me.savedJobIds) Accent else Muted
                    )
                }
                IconButton(onClick = { showReport = true }) { Icon(Icons.Filled.Flag, contentDescription = tr("Report", "রিপোর্ট")) }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------------- header
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(cat.emoji + " " + cat.name(lang))
                        Spacer(Modifier.width(6.dp))
                        Pill(statusLabel(job.status), stBg, stFg)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(job.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(money(job.budget), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
                        if (job.negotiable) {
                            Spacer(Modifier.width(8.dp))
                            Text(tr("negotiable", "আলোচনা সাপেক্ষ"), color = Muted, fontSize = 12.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            area.full(lang) + if (!isClient) " · " + km(Geo.between(me.areaId, job.areaId)) else "",
                            color = Muted, fontSize = 13.sp
                        )
                    }
                    Text(tr("Posted ", "পোস্ট করা হয়েছে ") + ago(job.createdAt, now), color = Muted, fontSize = 12.sp)
                }
            }

            // ---------------- primary action block
            item {
                when {
                    isClient && job.status == JobStatus.AWAITING_CONFIRMATION -> {
                        Surface(shape = RoundedCornerShape(16.dp), color = WarnBg) {
                            Column(Modifier.padding(16.dp)) {
                                Text(tr("The worker says the job is done", "কর্মী জানিয়েছেন কাজ শেষ"), fontWeight = FontWeight.ExtraBold)
                                Text(tr("Check the work, then confirm and record the payment.", "কাজ দেখে নিয়ে নিশ্চিত করুন ও পেমেন্ট রেকর্ড করুন।"), color = Muted, fontSize = 13.sp)
                                Spacer(Modifier.height(10.dp))
                                PrimaryButton(tr("Confirm & record payment", "নিশ্চিত করে পেমেন্ট রেকর্ড করুন"), { showConfirm = true }, icon = Icons.Filled.Check)
                            }
                        }
                    }
                    isHired && JobFlow.nextForSeeker(job.status) != null -> {
                        val next = JobFlow.nextForSeeker(job.status)!!
                        val label = when (next) {
                            JobStatus.ON_THE_WAY -> tr("I'm on my way", "আমি রওনা দিয়েছি")
                            JobStatus.ARRIVED -> tr("I've arrived (enter code)", "পৌঁছেছি (কোড দিন)")
                            JobStatus.IN_PROGRESS -> tr("Start work", "কাজ শুরু করুন")
                            else -> tr("Work finished", "কাজ শেষ")
                        }
                        PrimaryButton(label, onClick = {
                            if (next == JobStatus.ARRIVED) showCode = true else vm.advance(job.id, next)
                        })
                    }
                    isHired && job.status == JobStatus.AWAITING_CONFIRMATION -> {
                        AppCard {
                            Text("⏳ " + tr("Waiting for the client to confirm", "ক্লায়েন্টের নিশ্চিতকরণের অপেক্ষা"), fontWeight = FontWeight.Bold)
                        }
                    }
                    !isClient && !isHired && job.status == JobStatus.OPEN -> {
                        if (myApp != null) {
                            AppCard {
                                Text(tr("Your offer", "আপনার অফার"), fontWeight = FontWeight.Bold)
                                Text(money(myApp.price) + " · " + tr("arrive in ${myApp.etaMin} min", "${digitsFor(lang, myApp.etaMin.toString())} মিনিটে পৌঁছাবেন"), color = Primary, fontWeight = FontWeight.ExtraBold)
                                if (myApp.message.isNotBlank()) Text(myApp.message, color = Muted)
                                Text(
                                    when (myApp.status) {
                                        AppStatus.PENDING -> tr("Waiting for the client's reply", "ক্লায়েন্টের উত্তরের অপেক্ষা")
                                        AppStatus.REJECTED -> tr("Not selected", "নির্বাচিত হননি")
                                        else -> ""
                                    },
                                    color = Muted, fontSize = 13.sp
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SecondaryButton(tr("Edit", "সম্পাদনা"), { showApply = true }, Modifier.weight(1f))
                                    SecondaryButton(tr("Withdraw", "প্রত্যাহার"), { vm.withdraw(job.id) }, Modifier.weight(1f), danger = true)
                                }
                                Spacer(Modifier.height(8.dp))
                                SecondaryButton(
                                    tr("Message client", "ক্লায়েন্টকে বার্তা"),
                                    { nav.navigate(Routes.chat(chatIdOf(job.id, me.id))) }, icon = Icons.Filled.Chat
                                )
                            }
                        } else if (!me.isSeeker) {
                            PrimaryButton(tr("Create worker profile to apply", "আবেদনের জন্য কর্মী প্রোফাইল তৈরি করুন"), { nav.navigate(Routes.edit(true)) })
                        } else if (me.verifyStatus != VerifyStatus.APPROVED) {
                            SeekerBanners(d, nav)
                        } else {
                            PrimaryButton(tr("Send offer", "অফার পাঠান"), { showApply = true }, icon = Icons.AutoMirrored.Filled.Send)
                            Spacer(Modifier.height(8.dp))
                            SecondaryButton(
                                tr("Ask a question first", "আগে প্রশ্ন করুন"),
                                { nav.navigate(Routes.chat(chatIdOf(job.id, me.id))) }, icon = Icons.Filled.Chat
                            )
                        }
                    }
                    !isClient && !isHired && job.status != JobStatus.OPEN -> {
                        AppCard { Text(tr("This job is no longer open.", "কাজটি আর খোলা নেই।"), color = Muted) }
                    }
                    else -> {}
                }
            }

            // ---------------- description
            item {
                AppCard {
                    Text(tr("Description", "বিবরণ"), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(job.description.ifBlank { tr("No description.", "কোনো বিবরণ নেই।") })
                    if (job.photos.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScrollCompat()) {
                            for (p in job.photos) PhotoImage(p, Modifier.size(130.dp).clip(RoundedCornerShape(12.dp)))
                        }
                    }
                }
            }
            item {
                AppCard {
                    InfoRow(tr("When", "কখন"), urgencyLabel(job.urgency))
                    InfoRow(tr("Payment", "পেমেন্ট"), payLabel(job.payMethod))
                    if (isClient || isHired) {
                        InfoRow(tr("Landmark", "ল্যান্ডমার্ক"), job.landmark.ifBlank { "—" })
                        InfoRow(tr("Arrival code", "আগমন কোড"), if (isClient) digitsFor(lang, job.arrivalCode) else tr("Ask the client", "ক্লায়েন্টের কাছে চান"))
                    } else {
                        Text(
                            "🔒 " + tr("Exact address is shared after you are hired.", "নিয়োগ পাওয়ার পর সঠিক ঠিকানা জানানো হবে।"),
                            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (job.onlyVerified) InfoRow(tr("Workers", "কর্মী"), tr("ID verified only", "শুধু আইডি যাচাইকৃত"))
                    if (job.minRating > 0) InfoRow(tr("Min. rating", "সর্বনিম্ন রেটিং"), rating(job.minRating) + "★")
                }
            }

            // ---------------- client info (seeker view)
            if (!isClient && client != null) {
                item {
                    val (cr, cn) = Queries.clientRating(d, client.id)
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(client.name, client.photoPath, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(client.name, fontWeight = FontWeight.Bold)
                                Text(
                                    (if (cn > 0) rating(cr) + "★ (" + num(cn) + ") · " else tr("New client · ", "নতুন ক্লায়েন্ট · ")) +
                                        tr("${Queries.clientJobCount(d, client.id)} jobs posted", "${digitsFor(lang, Queries.clientJobCount(d, client.id).toString())} টি কাজ পোস্ট"),
                                    color = Muted, fontSize = 12.sp
                                )
                                VerifiedBadge(client.level)
                            }
                            if (isHired) {
                                IconButton(onClick = { dial(ctx, client.phone) }) { Icon(Icons.Filled.Phone, contentDescription = null, tint = Primary) }
                                IconButton(onClick = { nav.navigate(Routes.chat(chatIdOf(job.id, me.id))) }) { Icon(Icons.Filled.Chat, contentDescription = null, tint = Primary) }
                            }
                        }
                    }
                }
            }

            // ---------------- timeline
            if (job.status != JobStatus.OPEN && job.status != JobStatus.CANCELLED && job.status != JobStatus.EXPIRED) {
                item {
                    AppCard {
                        Text(tr("Progress", "অগ্রগতি"), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        JobTimeline(job)
                    }
                }
            }
            if (job.status == JobStatus.CANCELLED || job.status == JobStatus.EXPIRED) {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = ErrorBg) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(statusLabel(job.status), fontWeight = FontWeight.ExtraBold, color = ErrorRed)
                            if (job.cancelReason.isNotBlank()) Text(job.cancelReason, color = Muted)
                        }
                    }
                }
            }

            // ---------------- CLIENT: offers
            if (isClient && job.status == JobStatus.OPEN) {
                val offers = job.applications.filter { it.status == AppStatus.PENDING }.mapNotNull { a ->
                    val r = Queries.rankedOne(d, a.seekerId, now) ?: return@mapNotNull null
                    val dist = Geo.between(job.areaId, r.user.areaId)
                    val m = Ranking.match(
                        r.trust, Ranking.proximity(dist, r.user.radiusKm),
                        Ranking.completedInCategory(r.user.id, job.categoryId, d.jobs),
                        Ranking.priceFit(a.price, r.user.rateMin, r.user.rateMax)
                    )
                    Triple(a, r.copy(distanceKm = dist, match = m), m)
                }.sortedByDescending { it.third }
                item { SectionHeader(tr("Offers (${offers.size})", "অফার (${digitsFor(lang, offers.size.toString())})")) }
                if (offers.isEmpty()) {
                    item {
                        AppCard {
                            Text("⏳ " + tr("Waiting for offers", "অফারের অপেক্ষায়"), fontWeight = FontWeight.Bold)
                            Text(
                                tr("Verified workers nearby have been notified. Offers usually arrive within minutes.", "কাছের যাচাইকৃত কর্মীদের জানানো হয়েছে। সাধারণত কয়েক মিনিটেই অফার আসে।"),
                                color = Muted, fontSize = 13.sp
                            )
                        }
                    }
                }
                items(offers.size) { i ->
                    val (a, r, _) = offers[i]
                    Column {
                        if (i == 0 && offers.size > 1) {
                            Pill(tr("★ Best match", "★ সেরা মিল"), AccentLight, Accent)
                            Spacer(Modifier.height(6.dp))
                        }
                        SeekerCard(
                            r, onClick = { nav.navigate(Routes.seeker(r.user.id)) }, offer = a,
                            actions = {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SecondaryButton(tr("Chat", "চ্যাট"), { nav.navigate(Routes.chat(chatIdOf(job.id, r.user.id))) }, Modifier.weight(1f), icon = Icons.Filled.Chat)
                                    PrimaryButton(tr("Hire", "নিয়োগ দিন"), { vm.hire(job.id, r.user.id) }, Modifier.weight(1f), icon = Icons.Filled.Check)
                                }
                            }
                        )
                    }
                }
                item {
                    SecondaryButton(tr("Cancel this job", "কাজ বাতিল করুন"), { showCancel = true }, danger = true)
                }
            }

            // ---------------- CLIENT: hired worker
            if (isClient && hired != null && job.status != JobStatus.OPEN) {
                item {
                    val rs = Queries.rankedOne(d, hired.id, now)
                    if (rs != null) {
                        SeekerCard(
                            rs.copy(distanceKm = Geo.between(job.areaId, hired.areaId)),
                            onClick = { nav.navigate(Routes.seeker(hired.id)) },
                            actions = {
                                Spacer(Modifier.height(10.dp))
                                Text(tr("Agreed price: ", "নির্ধারিত দাম: ") + money(job.agreedPrice), fontWeight = FontWeight.ExtraBold, color = Primary)
                                if (job.status != JobStatus.CANCELLED) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SecondaryButton(tr("Chat", "চ্যাট"), { nav.navigate(Routes.chat(chatIdOf(job.id, hired.id))) }, Modifier.weight(1f), icon = Icons.Filled.Chat)
                                        SecondaryButton(tr("Call", "কল"), { dial(ctx, hired.phone) }, Modifier.weight(1f), icon = Icons.Filled.Phone)
                                    }
                                }
                            }
                        )
                    }
                }
                if (job.status == JobStatus.HIRED || job.status == JobStatus.ON_THE_WAY) {
                    item {
                        Surface(shape = RoundedCornerShape(16.dp), color = AccentLight) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(tr("Arrival code", "আগমন কোড"), fontWeight = FontWeight.Bold)
                                Text(digitsFor(lang, job.arrivalCode), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Primary, letterSpacing = 6.sp)
                                Text(
                                    tr("Tell this code to the worker only when they arrive.", "কর্মী পৌঁছালে তবেই তাঁকে এই কোডটি বলুন।"),
                                    color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    item { SecondaryButton(tr("Cancel job", "কাজ বাতিল করুন"), { showCancel = true }, danger = true) }
                }
            }

            // ---------------- HIRED SEEKER: cancel
            if (isHired && (job.status == JobStatus.HIRED || job.status == JobStatus.ON_THE_WAY)) {
                item {
                    SecondaryButton(tr("Cancel (affects your ranking)", "বাতিল (র‍্যাংকিংয়ে প্রভাব পড়বে)"), { showCancel = true }, danger = true)
                }
            }
            if (isHired && job.status != JobStatus.CANCELLED && job.status != JobStatus.OPEN && client != null) {
                item {
                    SecondaryButton(tr("Message client", "ক্লায়েন্টকে বার্তা"), { nav.navigate(Routes.chat(chatIdOf(job.id, me.id))) }, icon = Icons.Filled.Chat)
                }
            }

            // ---------------- completed
            if (job.status == JobStatus.COMPLETED) {
                item {
                    AppCard {
                        Text("✅ " + tr("Job completed", "কাজ সম্পন্ন"), fontWeight = FontWeight.ExtraBold, color = Success)
                        Text(
                            tr("Paid ", "পরিশোধ ") + money(job.agreedPrice) + " · " + payLabel(job.payMethod),
                            color = Muted
                        )
                        Spacer(Modifier.height(10.dp))
                        val reviewed = if (isClient) job.clientReviewed else job.seekerReviewed
                        if ((isClient || isHired) && !reviewed) {
                            PrimaryButton(
                                if (isClient) tr("Rate the worker", "কর্মীকে রেটিং দিন") else tr("Rate the client", "ক্লায়েন্টকে রেটিং দিন"),
                                { nav.navigate(Routes.review(job.id)) }, icon = Icons.Filled.Star
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        SecondaryButton(tr("View receipt", "রসিদ দেখুন"), { nav.navigate(Routes.receipt(job.id)) }, icon = Icons.Filled.Receipt)
                        if (isClient && hired != null) {
                            Spacer(Modifier.height(8.dp))
                            SecondaryButton(
                                tr("Hire ${hired.name} again", "${hired.name} কে আবার নিয়োগ দিন"),
                                { nav.navigate(Routes.post(job.categoryId, hired.id)) }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showApply) {
        ApplyDialog(job, myApp, onDismiss = { showApply = false }) { price, msg, eta ->
            val out = vm.apply(job.id, price, msg, eta)
            if (out.ok) showApply = false
        }
    }
    if (showCode) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCode = false },
            title = { Text(tr("Enter arrival code", "আগমন কোড দিন")) },
            text = {
                Column {
                    Text(tr("Ask the client for the 4-digit code.", "ক্লায়েন্টের কাছে ৪ সংখ্যার কোড চান।"), color = Muted)
                    Spacer(Modifier.height(8.dp))
                    AppTextField(code, { code = it }, tr("4-digit code", "৪ সংখ্যার কোড"), keyboard = KeyboardType.NumberPassword, maxLength = 4)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val out = vm.advance(job.id, JobStatus.ARRIVED, code)
                    if (out.ok) showCode = false
                }, enabled = code.length == 4) { Text(tr("Confirm", "নিশ্চিত করুন"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCode = false }) { Text(tr("Cancel", "বাতিল")) } }
        )
    }
    if (showConfirm) {
        var price by remember { mutableStateOf(job.agreedPrice.toString()) }
        var pay by remember { mutableStateOf(job.payMethod) }
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(tr("Confirm completion", "সম্পন্ন নিশ্চিত করুন")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(price, { price = it.filter { c -> c.isDigit() } }, tr("Final amount paid (৳)", "চূড়ান্ত পরিশোধিত (৳)"), keyboard = KeyboardType.Number, maxLength = 7)
                    Text(tr("Paid by", "পরিশোধের মাধ্যম"), fontWeight = FontWeight.Bold)
                    TagChips(PayMethod.values().map { it.name }, setOf(pay.name), { pay = PayMethod.valueOf(it) }, { payLabel(PayMethod.valueOf(it)) })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val out = vm.confirm(job.id, price.toIntOrNull() ?: 0, pay)
                    if (out.ok) { showConfirm = false; nav.navigate(Routes.review(job.id)) }
                }) { Text(tr("Confirm", "নিশ্চিত করুন"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text(tr("Cancel", "বাতিল")) } }
        )
    }
    if (showCancel) {
        var reason by remember { mutableStateOf(cancelReasons[0].first) }
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text(tr("Cancel this job?", "কাজটি বাতিল করবেন?")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("Please tell us why.", "কারণটি জানান।"), color = Muted)
                    TagChips(cancelReasons.map { it.first }, setOf(reason), { reason = it }, { en -> tr(en, cancelReasons.first { p -> p.first == en }.second) })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val out = vm.cancel(job.id, reason)
                    if (out.ok) showCancel = false
                }) { Text(tr("Cancel job", "কাজ বাতিল করুন"), color = ErrorRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCancel = false }) { Text(tr("Keep it", "রেখে দিন")) } }
        )
    }
    if (showReport) {
        ReportDialog(onDismiss = { showReport = false }, onSubmit = {
            vm.report("job", job.id, it); showReport = false
            vm.toast(if (lang == "bn") "রিপোর্ট পাঠানো হয়েছে। ধন্যবাদ।" else "Report sent. Thank you.")
        })
    }
}

@Composable
fun Modifier.horizontalScrollCompat(): Modifier = this.horizontalScroll(rememberScrollState())

@Composable
fun ApplyDialog(job: Job, existing: Application?, onDismiss: () -> Unit, onSend: (Int, String, Int) -> Unit) {
    var price by remember { mutableStateOf((existing?.price ?: job.budget).toString()) }
    var msg by remember { mutableStateOf(existing?.message ?: "") }
    var eta by remember { mutableStateOf(existing?.etaMin ?: 30) }
    val lang = LocalLang.current
    val p = price.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) tr("Edit your offer", "অফার সম্পাদনা") else tr("Send your offer", "আপনার অফার পাঠান")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Client budget: ", "ক্লায়েন্টের বাজেট: ") + money(job.budget), color = Muted, fontSize = 13.sp)
                AppTextField(price, { price = it.filter { c -> c.isDigit() } }, tr("Your price (৳)", "আপনার দাম (৳)"), keyboard = KeyboardType.Number, maxLength = 7, prefix = "৳ ")
                Text(tr("I can arrive in", "আমি পৌঁছাতে পারব"), fontWeight = FontWeight.Bold)
                TagChips(
                    listOf("15", "30", "60", "120"), setOf(eta.toString()), { eta = it.toInt() },
                    { digitsFor(LocalLang.current, it) + " " + tr("min", "মিনিট") }
                )
                AppTextField(msg, { msg = it }, tr("Message (optional)", "বার্তা (ঐচ্ছিক)"), singleLine = false, minLines = 2, maxLength = 300)
                TagChips(
                    listOf("a", "b"), emptySet(),
                    { k -> msg = if (k == "a") (if (lang == "bn") "আমি আজই কাজটি করে দিতে পারব।" else "I can do this today.") else (if (lang == "bn") "আমার কাছে প্রয়োজনীয় সব সরঞ্জাম আছে।" else "I have all the tools needed.") },
                    { k -> if (k == "a") tr("I can do this today", "আজই করে দিতে পারব") else tr("I have the tools", "আমার সরঞ্জাম আছে") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(p, msg, eta) }, enabled = p >= 50) {
                Text(tr("Send", "পাঠান"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "বাতিল")) } }
    )
}
