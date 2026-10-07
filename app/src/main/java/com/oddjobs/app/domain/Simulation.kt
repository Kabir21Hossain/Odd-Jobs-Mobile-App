package com.oddjobs.app.domain

import kotlin.random.Random

/**
 * Demo-mode counterpart. Moves simulated workers and clients forward in time so a single
 * person can try the full two-sided flow on one phone. Pure and idempotent: called every
 * couple of seconds with the current clock. Turn it off in Settings (a backend replaces it).
 */
object Simulation {
    private const val DAY = 86_400_000L

    private fun h(vararg parts: String): Int = parts.joinToString("|").hashCode() and 0x7fffffff

    fun step(input: AppData, now: Long): AppData {
        if (!input.demoSim) return input
        var d = input

        // Keep the marketplace stocked with fresh open jobs from demo clients.
        val openDemo = d.jobs.count { it.status == JobStatus.OPEN && d.user(it.clientId)?.isDemo == true }
        if (openDemo < 8) {
            val fresh = Seed.openFromTemplates(now, 8 - openDemo, (now / 60_000L).toInt(), Random(now), d.users)
            d = d.copy(jobs = fresh + d.jobs)
        }

        // Expire stale jobs.
        for (j in d.jobs) {
            if (j.status == JobStatus.OPEN && now - j.createdAt > 7 * DAY) d = Engine.expire(d, j.id, now)
        }

        // Identity verification review (stands in for the admin team).
        for (u in d.users) {
            if (!u.isDemo && u.verifyStatus == VerifyStatus.SUBMITTED && now - u.verifySubmittedAt >= 8_000L) {
                val approved = d.users.map {
                    if (it.id == u.id) it.copy(verifyStatus = VerifyStatus.APPROVED) else it
                }
                d = d.copy(users = approved)
                d = Engine.notice(
                    d, u.id, "Identity verified", "পরিচয় যাচাই সম্পন্ন",
                    "You can now apply to jobs and appear in worker lists.",
                    "এখন আপনি কাজে আবেদন করতে পারবেন এবং কর্মী তালিকায় দেখা যাবেন।", now
                )
            }
        }

        for (j in d.jobs.filter { it.status == JobStatus.OPEN || it.isActive || it.status == JobStatus.COMPLETED }) {
            d = stepJob(d, j.id, now)
        }
        d = stepChats(d, now)
        return d
    }

    private fun stepJob(input: AppData, jobId: String, now: Long): AppData {
        var d = input
        val job = d.job(jobId) ?: return d
        val client = d.user(job.clientId)
        val seeker = d.user(job.hiredSeekerId)
        val clientHuman = client != null && !client.isDemo
        val clientDemo = client != null && client.isDemo

        when (job.status) {
            JobStatus.OPEN -> {
                if (clientHuman) {
                    val demoApps = job.applications.count { d.user(it.seekerId)?.isDemo == true }
                    if (demoApps < 4) {
                        val candidates = d.users.filter { s ->
                            s.isDemo && s.isSeeker && s.verifyStatus == VerifyStatus.APPROVED && s.availableNow &&
                                job.categoryId in s.categories &&
                                job.applications.none { it.seekerId == s.id } &&
                                Geo.between(s.areaId, job.areaId) <= s.radiusKm &&
                                (!job.onlyVerified || s.level >= 2) &&
                                (job.minRating <= 0.0 || Ranking.stats(s.id, d.jobs, d.reviews, now).ratingAvg >= job.minRating)
                        }
                        for (s in candidates) {
                            val invited = job.invitedSeekerId == s.id
                            val delay = if (invited) 3_000L else 5_000L + (h(job.id, s.id) % 22_000)
                            if (now - job.createdAt >= delay && demoApps + 1 <= 4) {
                                val price = ((job.budget * (0.85 + (h(s.id, job.id) % 35) / 100.0)) / 10).toInt() * 10
                                val eta = 15 + h(job.id, s.id, "eta") % 60
                                val msg = if (d.lang == "bn") "আমি আজই কাজটি করে দিতে পারব।" else "I can do this work. I'm nearby and available."
                                val r = Engine.applyToJob(d, s.id, job.id, price.coerceAtLeast(50), msg, eta, now)
                                if (r.ok) d = r.data
                                break // one new applicant per tick
                            }
                        }
                    }
                } else if (clientDemo) {
                    val humanApp = job.applications.firstOrNull {
                        it.status == AppStatus.PENDING && d.user(it.seekerId)?.isDemo == false
                    }
                    if (humanApp != null && now - humanApp.createdAt >= 6_000L + (h(job.id, humanApp.seekerId) % 8_000)) {
                        val r = Engine.hire(d, job.clientId, job.id, humanApp.seekerId, now)
                        if (r.ok) {
                            d = r.data
                            val greet = if (d.lang == "bn") "ধন্যবাদ! সময়মতো চলে আসবেন। ঠিকানা: ${job.landmark}"
                            else "Thank you! Please come on time. Address landmark: ${job.landmark}"
                            d = Engine.sendMessage(d, chatIdOf(job.id, humanApp.seekerId), job.clientId, greet, now).data
                        }
                    }
                }
            }

            JobStatus.HIRED, JobStatus.ON_THE_WAY, JobStatus.ARRIVED, JobStatus.IN_PROGRESS -> {
                if (clientHuman && seeker != null && seeker.isDemo) {
                    val next = JobFlow.nextForSeeker(job.status)
                    if (next != null && now - job.lastEventAt >= 7_000L + (h(job.id, job.status.name) % 5_000)) {
                        val r = Engine.advance(d, seeker.id, job.id, next, now, job.arrivalCode)
                        if (r.ok) d = r.data
                    }
                }
            }

            JobStatus.AWAITING_CONFIRMATION -> {
                if (clientDemo && seeker != null && !seeker.isDemo && now - job.lastEventAt >= 8_000L) {
                    val r = Engine.confirmCompletion(d, job.clientId, job.id, job.agreedPrice, job.payMethod, now)
                    if (r.ok) d = r.data
                }
            }

            JobStatus.COMPLETED -> {
                if (now - job.completedAt >= 7_000L && seeker != null && client != null) {
                    if (clientHuman && seeker.isDemo && !job.seekerReviewed) {
                        val rating = if (h(job.id, "cr") % 5 == 0) 4 else 5
                        val c = if (d.lang == "bn") "ভালো ক্লায়েন্ট, সময়মতো টাকা দিয়েছেন।" else "Polite client and quick payment. Thanks!"
                        val r = Engine.submitReview(d, seeker.id, job.id, rating, listOf("polite", "communication"), c, now)
                        if (r.ok) d = r.data
                    }
                    if (clientDemo && !seeker.isDemo && !job.clientReviewed) {
                        val rating = listOf(5, 5, 5, 4, 4, 3)[h(job.id, "sr") % 6]
                        val r0 = Random(h(job.id, "rv").toLong())
                        val r = Engine.submitReview(
                            d, client.id, job.id, rating, Seed.tagsFor(rating, r0), Seed.commentFor(rating, r0), now
                        )
                        if (r.ok) d = r.data
                    }
                }
            }

            else -> {}
        }
        return d
    }

    private fun hasBangla(s: String) = s.any { it in '\u0980'..'\u09FF' }

    private fun stepChats(input: AppData, now: Long): AppData {
        var d = input
        val byChat = d.messages.groupBy { it.chatId }
        for ((chatId, msgs) in byChat) {
            val last = msgs.maxByOrNull { it.at } ?: continue
            val sender = d.user(last.senderId) ?: continue
            if (sender.isDemo) continue
            val job = d.job(jobIdOfChat(chatId)) ?: continue
            val seekerId = seekerIdOfChat(chatId)
            val replierId = if (last.senderId == job.clientId) seekerId else job.clientId
            val replier = d.user(replierId) ?: continue
            if (!replier.isDemo) continue
            if (now - last.at < 2_500L + (h(last.id) % 2_000)) continue
            val bn = hasBangla(last.text) || d.lang == "bn"
            val t = last.text.lowercase()
            val reply = if (replierId == seekerId) {
                val app = job.applications.firstOrNull { it.seekerId == seekerId }
                when {
                    listOf("price", "cost", "taka", "দাম", "কত", "টাকা", "rate").any { t.contains(it) } ->
                        if (bn) "কাজ দেখে ঠিক করব, আপাতত ৳${app?.price ?: job.budget} ধরুন।" else "Around BDT ${app?.price ?: job.budget}. I can adjust after seeing the work."
                    listOf("when", "time", "কখন", "সময়", "kokhon").any { t.contains(it) } ->
                        if (bn) "আমি ${app?.etaMin ?: 30} মিনিটের মধ্যে পৌঁছাতে পারব।" else "I can reach you within ${app?.etaMin ?: 30} minutes."
                    listOf("address", "where", "location", "ঠিকানা", "কোথায়").any { t.contains(it) } ->
                        if (bn) "দয়া করে ঠিকানা ও ল্যান্ডমার্ক পাঠান।" else "Please share the exact address and a landmark."
                    else -> if (bn) "ঠিক আছে, বুঝেছি। সময়মতো থাকব।" else "Okay, noted. I will be on time."
                }
            } else {
                when {
                    listOf("where", "address", "ঠিকানা", "কোথায়").any { t.contains(it) } ->
                        if (bn) "ল্যান্ডমার্ক: ${job.landmark}। এসে ফোন দেবেন।" else "Landmark: ${job.landmark}. Call me when you arrive."
                    listOf("arrive", "come", "way", "রওনা", "আসছি", "পৌঁছ").any { t.contains(it) } ->
                        if (bn) "ঠিক আছে, অপেক্ষা করছি।" else "Okay, I'm waiting."
                    else -> if (bn) "ধন্যবাদ, ঠিক আছে।" else "Thanks, sounds good."
                }
            }
            val r = Engine.sendMessage(d, chatId, replierId, reply, now, silent = false)
            if (r.ok) d = r.data
        }
        return d
    }
}
