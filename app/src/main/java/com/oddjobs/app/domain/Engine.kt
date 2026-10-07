package com.oddjobs.app.domain

import java.util.UUID
import kotlin.random.Random

data class ProfileInput(
    val name: String,
    val areaId: String,
    val isSeeker: Boolean,
    val categories: List<String>,
    val bio: String,
    val experienceYears: Int,
    val rateMin: Int,
    val rateMax: Int,
    val radiusKm: Int
)

data class JobDraft(
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val areaId: String = "dhanmondi",
    val landmark: String = "",
    val photos: List<String> = emptyList(),
    val urgency: Urgency = Urgency.TODAY,
    val scheduledAt: Long = 0L,
    val budget: Int = 500,
    val negotiable: Boolean = true,
    val payMethod: PayMethod = PayMethod.CASH,
    val onlyVerified: Boolean = false,
    val minRating: Double = 0.0,
    val invitedSeekerId: String? = null
)

/**
 * All business rules live here as pure functions (AppData in -> Out out).
 * The ViewModel only calls these; a real backend (Cloud Functions) would enforce the same rules.
 */
object Engine {
    fun newId(): String = UUID.randomUUID().toString()

    private fun AppData.putUser(u: User) = copy(users = users.map { if (it.id == u.id) u else it })
    private fun AppData.putJob(j: Job) = copy(jobs = jobs.map { if (it.id == j.id) j else it })

    fun notice(
        d: AppData, userId: String, tEn: String, tBn: String, bEn: String, bBn: String,
        now: Long, jobId: String? = null, chatId: String? = null
    ): AppData {
        val u = d.user(userId) ?: return d
        if (u.isDemo) return d
        val n = Notice(newId(), userId, tEn, tBn, bEn, bBn, now, false, jobId, chatId)
        return d.copy(notices = (listOf(n) + d.notices).take(300))
    }

    private fun err(d: AppData, key: String) = Out(d, key)

    // ---------------------------------------------------------------- auth

    fun requestOtp(d: AppData, phoneRaw: String, now: Long): Out {
        val p = Validators.normalizeBdPhone(phoneRaw) ?: return err(d, "phone_invalid")
        val code = Random.nextInt(100000, 1000000).toString()
        return Out(d.copy(pendingPhone = p, pendingOtp = code, otpSentAt = now), value = code)
    }

    fun verifyOtp(d: AppData, codeRaw: String, now: Long): Out {
        val phone = d.pendingPhone ?: return err(d, "otp_wrong")
        if (now - d.otpSentAt > 5 * 60_000L) return err(d, "otp_expired")
        if (Validators.asciiDigits(codeRaw) != d.pendingOtp) return err(d, "otp_wrong")
        val existing = d.users.firstOrNull { it.phone == phone && !it.isDemo }
        val user = existing ?: User(id = newId(), phone = phone, name = "", areaId = "dhanmondi", createdAt = now)
        val users = if (existing == null) d.users + user else d.users
        val mode = if (existing != null && existing.isSeeker && d.mode == Mode.SEEKER) Mode.SEEKER else Mode.CLIENT
        return Out(
            d.copy(users = users, currentUserId = user.id, pendingPhone = null, pendingOtp = null, mode = mode),
            value = if (existing == null) "new" else "existing"
        )
    }

    fun logout(d: AppData) = d.copy(currentUserId = null, mode = Mode.CLIENT)

    fun saveProfile(d: AppData, userId: String, p: ProfileInput): Out {
        val me = d.user(userId) ?: return err(d, "not_allowed")
        if (p.name.trim().length < 2) return err(d, "name_required")
        if (p.isSeeker && p.categories.isEmpty()) return err(d, "category_required")
        if (p.isSeeker && p.rateMin > p.rateMax) return err(d, "bad_rate")
        val first = !me.profileDone
        val u = me.copy(
            name = p.name.trim(), areaId = p.areaId, profileDone = true,
            isSeeker = me.isSeeker || p.isSeeker,
            categories = if (p.isSeeker) p.categories.take(5) else me.categories,
            bio = if (p.isSeeker) p.bio.trim().take(300) else me.bio,
            experienceYears = if (p.isSeeker) p.experienceYears.coerceIn(0, 60) else me.experienceYears,
            rateMin = if (p.isSeeker) p.rateMin else me.rateMin,
            rateMax = if (p.isSeeker) p.rateMax else me.rateMax,
            radiusKm = if (p.isSeeker) p.radiusKm.coerceIn(1, 50) else me.radiusKm
        )
        var out = d.putUser(u)
        if (p.isSeeker && !me.isSeeker) out = out.copy(mode = Mode.SEEKER)
        if (first && !p.isSeeker) out = out.copy(mode = Mode.CLIENT)
        return Out(out)
    }

    fun setPhoto(d: AppData, userId: String, path: String): AppData {
        val u = d.user(userId) ?: return d
        return d.putUser(u.copy(photoPath = path))
    }

    fun setAvailable(d: AppData, userId: String, on: Boolean): AppData {
        val u = d.user(userId) ?: return d
        return d.putUser(u.copy(availableNow = on))
    }

    fun switchMode(d: AppData, mode: Mode): AppData = d.copy(mode = mode)

    fun submitVerification(d: AppData, userId: String, photos: List<String>, now: Long): Out {
        val u = d.user(userId) ?: return err(d, "not_allowed")
        if (photos.size < 2) return err(d, "need_photos")
        return Out(
            d.putUser(
                u.copy(verifyStatus = VerifyStatus.SUBMITTED, verifySubmittedAt = now, idPhotos = photos)
            )
        )
    }

    fun deleteAccount(d: AppData, userId: String): AppData {
        val myJobIds = d.jobs.filter { it.clientId == userId }.map { it.id }.toSet()
        val jobs = d.jobs.filter { it.clientId != userId }.map { j ->
            j.copy(applications = j.applications.filter { it.seekerId != userId })
        }
        return d.copy(
            users = d.users.filter { it.id != userId },
            jobs = jobs,
            reviews = d.reviews.filter { it.reviewerId != userId && it.revieweeId != userId },
            messages = d.messages.filter { it.senderId != userId && jobIdOfChat(it.chatId) !in myJobIds },
            notices = d.notices.filter { it.userId != userId },
            currentUserId = null,
            mode = Mode.CLIENT
        )
    }

    // ---------------------------------------------------------------- jobs

    fun postJob(d: AppData, clientId: String, draft: JobDraft, now: Long): Out {
        val me = d.user(clientId) ?: return err(d, "not_allowed")
        if (!me.profileDone) return err(d, "not_allowed")
        if (Catalog.categories.none { it.id == draft.categoryId }) return err(d, "category_required")
        if (draft.budget < 50 || draft.budget > 1_000_000) return err(d, "bad_budget")
        if (draft.description.length > 1000) return err(d, "too_long")
        val cat = Catalog.category(draft.categoryId)
        val job = Job(
            id = newId(), clientId = clientId,
            title = draft.title.trim().ifBlank { cat.en },
            description = draft.description.trim(), categoryId = draft.categoryId,
            areaId = draft.areaId, landmark = draft.landmark.trim(), urgency = draft.urgency,
            budget = draft.budget, negotiable = draft.negotiable, payMethod = draft.payMethod,
            arrivalCode = Random.nextInt(1000, 10000).toString(), createdAt = now,
            photos = draft.photos.take(3), scheduledAt = draft.scheduledAt,
            onlyVerified = draft.onlyVerified, minRating = draft.minRating,
            invitedSeekerId = draft.invitedSeekerId,
            events = listOf(JobEvent(JobStatus.OPEN, clientId, now))
        )
        var out = d.copy(jobs = listOf(job) + d.jobs)
        val inv = draft.invitedSeekerId
        if (inv != null) {
            out = notice(
                out, inv, "You were invited to a job", "à¦†à¦ªà¦¨à¦¾à¦•à§‡ à¦à¦•à¦Ÿà¦¿ à¦•à¦¾à¦œà§‡ à¦†à¦®à¦¨à§à¦¤à§à¦°à¦£ à¦œà¦¾à¦¨à¦¾à¦¨à§‹ à¦¹à¦¯à¦¼à§‡à¦›à§‡",
                job.title, job.title, now, jobId = job.id
            )
        }
        return Out(out, value = job.id)
    }

    fun applyToJob(d: AppData, seekerId: String, jobId: String, price: Int, message: String, etaMin: Int, now: Long): Out {
        val me = d.user(seekerId) ?: return err(d, "not_allowed")
        val job = d.job(jobId) ?: return err(d, "not_found")
        if (!me.isSeeker || !me.profileDone) return err(d, "not_seeker")
        if (me.verifyStatus != VerifyStatus.APPROVED) return err(d, "not_verified")
        if (job.clientId == seekerId) return err(d, "own_job")
        if (job.status != JobStatus.OPEN) return err(d, "not_open")
        if (price < 50 || price > 1_000_000) return err(d, "bad_price")
        if (job.minRating > 0.0) {
            val s = Ranking.stats(seekerId, d.jobs, d.reviews, now)
            if (s.ratingCount > 0 && s.ratingAvg < job.minRating) return err(d, "rating_low")
        }
        val resp = ((now - job.createdAt) / 60_000L).toInt().coerceAtLeast(1)
        val app = Application(seekerId, price, message.trim().take(300), etaMin.coerceIn(5, 24 * 60), now, resp)
        val others = job.applications.filter { it.seekerId != seekerId }
        var out = d.putJob(job.copy(applications = others + app))
        val client = d.user(job.clientId)
        out = notice(
            out, job.clientId, "New offer: BDT $price", "à¦¨à¦¤à§à¦¨ à¦…à¦«à¦¾à¦°: à§³$price",
            "${me.name} applied to \"${job.title}\"", "${me.name} \"${job.title}\" à¦•à¦¾à¦œà§‡ à¦†à¦¬à§‡à¦¦à¦¨ à¦•à¦°à§‡à¦›à§‡à¦¨",
            now, jobId = job.id
        )
        if (client != null && message.isNotBlank()) {
            out = sendMessage(out, chatIdOf(jobId, seekerId), seekerId, message, now, silent = true).data
        }
        return Out(out)
    }

    fun withdraw(d: AppData, seekerId: String, jobId: String): Out {
        val job = d.job(jobId) ?: return err(d, "not_found")
        if (job.status != JobStatus.OPEN) return err(d, "not_open")
        val apps = job.applications.map {
            if (it.seekerId == seekerId && it.status == AppStatus.PENDING) it.copy(status = AppStatus.WITHDRAWN) else it
        }
        return Out(d.putJob(job.copy(applications = apps)))
    }

    fun hire(d: AppData, clientId: String, jobId: String, seekerId: String, now: Long): Out {
        val job = d.job(jobId) ?: return err(d, "not_found")
        if (job.clientId != clientId) return err(d, "not_allowed")
        if (!JobFlow.canTransition(job.status, JobStatus.HIRED, Actor.CLIENT)) return err(d, "bad_transition")
        val app = job.applications.firstOrNull { it.seekerId == seekerId && it.status == AppStatus.PENDING }
            ?: return err(d, "no_application")
        val apps = job.applications.map {
            when {
                it.seekerId == seekerId -> it.copy(status = AppStatus.ACCEPTED)
                it.status == AppStatus.PENDING -> it.copy(status = AppStatus.REJECTED)
                else -> it
            }
        }
        val updated = job.copy(
            status = JobStatus.HIRED, hiredSeekerId = seekerId, agreedPrice = app.price,
            applications = apps, events = job.events + JobEvent(JobStatus.HIRED, clientId, now)
        )
        var out = d.putJob(updated)
        out = notice(
            out, seekerId, "You got the job!", "à¦†à¦ªà¦¨à¦¿ à¦•à¦¾à¦œà¦Ÿà¦¿ à¦ªà§‡à¦¯à¦¼à§‡à¦›à§‡à¦¨!",
            "\"${job.title}\" â€” open it to see the address and arrival code flow.",
            "\"${job.title}\" â€” à¦ à¦¿à¦•à¦¾à¦¨à¦¾ à¦¦à§‡à¦–à¦¤à§‡ à¦•à¦¾à¦œà¦Ÿà¦¿ à¦–à§à¦²à§à¦¨à¥¤", now, jobId = jobId
        )
        for (o in job.applications) {
            if (o.seekerId != seekerId && o.status == AppStatus.PENDING) {
                out = notice(
                    out, o.seekerId, "Job filled", "à¦•à¦¾à¦œà¦Ÿà¦¿ à¦…à¦¨à§à¦¯à¦œà¦¨ à¦ªà§‡à¦¯à¦¼à§‡à¦›à§‡à¦¨",
                    "\"${job.title}\" was given to another worker.", "\"${job.title}\" à¦…à¦¨à§à¦¯ à¦à¦•à¦œà¦¨ à¦•à¦°à§à¦®à§€ à¦ªà§‡à¦¯à¦¼à§‡à¦›à§‡à¦¨à¥¤",
                    now, jobId = jobId
                )
            }
        }
        return Out(out)
    }

    private fun actorOf(job: Job, userId: String): Actor? = when {
        userId == job.clientId -> Actor.CLIENT
        userId == job.hiredSeekerId -> Actor.SEEKER
        else -> null
    }

    fun advance(d: AppData, userId: String, jobId: String, to: JobStatus, now: Long, code: String? = null): Out {
        val job = d.job(jobId) ?: return err(d, "not_found")
        val actor = actorOf(job, userId) ?: return err(d, "not_allowed")
        if (!JobFlow.canTransition(job.status, to, actor)) return err(d, "bad_transition")
        if (to == JobStatus.ARRIVED && Validators.asciiDigits(code ?: "") != job.arrivalCode) return err(d, "code_wrong")
        var j = job.copy(status = to, events = job.events + JobEvent(to, userId, now))
        if (to == JobStatus.ARRIVED && job.scheduledAt > 0 && now > job.scheduledAt + 15 * 60_000L) {
            j = j.copy(onTime = false)
        }
        var out = d.putJob(j)
        val (tEn, tBn) = when (to) {
            JobStatus.ON_THE_WAY -> "Worker is on the way" to "à¦•à¦°à§à¦®à§€ à¦°à¦“à¦¨à¦¾ à¦¦à¦¿à¦¯à¦¼à§‡à¦›à§‡à¦¨"
            JobStatus.ARRIVED -> "Worker has arrived" to "à¦•à¦°à§à¦®à§€ à¦ªà§Œà¦à¦›à§‡à¦›à§‡à¦¨"
            JobStatus.IN_PROGRESS -> "Work started" to "à¦•à¦¾à¦œ à¦¶à§à¦°à§ à¦¹à¦¯à¦¼à§‡à¦›à§‡"
            JobStatus.AWAITING_CONFIRMATION -> "Work finished â€” please confirm" to "à¦•à¦¾à¦œ à¦¶à§‡à¦· â€” à¦¨à¦¿à¦¶à§à¦šà¦¿à¦¤ à¦•à¦°à§à¦¨"
            else -> "Job updated" to "à¦•à¦¾à¦œ à¦†à¦ªà¦¡à§‡à¦Ÿ à¦¹à¦¯à¦¼à§‡à¦›à§‡"
        }
        val other = if (actor == Actor.CLIENT) job.hiredSeekerId else job.clientId
        if (other != null) out = notice(out, other, tEn, tBn, job.title, job.title, now, jobId = jobId)
        return Out(out)
    }

    fun confirmCompletion(d: AppData, clientId: String, jobId: String, finalPrice: Int, pay: PayMethod, now: Long): Out {
        val job = d.job(jobId) ?: return err(d, "not_found")
        if (job.clientId != clientId) return err(d, "not_allowed")
        if (!JobFlow.canTransition(job.status, JobStatus.COMPLETED, Actor.CLIENT)) return err(d, "bad_transition")
        if (finalPrice < 0 || finalPrice > 1_000_000) return err(d, "bad_price")
        val j = job.copy(
            status = JobStatus.COMPLETED, agreedPrice = finalPrice, payMethod = pay, paidConfirmed = true,
            completedAt = now, events = job.events + JobEvent(JobStatus.COMPLETED, clientId, now)
        )
        var out = d.putJob(j)
        val sid = job.hiredSeekerId
        if (sid != null) {
            out = notice(
                out, sid, "Job completed", "à¦•à¦¾à¦œ à¦¸à¦®à§à¦ªà¦¨à§à¦¨ à¦¹à¦¯à¦¼à§‡à¦›à§‡",
                "${job.title} â€” payment of BDT $finalPrice recorded. Rate your client.",
                "${job.title} â€” à§³$finalPrice à¦ªà§‡à¦®à§‡à¦¨à§à¦Ÿ à¦°à§‡à¦•à¦°à§à¦¡ à¦¹à¦¯à¦¼à§‡à¦›à§‡à¥¤ à¦•à§à¦²à¦¾à¦¯à¦¼à§‡à¦¨à§à¦Ÿà¦•à§‡ à¦°à§‡à¦Ÿà¦¿à¦‚ à¦¦à¦¿à¦¨à¥¤", now, jobId = jobId
            )
        }
        return Out(out)
    }

    fun cancel(d: AppData, userId: String, jobId: String, reason: String, now: Long): Out {
        val job = d.job(jobId) ?: return err(d, "not_found")
        val actor = actorOf(job, userId) ?: return err(d, "not_allowed")
        if (!JobFlow.canTransition(job.status, JobStatus.CANCELLED, actor)) return err(d, "bad_transition")
        val j = job.copy(
            status = JobStatus.CANCELLED, cancelledBy = userId, cancelReason = reason.trim().take(200),
            events = job.events + JobEvent(JobStatus.CANCELLED, userId, now)
        )
        var out = d.putJob(j)
        val other = if (actor == Actor.CLIENT) job.hiredSeekerId else job.clientId
        if (other != null) {
            out = notice(out, other, "Job cancelled", "à¦•à¦¾à¦œ à¦¬à¦¾à¦¤à¦¿à¦² à¦¹à¦¯à¦¼à§‡à¦›à§‡", job.title, job.title, now, jobId = jobId)
        }
        return Out(out)
    }

    fun expire(d: AppData, jobId: String, now: Long): AppData {
        val job = d.job(jobId) ?: return d
        if (!JobFlow.canTransition(job.status, JobStatus.EXPIRED, Actor.SYSTEM)) return d
        return d.putJob(job.copy(status = JobStatus.EXPIRED, events = job.events + JobEvent(JobStatus.EXPIRED, "system", now)))
    }

    fun toggleSaved(d: AppData, userId: String, jobId: String): AppData {
        val u = d.user(userId) ?: return d
        val s = if (jobId in u.savedJobIds) u.savedJobIds - jobId else u.savedJobIds + jobId
        return d.putUser(u.copy(savedJobIds = s))
    }

    fun hideJob(d: AppData, userId: String, jobId: String): AppData {
        val u = d.user(userId) ?: return d
        return d.putUser(u.copy(hiddenJobIds = u.hiddenJobIds + jobId))
    }

    fun toggleFavorite(d: AppData, userId: String, seekerId: String): AppData {
        val u = d.user(userId) ?: return d
        val s = if (seekerId in u.favoriteSeekerIds) u.favoriteSeekerIds - seekerId else u.favoriteSeekerIds + seekerId
        return d.putUser(u.copy(favoriteSeekerIds = s))
    }

    fun block(d: AppData, userId: String, otherId: String): AppData {
        val u = d.user(userId) ?: return d
        return d.putUser(u.copy(blockedIds = u.blockedIds + otherId))
    }

    fun unblock(d: AppData, userId: String, otherId: String): AppData {
        val u = d.user(userId) ?: return d
        return d.putUser(u.copy(blockedIds = u.blockedIds - otherId))
    }

    fun report(d: AppData, reporterId: String, targetType: String, targetId: String, reason: String, now: Long): AppData =
        d.copy(reports = d.reports + Report(newId(), reporterId, targetType, targetId, reason.take(300), now))

    // ---------------------------------------------------------------- reviews

    fun submitReview(
        d: AppData, reviewerId: String, jobId: String, rating: Int, tags: List<String>, comment: String, now: Long
    ): Out {
        val job = d.job(jobId) ?: return err(d, "not_found")
        if (job.status != JobStatus.COMPLETED) return err(d, "not_completed")
        if (rating !in 1..5) return err(d, "bad_rating")
        val seekerId = job.hiredSeekerId ?: return err(d, "not_allowed")
        val byClient = reviewerId == job.clientId
        val bySeeker = reviewerId == seekerId
        if (!byClient && !bySeeker) return err(d, "not_allowed")
        if (byClient && job.clientReviewed) return err(d, "already_reviewed")
        if (bySeeker && job.seekerReviewed) return err(d, "already_reviewed")
        val review = Review(
            id = newId(), jobId = jobId, reviewerId = reviewerId,
            revieweeId = if (byClient) seekerId else job.clientId, revieweeIsSeeker = byClient,
            rating = rating, tags = tags.filter { it in Ranking.positiveTags || it in Ranking.negativeTags },
            comment = comment.trim().take(500), createdAt = now
        )
        val j = if (byClient) job.copy(clientReviewed = true) else job.copy(seekerReviewed = true)
        var out = d.putJob(j).copy(reviews = d.reviews + review)
        out = notice(
            out, review.revieweeId, "You received a review", "à¦†à¦ªà¦¨à¦¿ à¦à¦•à¦Ÿà¦¿ à¦°à¦¿à¦­à¦¿à¦‰ à¦ªà§‡à¦¯à¦¼à§‡à¦›à§‡à¦¨",
            "$rating â˜… â€” ${job.title}", "$rating â˜… â€” ${job.title}", now, jobId = jobId
        )
        return Out(out)
    }

    // ---------------------------------------------------------------- chat

    fun sendMessage(d: AppData, chatId: String, senderId: String, text: String, now: Long, silent: Boolean = false): Out {
        val t = text.trim()
        if (t.isEmpty()) return err(d, "empty")
        if (t.length > 1000) return err(d, "too_long")
        val job = d.job(jobIdOfChat(chatId)) ?: return err(d, "not_found")
        val seekerId = seekerIdOfChat(chatId)
        if (d.user(seekerId) == null) return err(d, "not_found")
        val fromClient = senderId == job.clientId
        val fromSeeker = senderId == seekerId
        if (!fromClient && !fromSeeker) return err(d, "not_allowed")
        val related = job.applications.any { it.seekerId == seekerId } ||
            job.hiredSeekerId == seekerId || job.invitedSeekerId == seekerId
        if (fromClient && !related) return err(d, "not_allowed")
        if (fromSeeker && !related && job.status != JobStatus.OPEN) return err(d, "not_allowed")
        val other = if (fromClient) seekerId else job.clientId
        if (d.user(other)?.blockedIds?.contains(senderId) == true || d.user(senderId)?.blockedIds?.contains(other) == true) {
            return err(d, "blocked")
        }
        val m = ChatMessage(newId(), chatId, senderId, t, now)
        var out = d.copy(messages = d.messages + m)
        if (!silent) {
            val name = d.user(senderId)?.name ?: ""
            out = notice(out, other, "New message from $name", "$name à¦à¦° à¦¨à¦¤à§à¦¨ à¦¬à¦¾à¦°à§à¦¤à¦¾", t.take(80), t.take(80), now, jobId = job.id, chatId = chatId)
        }
        return Out(out)
    }

    fun markChatRead(d: AppData, userId: String, chatId: String, now: Long): AppData =
        d.copy(chatRead = d.chatRead + ("$userId|$chatId" to now))

    fun markNoticesRead(d: AppData, userId: String): AppData =
        d.copy(notices = d.notices.map { if (it.userId == userId && !it.read) it.copy(read = true) else it })

    fun setLang(d: AppData, lang: String) = d.copy(lang = lang, langChosen = true)
    fun setDemo(d: AppData, on: Boolean) = d.copy(demoSim = on)
}

