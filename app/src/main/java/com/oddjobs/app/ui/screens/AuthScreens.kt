@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.oddjobs.app.ui.screens

import android.net.Uri
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import androidx.compose.ui.res.painterResource
import com.oddjobs.app.R

@Composable
fun WelcomeScreen(vm: AppViewModel, nav: NavHostController) {
    Column(
        Modifier.fillMaxSize().background(Canvas).padding(28.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(R.drawable.logo_mark), contentDescription = "Odd Jobs",
            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(28.dp))
        )
        Spacer(Modifier.height(20.dp))
        Text("Odd Jobs", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Trusted local help, one tap away",
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center
        )
        Text(
            "à¦¬à¦¿à¦¶à§à¦¬à¦¸à§à¦¤ à¦¸à§à¦¥à¦¾à¦¨à§€à¦¯à¦¼ à¦•à¦°à§à¦®à§€, à¦®à¦¾à¦¤à§à¦° à¦à¦• à¦Ÿà§à¦¯à¦¾à¦ªà§‡",
            style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, color = Muted
        )
        Spacer(Modifier.height(32.dp))
        Text("à¦­à¦¾à¦·à¦¾ à¦¬à¦¾à¦›à¦¾à¦‡ à¦•à¦°à§à¦¨ Â· Choose your language", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        PrimaryButton("à¦¬à¦¾à¦‚à¦²à¦¾", onClick = { vm.chooseLang("bn"); nav.navigate(Routes.LOGIN) })
        Spacer(Modifier.height(10.dp))
        SecondaryButton("English", onClick = { vm.chooseLang("en"); nav.navigate(Routes.LOGIN) })
    }
}

@Composable
fun LoginScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val auth by vm.authState.collectAsState()
    var phone by remember { mutableStateOf("") }
    val valid = Validators.normalizeBdPhone(phone) != null
    LaunchedEffect(auth.authenticated) {
        if (auth.authenticated) {
            val me = vm.state.value.me
            if (me == null || !me.profileDone) nav.resetTo(Routes.SETUP)
            else nav.resetTo(Routes.tabFor(vm.state.value.mode))
        }
    }
    Column(
        Modifier.fillMaxSize().background(Canvas).padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(32.dp))
        Image(
            painterResource(R.drawable.logo_mark), contentDescription = null,
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp))
        )
        Spacer(Modifier.height(20.dp))
        Text(tr("Welcome to Odd Jobs", "à¦…à¦¡ à¦œà¦¬à¦¸à§‡ à¦¸à§à¦¬à¦¾à¦—à¦¤à¦®"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Enter your mobile number. We will send a 6-digit code to verify it.", "à¦†à¦ªà¦¨à¦¾à¦° à¦®à§‹à¦¬à¦¾à¦‡à¦² à¦¨à¦®à§à¦¬à¦° à¦¦à¦¿à¦¨à¥¤ à¦¯à¦¾à¦šà¦¾à¦‡à¦¯à¦¼à§‡à¦° à¦œà¦¨à§à¦¯ à§¬ à¦¸à¦‚à¦–à§à¦¯à¦¾à¦° à¦•à§‹à¦¡ à¦ªà¦¾à¦ à¦¾à¦¨à§‹ à¦¹à¦¬à§‡à¥¤"),
            color = Muted
        )
        Spacer(Modifier.height(24.dp))
        AppTextField(
            phone, { phone = it }, tr("Mobile number", "à¦®à§‹à¦¬à¦¾à¦‡à¦² à¦¨à¦®à§à¦¬à¦°"),
            keyboard = KeyboardType.Phone, prefix = "+88  ", maxLength = 14,
            error = phone.length >= 11 && !valid,
            supporting = tr("Example: 01712345678", "à¦¯à§‡à¦®à¦¨: à§¦à§§à§­à§§à§¨à§©à§ªà§«à§¬à§­à§®")
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            tr("Send code", "à¦•à§‹à¦¡ à¦ªà¦¾à¦ à¦¾à¦¨"),
            onClick = {
                val out = vm.requestOtp(phone)
                if (out.ok) nav.navigate(Routes.OTP)
            },
            enabled = valid && !auth.loading
        )
        Spacer(Modifier.height(22.dp))
        Text(tr("Demo sign in works offline. The code will appear on the next screen.", "ডেমো লগইন অফলাইনে কাজ করে। পরের স্ক্রিনে কোডটি দেখানো হবে।"), color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Language, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            TextButton(onClick = { vm.chooseLang(if (d.lang == "bn") "en" else "bn") }) {
                Text(if (d.lang == "bn") "Switch to English" else "à¦¬à¦¾à¦‚à¦²à¦¾à¦¯à¦¼ à¦¦à§‡à¦–à§à¦¨")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            tr(
                "By continuing you agree to our Terms of Service and Privacy Policy.",
                "à¦šà¦¾à¦²à¦¿à¦¯à¦¼à§‡ à¦—à§‡à¦²à§‡ à¦†à¦ªà¦¨à¦¿ à¦†à¦®à¦¾à¦¦à§‡à¦° à¦¶à¦°à§à¦¤à¦¾à¦¬à¦²à§€ à¦“ à¦—à§‹à¦ªà¦¨à§€à¦¯à¦¼à¦¤à¦¾ à¦¨à§€à¦¤à¦¿à¦¤à§‡ à¦¸à¦®à§à¦®à¦¤ à¦¹à¦šà§à¦›à§‡à¦¨à¥¤"
            ),
            color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun OtpScreen(vm: AppViewModel, nav: NavHostController) {
    val d by vm.state.collectAsState()
    val auth by vm.authState.collectAsState()
    var code by remember { mutableStateOf("") }
    var sentAt by remember { mutableStateOf(d.otpSentAt) }
    var tick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(1000)
        }
    }
    val left = (60 - (tick - d.otpSentAt) / 1000).toInt().coerceAtLeast(0)
    val pendingPhone = d.pendingPhone ?: ""
    LaunchedEffect(auth.authenticated) {
        if (auth.authenticated) {
            val me = vm.state.value.me
            if (me == null || !me.profileDone) nav.resetTo(Routes.SETUP)
            else nav.resetTo(Routes.tabFor(vm.state.value.mode))
        }
    }
    ScreenFrame(title = tr("Verify number", "à¦¨à¦®à§à¦¬à¦° à¦¯à¦¾à¦šà¦¾à¦‡"), onBack = { nav.popBackStack() }) { pad ->
        Column(Modifier.padding(pad).padding(24.dp).verticalScroll(rememberScrollState())) {
            Text(
                tr("Enter the 6-digit code sent to", "à§¬ à¦¸à¦‚à¦–à§à¦¯à¦¾à¦° à¦•à§‹à¦¡à¦Ÿà¦¿ à¦¦à¦¿à¦¨, à¦ªà¦¾à¦ à¦¾à¦¨à§‹ à¦¹à¦¯à¦¼à§‡à¦›à§‡") + "\n" + digitsFor(d.lang, pendingPhone),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            if (d.pendingOtp != null) {
                AppCard {
                    Text(tr("Demo mode", "à¦¡à§‡à¦®à§‹ à¦®à§‹à¦¡"), fontWeight = FontWeight.Bold, color = Warn)
                    Text(
                        tr(
                            "No SMS gateway is connected yet, so your code is shown here:",
                            "à¦à¦–à¦¨à§‹ SMS à¦—à§‡à¦Ÿà¦“à¦¯à¦¼à§‡ à¦¯à§à¦•à§à¦¤ à¦¨à§‡à¦‡, à¦¤à¦¾à¦‡ à¦†à¦ªà¦¨à¦¾à¦° à¦•à§‹à¦¡ à¦à¦–à¦¾à¦¨à§‡ à¦¦à§‡à¦–à¦¾à¦¨à§‹ à¦¹à¦šà§à¦›à§‡:"
                        ),
                        color = Muted, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            digitsFor(d.lang, d.pendingOtp ?: ""), fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold, color = Primary, letterSpacing = 4.sp
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { code = d.pendingOtp ?: "" }) { Text(tr("Autofill", "à¦…à¦Ÿà§‹à¦«à¦¿à¦²")) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            AppTextField(code, { code = it }, tr("6-digit code", "à§¬ à¦¸à¦‚à¦–à§à¦¯à¦¾à¦° à¦•à§‹à¦¡"), keyboard = KeyboardType.NumberPassword, maxLength = 6)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                tr("Verify & continue", "à¦¯à¦¾à¦šà¦¾à¦‡ à¦•à¦°à§‡ à¦à¦—à¦¿à¦¯à¦¼à§‡ à¦¯à¦¾à¦¨"),
                onClick = {
                    val out = vm.verifyOtp(code)
                    if (out.ok) {
                        val me = vm.state.value.me
                        if (me == null || !me.profileDone) nav.resetTo(Routes.SETUP)
                        else nav.resetTo(Routes.tabFor(vm.state.value.mode))
                    }
                },
                enabled = code.length == 6 && !auth.loading
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val out = vm.requestOtp(pendingPhone)
                    if (out.ok) { code = ""; sentAt = vm.state.value.otpSentAt }
                },
                enabled = left == 0,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    if (left > 0) tr("Resend code in ${left}s", "${digitsFor(d.lang, left.toString())} à¦¸à§‡à¦•à§‡à¦¨à§à¦¡ à¦ªà¦° à¦†à¦¬à¦¾à¦° à¦•à§‹à¦¡ à¦¨à¦¿à¦¨")
                    else tr("Resend code", "à¦†à¦¬à¦¾à¦° à¦•à§‹à¦¡ à¦ªà¦¾à¦ à¦¾à¦¨")
                )
            }
        }
    }
}



