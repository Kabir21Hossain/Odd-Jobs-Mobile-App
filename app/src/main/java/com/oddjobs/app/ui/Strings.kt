package com.oddjobs.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.oddjobs.app.domain.JobStatus
import com.oddjobs.app.domain.PayMethod
import com.oddjobs.app.domain.Urgency

val LocalLang = compositionLocalOf { "bn" }

/** Inline bilingual text: tr("English", "বাংলা"). */
@Composable
fun tr(en: String, bn: String): String = if (LocalLang.current == "bn") bn else en

fun bnDigits(s: String): String {
    val sb = StringBuilder()
    for (c in s) sb.append(if (c in '0'..'9') '০' + (c - '0') else c)
    return sb.toString()
}

fun digitsFor(lang: String, s: String): String = if (lang == "bn") bnDigits(s) else s

/** Bangladeshi digit grouping: 1,00,000 */
fun groupBd(n: Int): String {
    val neg = n < 0
    val s = Math.abs(n).toString()
    if (s.length <= 3) return (if (neg) "-" else "") + s
    val last3 = s.takeLast(3)
    var rest = s.dropLast(3)
    val parts = ArrayList<String>()
    while (rest.length > 2) {
        parts.add(0, rest.takeLast(2))
        rest = rest.dropLast(2)
    }
    if (rest.isNotEmpty()) parts.add(0, rest)
    return (if (neg) "-" else "") + parts.joinToString(",") + "," + last3
}

@Composable
fun num(v: Any): String = digitsFor(LocalLang.current, v.toString())

@Composable
fun money(v: Int): String = digitsFor(LocalLang.current, "৳" + groupBd(v))

@Composable
fun rating(v: Double): String = digitsFor(LocalLang.current, String.format(java.util.Locale.US, "%.1f", v))

@Composable
fun ago(at: Long, now: Long): String {
    val mins = ((now - at) / 60_000L).coerceAtLeast(0)
    return when {
        mins < 1 -> tr("just now", "এইমাত্র")
        mins < 60 -> tr("${mins} min ago", "${bnDigits(mins.toString())} মিনিট আগে")
        mins < 60 * 24 -> tr("${mins / 60} h ago", "${bnDigits((mins / 60).toString())} ঘণ্টা আগে")
        else -> tr("${mins / 1440} d ago", "${bnDigits((mins / 1440).toString())} দিন আগে")
    }
}

@Composable
fun urgencyLabel(u: Urgency): String = when (u) {
    Urgency.NOW -> tr("Right now", "এখনই")
    Urgency.TODAY -> tr("Today", "আজ")
    Urgency.THIS_WEEK -> tr("This week", "এই সপ্তাহে")
    Urgency.FLEXIBLE -> tr("Flexible", "সময় নমনীয়")
}

@Composable
fun payLabel(p: PayMethod): String = when (p) {
    PayMethod.CASH -> tr("Cash", "নগদ")
    PayMethod.BKASH -> "bKash"
    PayMethod.NAGAD -> tr("Nagad", "নগদ (Nagad)")
}

@Composable
fun statusLabel(s: JobStatus): String = when (s) {
    JobStatus.OPEN -> tr("Open", "খোলা")
    JobStatus.HIRED -> tr("Hired", "নিয়োগ হয়েছে")
    JobStatus.ON_THE_WAY -> tr("On the way", "রওনা দিয়েছে")
    JobStatus.ARRIVED -> tr("Arrived", "পৌঁছেছে")
    JobStatus.IN_PROGRESS -> tr("In progress", "কাজ চলছে")
    JobStatus.AWAITING_CONFIRMATION -> tr("Confirm work", "নিশ্চিত করুন")
    JobStatus.COMPLETED -> tr("Completed", "সম্পন্ন")
    JobStatus.CANCELLED -> tr("Cancelled", "বাতিল")
    JobStatus.EXPIRED -> tr("Expired", "মেয়াদ শেষ")
}

@Composable
fun tagLabel(key: String): String = when (key) {
    "punctual" -> tr("Punctual", "সময়মতো")
    "polite" -> tr("Polite", "বিনয়ী")
    "quality" -> tr("Quality work", "ভালো কাজ")
    "fair_price" -> tr("Fair price", "ন্যায্য দাম")
    "clean" -> tr("Clean", "পরিচ্ছন্ন")
    "communication" -> tr("Good communication", "ভালো যোগাযোগ")
    "late" -> tr("Late", "দেরি")
    "rude" -> tr("Rude", "অভদ্র")
    "poor_quality" -> tr("Poor quality", "খারাপ কাজ")
    "overpriced" -> tr("Overpriced", "বেশি দাম")
    else -> key
}

object Errors {
    private val map = mapOf(
        "phone_invalid" to ("Enter a valid Bangladeshi mobile number (01XXXXXXXXX)." to "সঠিক মোবাইল নম্বর দিন (01XXXXXXXXX)।"),
        "otp_wrong" to ("That code is not correct." to "কোডটি সঠিক নয়।"),
        "otp_expired" to ("This code has expired. Request a new one." to "কোডের মেয়াদ শেষ। নতুন কোড নিন।"),
        "name_required" to ("Please enter your name." to "আপনার নাম লিখুন।"),
        "category_required" to ("Please choose a category." to "একটি ক্যাটেগরি বাছাই করুন।"),
        "bad_rate" to ("Minimum rate cannot be above maximum." to "সর্বনিম্ন রেট সর্বোচ্চের বেশি হতে পারে না।"),
        "need_photos" to ("Please add your NID photo and a selfie." to "NID এর ছবি ও একটি সেলফি দিন।"),
        "bad_budget" to ("Budget must be between ৳50 and ৳10,00,000." to "বাজেট ৳৫০ থেকে ৳১০,০০,০০০ এর মধ্যে হতে হবে।"),
        "too_long" to ("Text is too long." to "লেখা অনেক বড়।"),
        "not_seeker" to ("Set up your worker profile first." to "আগে কর্মী প্রোফাইল তৈরি করুন।"),
        "not_verified" to ("Verify your identity (NID) to apply for jobs." to "কাজে আবেদনের জন্য পরিচয় (NID) যাচাই করুন।"),
        "own_job" to ("You cannot apply to your own job." to "নিজের কাজে আবেদন করা যায় না।"),
        "not_open" to ("This job is no longer open." to "কাজটি আর খোলা নেই।"),
        "bad_price" to ("Enter a valid price." to "সঠিক দাম লিখুন।"),
        "rating_low" to ("This client requires a higher rating." to "এই ক্লায়েন্ট আরও বেশি রেটিং চান।"),
        "no_application" to ("That offer is no longer available." to "অফারটি আর নেই।"),
        "not_found" to ("Not found." to "পাওয়া যায়নি।"),
        "not_allowed" to ("You are not allowed to do that." to "আপনি এটি করতে পারবেন না।"),
        "bad_transition" to ("That step is not allowed right now." to "এই ধাপটি এখন করা যাবে না।"),
        "code_wrong" to ("Wrong arrival code. Ask the client for the 4-digit code." to "ভুল কোড। ক্লায়েন্টের ৪ সংখ্যার কোড চান।"),
        "not_completed" to ("The job must be completed first." to "আগে কাজ সম্পন্ন হতে হবে।"),
        "bad_rating" to ("Choose 1 to 5 stars." to "১ থেকে ৫ তারা দিন।"),
        "already_reviewed" to ("You already reviewed this job." to "আপনি ইতিমধ্যে রিভিউ দিয়েছেন।"),
        "empty" to ("Type a message first." to "আগে বার্তা লিখুন।"),
        "blocked" to ("You cannot message this user." to "এই ব্যবহারকারীকে বার্তা পাঠানো যাবে না।")
    )

    fun text(key: String, lang: String): String {
        val p = map[key] ?: return key
        return if (lang == "bn") p.second else p.first
    }
}

val LocalNow = androidx.compose.runtime.compositionLocalOf { System.currentTimeMillis() }

fun clockOf(at: Long, lang: String): String =
    digitsFor(lang, java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.US).format(java.util.Date(at)))
