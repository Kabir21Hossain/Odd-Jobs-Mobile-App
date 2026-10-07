package com.oddjobs.app.domain

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object Sentiment {
    private val positiveEn = setOf(
        "good", "great", "excellent", "nice", "polite", "punctual", "professional", "quick", "fast",
        "clean", "perfect", "best", "recommend", "helpful", "honest", "satisfied", "thanks", "amazing", "skilled"
    )
    private val negativeEn = setOf(
        "bad", "late", "rude", "poor", "slow", "dirty", "worst", "cheat", "overcharged",
        "unprofessional", "careless", "damage", "damaged", "broke", "waste", "delay", "delayed", "noshow"
    )
    private val positiveBn = listOf(
        "ভালো", "ভাল", "চমৎকার", "দারুণ", "সময়মতো", "দ্রুত", "পরিষ্কার", "নিখুঁত", "ধন্যবাদ",
        "বিনয়ী", "সৎ", "দক্ষ", "অসাধারণ", "সন্তুষ্ট", "প্রফেশনাল"
    )
    private val negativeBn = listOf(
        "খারাপ", "দেরি", "দেরী", "বাজে", "ধীর", "নোংরা", "প্রতারণা", "অভদ্র", "ঠকিয়েছে",
        "ক্ষতি", "ভেঙে", "সময় নষ্ট", "অসন্তুষ্ট", "ফাঁকি"
    )
    private val splitter = Regex("[\\s,.!?।;:()\"'\\-]+")

    /** Returns a value in (-1, 1). Light-weight lexicon for English and Bangla. */
    fun score(comment: String): Double {
        val text = comment.trim().lowercase()
        if (text.isEmpty()) return 0.0
        val tokens = text.split(splitter).filter { it.isNotEmpty() }.toSet()
        var pos = tokens.count { it in positiveEn }
        var neg = tokens.count { it in negativeEn }
        pos += positiveBn.count { text.contains(it) }
        neg += negativeBn.count { text.contains(it) }
        return (pos - neg).toDouble() / (pos + neg + 1)
    }
}

data class SeekerStats(
    val ratingAvg: Double,
    val ratingCount: Int,
    val histogram: List<Int>,
    val completed: Int,
    val cancelledBySeeker: Int,
    val medianResponseMin: Int,
    val completedLast30: Int,
    val onTimeRate: Double,
    val positiveTagRatio: Double,
    val sentiment01: Double,
    val topTags: List<Pair<String, Int>>
)

data class RankedSeeker(
    val user: User,
    val stats: SeekerStats,
    val trust: Double,
    val distanceKm: Double,
    val match: Double
)

object Ranking {
    const val GLOBAL_AVG = 4.3
    const val PRIOR_M = 5.0
    const val HALF_LIFE_DAYS = 180.0
    const val DAY_MS = 86_400_000L

    val positiveTags = listOf("punctual", "polite", "quality", "fair_price", "clean", "communication")
    val negativeTags = listOf("late", "rude", "poor_quality", "overpriced")

    fun stats(seekerId: String, jobs: List<Job>, reviews: List<Review>, now: Long): SeekerStats {
        val rs = reviews.filter { it.revieweeId == seekerId && it.revieweeIsSeeker }
        var wSum = 0.0
        var wTotal = 0.0
        val hist = MutableList(5) { 0 }
        for (r in rs) {
            val ageDays = max(0.0, (now - r.createdAt).toDouble() / DAY_MS)
            val w = 0.5.pow(ageDays / HALF_LIFE_DAYS)
            wSum += w * r.rating
            wTotal += w
            hist[(r.rating - 1).coerceIn(0, 4)] += 1
        }
        val avg = if (wTotal > 0) wSum / wTotal else 0.0

        val hired = jobs.filter { it.hiredSeekerId == seekerId }
        val completedJobs = hired.filter { it.status == JobStatus.COMPLETED }
        val cancelledBySeeker = hired.count { it.status == JobStatus.CANCELLED && it.cancelledBy == seekerId }
        val last30 = completedJobs.count { it.completedAt >= now - 30 * DAY_MS }
        val onTime = (completedJobs.count { it.onTime } + 1.0) / (completedJobs.size + 1.0)

        val responses = jobs.flatMap { it.applications }
            .filter { it.seekerId == seekerId }
            .map { it.responseMin }
            .sorted()
        val median = if (responses.isEmpty()) 0 else responses[responses.size / 2]

        val allTags = rs.flatMap { it.tags }
        val pos = allTags.count { it in positiveTags }
        val neg = allTags.count { it in negativeTags }
        val tagRatio = if (pos + neg == 0) 0.5 else pos.toDouble() / (pos + neg)

        val commented = rs.filter { it.comment.isNotBlank() }
        val sent = if (commented.isEmpty()) 0.5
        else commented.map { (Sentiment.score(it.comment) + 1.0) / 2.0 }.average()

        val top = allTags.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.take(4).map { it.key to it.value }

        return SeekerStats(
            ratingAvg = avg, ratingCount = rs.size, histogram = hist,
            completed = completedJobs.size, cancelledBySeeker = cancelledBySeeker,
            medianResponseMin = median, completedLast30 = last30, onTimeRate = onTime,
            positiveTagRatio = tagRatio, sentiment01 = sent, topTags = top
        )
    }

    fun bayesianRating(avg: Double, count: Int): Double {
        val v = count.toDouble()
        val a = if (count > 0) avg else GLOBAL_AVG
        return (v * a + PRIOR_M * GLOBAL_AVG) / (v + PRIOR_M)
    }

    fun responseScore(medianMin: Int): Double {
        if (medianMin <= 0) return 0.5
        if (medianMin <= 5) return 1.0
        if (medianMin >= 360) return 0.0
        return (1.0 - ln(medianMin / 5.0) / ln(72.0)).coerceIn(0.0, 1.0)
    }

    /** TrustScore 0..100. See docs/RANKING.md. */
    fun trust(user: User, s: SeekerStats): Double {
        val r = ((bayesianRating(s.ratingAvg, s.ratingCount) - 1.0) / 4.0).coerceIn(0.0, 1.0)
        val c = (s.completed + 2.0) / (s.completed + s.cancelledBySeeker + 2.0)
        val t = responseScore(s.medianResponseMin)
        val v = when (user.level) { 1 -> 0.3; 2 -> 0.7; else -> 1.0 }
        val q = 0.5 * s.positiveTagRatio + 0.5 * s.sentiment01
        val a = 0.6 * min(1.0, s.completedLast30 / 5.0) + 0.4 * s.onTimeRate
        val raw = 100.0 * (0.35 * r + 0.20 * c + 0.15 * t + 0.10 * v + 0.10 * q + 0.10 * a)
        val penalty = min(15.0, 3.0 * s.cancelledBySeeker)
        return (raw - penalty).coerceIn(0.0, 100.0)
    }

    fun proximity(distKm: Double, radiusKm: Int): Double {
        if (distKm <= 1.0) return 1.0
        if (radiusKm <= 1 || distKm >= radiusKm) return 0.0
        return 1.0 - (distKm - 1.0) / (radiusKm - 1.0)
    }

    fun priceFit(budget: Int, rateMin: Int, rateMax: Int): Double {
        if (budget in rateMin..rateMax) return 1.0
        if (budget < rateMin) return (budget.toDouble() / max(1, rateMin)).coerceIn(0.0, 1.0)
        return (rateMax.toDouble() / max(1, budget)).coerceIn(0.0, 1.0)
    }

    fun match(trust: Double, proximity: Double, categoryCompleted: Int, priceFit: Double): Double =
        0.55 * trust / 100.0 + 0.20 * proximity + 0.15 * min(1.0, categoryCompleted / 5.0) + 0.10 * priceFit

    fun completedInCategory(seekerId: String, categoryId: String, jobs: List<Job>): Int =
        jobs.count { it.hiredSeekerId == seekerId && it.status == JobStatus.COMPLETED && it.categoryId == categoryId }

    /** Why this seeker ranks well, as short keys used by the UI. */
    fun isNew(s: SeekerStats) = s.ratingCount < 3
}
