package com.oddjobs.app.domain

import kotlin.math.max

data class JobFilter(
    val query: String = "",
    val categories: Set<String> = emptySet(),
    val district: String? = null,
    val areaId: String? = null,
    val maxKm: Int = 0,
    val minBudget: Int = 0,
    val urgency: Urgency? = null,
    val sort: SortBy = SortBy.BEST_MATCH
) {
    val activeCount: Int
        get() = listOf(
            categories.isNotEmpty(), district != null || areaId != null, maxKm > 0, minBudget > 0, urgency != null
        ).count { it }
}

data class FeedItem(
    val job: Job,
    val client: User?,
    val distanceKm: Double,
    val match: Double,
    val myApplication: Application?
)

data class ChatSummary(
    val chatId: String,
    val job: Job,
    val other: User,
    val last: ChatMessage,
    val unread: Int
)

data class Earnings(val today: Int, val week: Int, val month: Int, val total: Int, val jobs: Int)

object Queries {
    private const val DAY = 86_400_000L

    fun feed(d: AppData, me: User, f: JobFilter, now: Long): List<FeedItem> {
        val q = f.query.trim().lowercase()
        val items = d.jobs.asSequence()
            .filter { it.status == JobStatus.OPEN && it.clientId != me.id && it.id !in me.hiddenJobIds }
            .filter { j -> d.user(j.clientId)?.let { me.id !in it.blockedIds && it.id !in me.blockedIds } != false }
            .filter { f.categories.isEmpty() || it.categoryId in f.categories }
            .filter { j ->
                val a = Catalog.area(j.areaId)
                (f.district == null || a.district == f.district) && (f.areaId == null || a.id == f.areaId)
            }
            .filter { it.budget >= f.minBudget }
            .filter { f.urgency == null || it.urgency == f.urgency }
            .filter { j ->
                if (q.isEmpty()) true else {
                    val c = Catalog.category(j.categoryId)
                    val a = Catalog.area(j.areaId)
                    listOf(j.title, j.description, c.en, c.bn, a.en, a.bn, a.district, a.districtBn, j.landmark)
                        .any { it.lowercase().contains(q) }
                }
            }
            .map { j ->
                val dist = Geo.between(me.areaId, j.areaId)
                FeedItem(j, d.user(j.clientId), dist, jobMatch(me, j, dist, now), j.applications.firstOrNull { it.seekerId == me.id && it.status != AppStatus.WITHDRAWN })
            }
            .filter { f.maxKm <= 0 || it.distanceKm <= f.maxKm }
            .toList()
        return when (f.sort) {
            SortBy.NEAREST -> items.sortedBy { it.distanceKm }
            SortBy.NEWEST -> items.sortedByDescending { it.job.createdAt }
            SortBy.BUDGET -> items.sortedByDescending { it.job.budget }
            SortBy.BEST_MATCH -> items.sortedByDescending { it.match }
        }
    }

    fun jobMatch(me: User, j: Job, distKm: Double, now: Long): Double {
        val cat = if (j.categoryId in me.categories) 1.0 else 0.3
        val prox = Ranking.proximity(distKm, me.radiusKm)
        val fit = Ranking.priceFit(j.budget, me.rateMin, me.rateMax)
        val fresh = (1.0 - (now - j.createdAt).toDouble() / (7 * DAY)).coerceIn(0.0, 1.0)
        return 0.5 * prox + 0.25 * cat + 0.15 * fit + 0.10 * fresh
    }

    fun ranked(
        d: AppData, categoryId: String?, areaId: String, excludeId: String?, now: Long,
        onlyVerified: Boolean = false, minRating: Double = 0.0, budget: Int = 0
    ): List<RankedSeeker> {
        return d.users.asSequence()
            .filter { it.isSeeker && it.profileDone && it.id != excludeId }
            .filter { excludeId == null || d.user(excludeId)?.blockedIds?.contains(it.id) != true }
            .filter { categoryId == null || categoryId in it.categories }
            .filter { !onlyVerified || it.level >= 2 }
            .map { u ->
                val st = Ranking.stats(u.id, d.jobs, d.reviews, now)
                val trust = Ranking.trust(u, st)
                val dist = Geo.between(areaId, u.areaId)
                val prox = Ranking.proximity(dist, u.radiusKm)
                val fit = if (budget > 0) Ranking.priceFit(budget, u.rateMin, u.rateMax) else 1.0
                val done = if (categoryId != null) Ranking.completedInCategory(u.id, categoryId, d.jobs) else st.completed
                RankedSeeker(u, st, trust, dist, Ranking.match(trust, prox, done, fit))
            }
            .filter { minRating <= 0.0 || it.stats.ratingCount == 0 || it.stats.ratingAvg >= minRating }
            .sortedByDescending { it.match }
            .toList()
    }

    fun rankedOne(d: AppData, seekerId: String, now: Long): RankedSeeker? {
        val u = d.user(seekerId) ?: return null
        val st = Ranking.stats(seekerId, d.jobs, d.reviews, now)
        val trust = Ranking.trust(u, st)
        return RankedSeeker(u, st, trust, 0.0, trust / 100.0)
    }

    fun chats(d: AppData, userId: String): List<ChatSummary> {
        val byChat = d.messages.groupBy { it.chatId }
        val out = ArrayList<ChatSummary>()
        for ((chatId, msgs) in byChat) {
            val job = d.job(jobIdOfChat(chatId)) ?: continue
            val seekerId = seekerIdOfChat(chatId)
            val otherId = when (userId) {
                job.clientId -> seekerId
                seekerId -> job.clientId
                else -> continue
            }
            val other = d.user(otherId) ?: continue
            val last = msgs.maxByOrNull { it.at } ?: continue
            val readAt = d.chatRead["$userId|$chatId"] ?: 0L
            val unread = msgs.count { it.senderId != userId && it.at > readAt }
            out.add(ChatSummary(chatId, job, other, last, unread))
        }
        return out.sortedByDescending { it.last.at }
    }

    fun unreadChats(d: AppData, userId: String): Int = chats(d, userId).sumOf { it.unread }
    fun unreadNotices(d: AppData, userId: String): Int = d.notices.count { it.userId == userId && !it.read }

    fun clientJobs(d: AppData, userId: String): List<Job> =
        d.jobs.filter { it.clientId == userId }.sortedByDescending { it.createdAt }

    /** Jobs where the user is the hired seeker or has an application. */
    fun seekerJobs(d: AppData, userId: String): List<Job> =
        d.jobs.filter { j -> j.hiredSeekerId == userId || j.applications.any { it.seekerId == userId } }
            .sortedByDescending { max(it.lastEventAt, it.applications.firstOrNull { a -> a.seekerId == userId }?.createdAt ?: 0L) }

    fun myApplication(j: Job, userId: String): Application? =
        j.applications.firstOrNull { it.seekerId == userId }

    fun earnings(d: AppData, userId: String, now: Long): Earnings {
        val done = d.jobs.filter { it.hiredSeekerId == userId && it.status == JobStatus.COMPLETED }
        fun sum(from: Long) = done.filter { it.completedAt >= from }.sumOf { it.agreedPrice }
        return Earnings(sum(now - DAY), sum(now - 7 * DAY), sum(now - 30 * DAY), done.sumOf { it.agreedPrice }, done.size)
    }

    fun reviewsFor(d: AppData, userId: String, asSeeker: Boolean): List<Review> =
        d.reviews.filter { it.revieweeId == userId && it.revieweeIsSeeker == asSeeker }.sortedByDescending { it.createdAt }

    /** Simple client reputation: average of reviews that seekers left for them. */
    fun clientRating(d: AppData, userId: String): Pair<Double, Int> {
        val rs = reviewsFor(d, userId, false)
        return if (rs.isEmpty()) 0.0 to 0 else rs.map { it.rating }.average() to rs.size
    }

    fun clientJobCount(d: AppData, userId: String): Int = d.jobs.count { it.clientId == userId }
}
