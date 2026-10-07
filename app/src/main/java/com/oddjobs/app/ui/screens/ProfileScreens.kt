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

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun PhotoSlot(label: String, path: String?, vm: AppViewModel, modifier: Modifier = Modifier, onPicked: (String) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importPhoto(uri) { p -> if (p != null) onPicked(p) }
    }
    Surface(
        modifier = modifier.height(120.dp).clickable {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        shape = RoundedCornerShape(14.dp), color = CardColor, border = BorderStroke(1.dp, Border)
    ) {
        if (path != null) {
            PhotoImage(path, Modifier.fillMaxSize())
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Primary, modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(6.dp))
                Text(label, fontSize = 12.sp, color = Muted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ProfileFormScreen(vm: AppViewModel, nav: NavHostController, first: Boolean, startSeeker: Boolean) {
    val d by vm.state.collectAsState()
    val me = d.me ?: return
    var name by remember { mutableStateOf(me.name) }
    var areaId by remember { mutableStateOf(me.areaId) }
    var seeker by remember { mutableStateOf(me.isSeeker || startSeeker) }
    var cats by remember { mutableStateOf(me.categories.toSet()) }
    var bio by remember { mutableStateOf(me.bio) }
    var exp by remember { mutableStateOf(me.experienceYears) }
    var rMin by remember { mutableStateOf(me.rateMin.toString()) }
    var rMax by remember { mutableStateOf(me.rateMax.toString()) }
    var radius by remember { mutableStateOf(me.radiusKm) }
    var agree by remember { mutableStateOf(!first) }
    var showArea by remember { mutableStateOf(false) }
    val lang = d.lang
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importPhoto(uri) { p -> if (p != null) vm.setPhoto(p) }
    }

    ScreenFrame(
        title = if (first) tr("Create your profile", "প্রোফাইল তৈরি করুন") else tr("Edit profile", "প্রোফাইল সম্পাদনা"),
        onBack = if (first) null else fun() { nav.popBackStack() }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(Modifier.clickable {
                    photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Avatar(name.ifBlank { "?" }, me.photoPath, 96.dp)
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(32.dp).background(Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
            Text(
                tr("Add a clear face photo. People hire people they can see.", "পরিষ্কার মুখের ছবি দিন। ছবি থাকলে মানুষ বেশি ভরসা করে।"),
                color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            AppTextField(name, { name = it }, tr("Full name", "পুরো নাম"), maxLength = 40)
            PickerField(
                tr("Your area", "আপনার এলাকা"), Catalog.area(areaId).full(lang), { showArea = true }
            )

            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("I also want to work and earn", "আমি কাজ করে আয়ও করতে চাই"), fontWeight = FontWeight.Bold)
                        Text(
                            tr("Create a worker profile to apply for jobs near you.", "কর্মী প্রোফাইল তৈরি করে কাছের কাজে আবেদন করুন।"),
                            color = Muted, fontSize = 13.sp
                        )
                    }
                    Switch(checked = seeker, onCheckedChange = { seeker = it }, enabled = !me.isSeeker)
                }
            }

            if (seeker) {
                SectionHeader(tr("Your skills (choose up to 5)", "আপনার দক্ষতা (সর্বোচ্চ ৫টি)"))
                CategoryGrid(
                    selected = cats,
                    onToggle = { c ->
                        cats = if (c.id in cats) cats - c.id else if (cats.size < 5) cats + c.id else cats
                    }
                )
                Text(
                    tr("Experience: ", "অভিজ্ঞতা: ") + num(exp) + " " + tr("years", "বছর"),
                    fontWeight = FontWeight.SemiBold
                )
                Slider(value = exp.toFloat(), onValueChange = { exp = it.toInt() }, valueRange = 0f..30f)
                AppTextField(
                    bio, { bio = it }, tr("About you (what do you do best?)", "আপনার সম্পর্কে (কোন কাজে সেরা?)"),
                    singleLine = false, minLines = 3, maxLength = 300
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppTextField(rMin, { rMin = it.filter { c -> c.isDigit() } }, tr("Min rate ৳", "সর্বনিম্ন ৳"), Modifier.weight(1f), KeyboardType.Number, maxLength = 6)
                    AppTextField(rMax, { rMax = it.filter { c -> c.isDigit() } }, tr("Max rate ৳", "সর্বোচ্চ ৳"), Modifier.weight(1f), KeyboardType.Number, maxLength = 6)
                }
                Text(
                    tr("Work within: ", "কাজের পরিধি: ") + num(radius) + " " + tr("km", "কিমি"),
                    fontWeight = FontWeight.SemiBold
                )
                Slider(value = radius.toFloat(), onValueChange = { radius = it.toInt().coerceAtLeast(1) }, valueRange = 1f..30f)
            }

            if (first) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = agree, onCheckedChange = { agree = it })
                    Text(
                        tr("I agree to the Terms of Service and Privacy Policy", "আমি শর্তাবলী ও গোপনীয়তা নীতিতে সম্মত"),
                        fontSize = 13.sp, modifier = Modifier.weight(1f)
                    )
                }
            }
            PrimaryButton(
                if (first) tr("Finish & start", "শেষ করে শুরু করুন") else tr("Save changes", "পরিবর্তন সংরক্ষণ করুন"),
                onClick = {
                    val modeBefore = vm.state.value.mode
                    val out = vm.saveProfile(
                        ProfileInput(
                            name, areaId, seeker, cats.toList(), bio, exp,
                            rMin.toIntOrNull() ?: 0, rMax.toIntOrNull() ?: 0, radius
                        )
                    )
                    if (out.ok) {
                        val modeNow = vm.state.value.mode
                        if (first || modeNow != modeBefore) nav.resetTo(Routes.tabFor(modeNow)) else nav.popBackStack()
                    }
                },
                enabled = agree && name.trim().length >= 2
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showArea) {
        AreaPickerDialog(onSelect = { areaId = it; showArea = false }, onDismiss = { showArea = false })
    }
}

@Composable
fun VerificationScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val me = d.me ?: return
    var front by remember { mutableStateOf<String?>(null) }
    var back by remember { mutableStateOf<String?>(null) }
    var selfie by remember { mutableStateOf<String?>(null) }
    ScreenFrame(title = tr("Verify identity", "পরিচয় যাচাই"), onBack = { nav.popBackStack() }) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(tr("Why verify?", "কেন যাচাই করবেন?"), fontWeight = FontWeight.Bold)
                        Text(
                            tr(
                                "Verified workers can apply to jobs, get the ID-verified badge and rank higher.",
                                "যাচাইকৃত কর্মীরা কাজে আবেদন করতে পারেন, ব্যাজ পান এবং তালিকায় উপরে থাকেন।"
                            ),
                            color = Muted, fontSize = 13.sp
                        )
                    }
                }
            }
            when (me.verifyStatus) {
                VerifyStatus.APPROVED -> AppCard {
                    Text("✅ " + tr("Identity verified", "পরিচয় যাচাই সম্পন্ন"), fontWeight = FontWeight.ExtraBold, color = Success)
                    Text(tr("You can apply to jobs and your profile shows the verified badge.", "আপনি কাজে আবেদন করতে পারবেন এবং প্রোফাইলে ব্যাজ দেখা যাবে।"), color = Muted)
                }
                VerifyStatus.SUBMITTED -> AppCard {
                    Text("⏳ " + tr("In review", "পর্যালোচনায় আছে"), fontWeight = FontWeight.ExtraBold, color = Warn)
                    Text(
                        tr(
                            "Your documents were submitted. In demo mode the review finishes in a few seconds; in production our team reviews it within 24 hours.",
                            "আপনার কাগজপত্র জমা হয়েছে। ডেমো মোডে কয়েক সেকেন্ডে সম্পন্ন হয়; আসল সংস্করণে আমাদের টিম ২৪ ঘণ্টার মধ্যে দেখবে।"
                        ),
                        color = Muted
                    )
                }
                else -> {
                    Text(tr("1. National ID (front)", "১. জাতীয় পরিচয়পত্র (সামনে)"), fontWeight = FontWeight.Bold)
                    PhotoSlot(tr("Tap to add NID front", "NID এর সামনের ছবি দিন"), front, vm) { front = it }
                    Text(tr("2. National ID (back, optional)", "২. জাতীয় পরিচয়পত্র (পিছনে, ঐচ্ছিক)"), fontWeight = FontWeight.Bold)
                    PhotoSlot(tr("Tap to add NID back", "NID এর পিছনের ছবি দিন"), back, vm) { back = it }
                    Text(tr("3. Selfie holding your NID", "৩. NID হাতে সেলফি"), fontWeight = FontWeight.Bold)
                    PhotoSlot(tr("Tap to add selfie", "সেলফি দিন"), selfie, vm) { selfie = it }
                    Text(
                        tr(
                            "Your photos stay private. They are used only for verification and you can delete them any time from Settings.",
                            "আপনার ছবি গোপন থাকবে। শুধু যাচাইয়ের জন্য ব্যবহার হবে এবং সেটিংস থেকে যেকোনো সময় মুছতে পারবেন।"
                        ),
                        color = Muted, fontSize = 12.sp
                    )
                    PrimaryButton(
                        tr("Submit for review", "পর্যালোচনার জন্য জমা দিন"),
                        onClick = {
                            val list = listOfNotNull(front, back, selfie)
                            vm.submitVerification(list)
                        },
                        enabled = front != null && selfie != null
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun MenuRow(icon: ImageVector, text: String, onClick: () -> Unit, trailing: String? = null, danger: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) ErrorRed else Primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = if (danger) ErrorRed else MaterialTheme.colorScheme.onSurface)
        if (trailing != null) {
            Text(trailing, color = Muted, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted)
    }
}

@Composable
fun ProfileScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val me = d.me ?: return
    val ranked = if (me.isSeeker) Queries.rankedOne(d, me.id, now) else null
    TabScaffold(nav, d, Routes.PROFILE, tr("Profile", "প্রোফাইল")) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard(onClick = { nav.navigate(Routes.edit(false)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(me.name, me.photoPath, 64.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(me.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Text(digitsFor(d.lang, me.phone), color = Muted, fontSize = 13.sp)
                            Text(Catalog.area(me.areaId).full(d.lang), color = Muted, fontSize = 13.sp)
                            VerifiedBadge(me.level)
                        }
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = Muted)
                    }
                }
            }
            item {
                Text(tr("I want to…", "আমি চাই…"), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val hire = d.mode == Mode.CLIENT
                    ModeCard(
                        "🧑‍💼", tr("Hire someone", "কাউকে নিয়োগ দিন"), hire, Modifier.weight(1f)
                    ) { vm.switchMode(Mode.CLIENT); nav.resetTo(Routes.HOME) }
                    ModeCard(
                        "🛠️", tr("Find work", "কাজ খুঁজুন"), !hire, Modifier.weight(1f)
                    ) {
                        if (!me.isSeeker) nav.navigate(Routes.edit(true))
                        else { vm.switchMode(Mode.SEEKER); nav.resetTo(Routes.FEED) }
                    }
                }
            }
            if (ranked != null) {
                item {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tr("Your Trust Score", "আপনার ট্রাস্ট স্কোর"), fontWeight = FontWeight.Bold)
                                Text(
                                    tr("Built from ratings, completed jobs, speed and verification.", "রেটিং, সম্পন্ন কাজ, গতি ও যাচাই থেকে তৈরি।"),
                                    color = Muted, fontSize = 12.sp
                                )
                            }
                            Text(num(Math.round(ranked.trust).toInt()), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox(tr("Rating", "রেটিং"), if (ranked.stats.ratingCount > 0) rating(ranked.stats.ratingAvg) else "—", Modifier.weight(1f))
                            StatBox(tr("Jobs done", "কাজ সম্পন্ন"), num(ranked.stats.completed), Modifier.weight(1f))
                            StatBox(tr("Reviews", "রিভিউ"), num(ranked.stats.ratingCount), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tr("Available for new jobs", "নতুন কাজের জন্য প্রস্তুত"), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Switch(checked = me.availableNow, onCheckedChange = { vm.setAvailable(it) })
                        }
                    }
                }
            }
            item {
                AppCard {
                    Column {
                        val vtext = when (me.verifyStatus) {
                            VerifyStatus.APPROVED -> tr("Verified", "যাচাইকৃত")
                            VerifyStatus.SUBMITTED -> tr("In review", "পর্যালোচনায়")
                            else -> tr("Not verified", "যাচাই হয়নি")
                        }
                        MenuRow(Icons.Filled.VerifiedUser, tr("Identity verification", "পরিচয় যাচাই"), { nav.navigate(Routes.VERIFY) }, vtext)
                        HorizontalDivider(color = Border)
                        if (me.isSeeker) {
                            MenuRow(Icons.Filled.Person, tr("My public profile & reviews", "আমার পাবলিক প্রোফাইল ও রিভিউ"), { nav.navigate(Routes.seeker(me.id)) })
                            HorizontalDivider(color = Border)
                            MenuRow(Icons.Filled.Payments, tr("Earnings", "আয়"), { nav.navigate(Routes.EARNINGS) })
                            HorizontalDivider(color = Border)
                            MenuRow(Icons.Filled.Bookmark, tr("Saved jobs", "সংরক্ষিত কাজ"), { nav.navigate(Routes.SAVED) }, num(me.savedJobIds.size))
                            HorizontalDivider(color = Border)
                        }
                        MenuRow(Icons.Filled.History, tr("Job history", "কাজের ইতিহাস"), { nav.navigate(Routes.HISTORY) })
                        HorizontalDivider(color = Border)
                        MenuRow(Icons.Filled.Notifications, tr("Notifications", "নোটিফিকেশন"), { nav.navigate(Routes.NOTICES) })
                        HorizontalDivider(color = Border)
                        MenuRow(Icons.Filled.Settings, tr("Settings & help", "সেটিংস ও সহায়তা"), { nav.navigate(Routes.SETTINGS) })
                    }
                }
            }
        }
    }
}

@Composable
fun ModeCard(emoji: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PrimaryLight else CardColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Primary else Border)
    ) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, fontWeight = FontWeight.Bold, color = if (selected) Primary else MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = Canvas) {
        Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(label, color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ReportDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    val reasons = listOf(
        "Spam or scam" to "স্প্যাম বা প্রতারণা",
        "Fake profile or job" to "ভুয়া প্রোফাইল বা কাজ",
        "Rude or abusive" to "অভদ্র বা আপত্তিকর আচরণ",
        "Unsafe or illegal" to "অনিরাপদ বা বেআইনি",
        "Other" to "অন্যান্য"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Report", "রিপোর্ট করুন")) },
        text = {
            Column {
                for ((en, bn) in reasons) {
                    Text(
                        tr(en, bn),
                        modifier = Modifier.fillMaxWidth().clickable { onSubmit(en) }.padding(vertical = 12.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(color = Border)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "বাতিল")) } }
    )
}

@Composable
fun SeekerProfileScreen(vm: AppViewModel, nav: NavHostController, id: String) {
    val d by vm.state.collectAsState()
    val now = LocalNow.current
    val lang = d.lang
    val me = d.me
    val r = Queries.rankedOne(d, id, now)
    var showReport by remember { mutableStateOf(false) }
    var showBlock by remember { mutableStateOf(false) }
    if (r == null || me == null) {
        ScreenFrame(title = tr("Profile", "প্রোফাইল"), onBack = { nav.popBackStack() }) { pad ->
            EmptyState("🤷", tr("Profile not found", "প্রোফাইল পাওয়া যায়নি"), modifier = Modifier.padding(pad))
        }
        return
    }
    val u = r.user
    val s = r.stats
    val reviews = Queries.reviewsFor(d, id, true)
    ScreenFrame(
        title = u.name, onBack = { nav.popBackStack() },
        actions = {
            if (u.id != me.id) {
                IconButton(onClick = { vm.toggleFavorite(u.id) }) {
                    Icon(
                        if (u.id in me.favoriteSeekerIds) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null, tint = if (u.id in me.favoriteSeekerIds) Accent else Muted
                    )
                }
                IconButton(onClick = { showReport = true }) { Icon(Icons.Filled.Flag, contentDescription = tr("Report", "রিপোর্ট")) }
            }
        },
        bottomBar = {
            if (u.id != me.id && d.mode == Mode.CLIENT) {
                Surface(color = CardColor, shadowElevation = 8.dp) {
                    Column(Modifier.padding(14.dp)) {
                        PrimaryButton(
                            tr("Invite to a job", "কাজে আমন্ত্রণ জানান"),
                            onClick = { nav.navigate(Routes.post(u.categories.firstOrNull() ?: "", u.id)) },
                            icon = Icons.Filled.Work
                        )
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.name, u.photoPath, 76.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            VerifiedBadge(u.level)
                            Text(Catalog.area(u.areaId).full(lang), color = Muted, fontSize = 13.sp)
                            if (s.ratingCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RatingStars(s.ratingAvg)
                                    Spacer(Modifier.width(6.dp))
                                    Text(rating(s.ratingAvg) + " (" + num(s.ratingCount) + ")", fontSize = 13.sp)
                                }
                            } else Pill(tr("New on Odd Jobs", "নতুন"), AccentLight, Accent)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(num(Math.round(r.trust).toInt()), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
                            Text(tr("Trust", "ট্রাস্ট"), color = Muted, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox(tr("Jobs done", "কাজ সম্পন্ন"), num(s.completed), Modifier.weight(1f))
                        StatBox(
                            tr("Replies in", "উত্তর দেয়"),
                            if (s.medianResponseMin > 0) num(s.medianResponseMin) + tr(" min", " মিনিটে") else "—",
                            Modifier.weight(1f)
                        )
                        StatBox(tr("On time", "সময়মতো"), num(Math.round(s.onTimeRate * 100).toInt()) + "%", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        money(u.rateMin) + " – " + money(u.rateMax) + " " + tr("per job", "প্রতি কাজ") + " · " +
                            num(u.experienceYears) + " " + tr("yrs experience", "বছরের অভিজ্ঞতা"),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (u.bio.isNotBlank()) {
                item {
                    AppCard {
                        Text(tr("About", "পরিচিতি"), fontWeight = FontWeight.Bold)
                        Text(u.bio)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (c in u.categories) Pill(Catalog.category(c).emoji + " " + Catalog.category(c).name(lang))
                        }
                    }
                }
            }
            if (s.ratingCount > 0) {
                item {
                    AppCard {
                        Text(tr("Ratings", "রেটিং"), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        for (star in 5 downTo 1) {
                            val count = s.histogram[star - 1]
                            val frac = if (s.ratingCount > 0) count.toFloat() / s.ratingCount else 0f
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                                Text(num(star) + "★", modifier = Modifier.width(34.dp), fontSize = 13.sp)
                                Box(Modifier.weight(1f).height(8.dp).background(Border, RoundedCornerShape(50))) {
                                    Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().background(Accent, RoundedCornerShape(50)))
                                }
                                Text(num(count), modifier = Modifier.width(34.dp), textAlign = TextAlign.End, fontSize = 13.sp, color = Muted)
                            }
                        }
                        if (s.topTags.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for ((tag, n) in s.topTags) {
                                    val good = tag in Ranking.positiveTags
                                    Pill(tagLabel(tag) + " · " + num(n), if (good) SuccessBg else WarnBg, if (good) Success else Warn)
                                }
                            }
                        }
                    }
                }
            }
            item { SectionHeader(tr("Reviews", "রিভিউ") + " (" + num(reviews.size) + ")") }
            if (reviews.isEmpty()) {
                item { Text(tr("No reviews yet.", "এখনো কোনো রিভিউ নেই।"), color = Muted) }
            }
            items(reviews.take(30)) { rv ->
                val who = d.user(rv.reviewerId)
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(who?.name ?: "?", who?.photoPath, 32.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(who?.name ?: "", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(ago(rv.createdAt, now), color = Muted, fontSize = 11.sp)
                        }
                        RatingStars(rv.rating.toDouble(), 14.dp)
                    }
                    if (rv.comment.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(rv.comment)
                    }
                    if (rv.tags.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (t in rv.tags) {
                                val good = t in Ranking.positiveTags
                                Pill(tagLabel(t), if (good) SuccessBg else WarnBg, if (good) Success else Warn)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = Success, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Verified job", "যাচাইকৃত কাজ"), color = Success, fontSize = 11.sp)
                    }
                }
            }
            if (u.id != me.id) {
                item {
                    TextButton(onClick = { showBlock = true }) {
                        Text(tr("Block this user", "এই ব্যবহারকারীকে ব্লক করুন"), color = ErrorRed)
                    }
                }
            }
        }
    }
    if (showReport) {
        ReportDialog(onDismiss = { showReport = false }, onSubmit = {
            vm.report("user", id, it); showReport = false
            vm.toast(if (lang == "bn") "রিপোর্ট পাঠানো হয়েছে। ধন্যবাদ।" else "Report sent. Thank you.")
        })
    }
    if (showBlock) {
        ConfirmDialog(
            tr("Block ${u.name}?", "${u.name} কে ব্লক করবেন?"),
            tr("You will no longer see their jobs or messages.", "তাঁর কাজ বা বার্তা আর দেখতে পাবেন না।"),
            tr("Block", "ব্লক করুন"),
            onConfirm = { vm.block(id); showBlock = false; nav.popBackStack() },
            onDismiss = { showBlock = false }, danger = true
        )
    }
}
