package com.oddjobs.app

import com.oddjobs.app.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    private val t0 = 1_800_000_000_000L

    private fun freshData(): AppData {
        val s = Seed.initial(t0)
        return AppData(users = s.users, jobs = s.jobs, reviews = s.reviews)
    }

    private fun signUp(d0: AppData, phone: String, seeker: Boolean, cats: List<String> = emptyList(), now: Long): AppData {
        var d = Engine.requestOtp(d0, phone, now).data
        d = Engine.verifyOtp(d, d.pendingOtp!!, now).data
        val r = Engine.saveProfile(
            d, d.currentUserId!!,
            ProfileInput("Kabir Test", "dhanmondi", seeker, cats, "bio", 3, 300, 900, 12)
        )
        assertTrue(r.error, r.ok)
        return r.data
    }

    private fun run(d0: AppData, from: Long, seconds: Int, stepSec: Int = 2): Pair<AppData, Long> {
        var d = d0
        var now = from
        var elapsed = 0
        while (elapsed < seconds) {
            now += stepSec * 1000L
            d = Simulation.step(d, now)
            elapsed += stepSec
        }
        return d to now
    }

    @Test
    fun phoneValidation() {
        assertEquals("+8801712345678", Validators.normalizeBdPhone("01712345678"))
        assertEquals("+8801712345678", Validators.normalizeBdPhone("+8801712345678"))
        assertEquals("+8801912345678", Validators.normalizeBdPhone("০১৯১২৩৪৫৬৭৮"))
        assertNull(Validators.normalizeBdPhone("01212345678"))
        assertNull(Validators.normalizeBdPhone("1234"))
    }

    @Test
    fun otpFlow() {
        var d = AppData(users = emptyList())
        val bad = Engine.requestOtp(d, "123", t0)
        assertEquals("phone_invalid", bad.error)
        d = Engine.requestOtp(d, "01712345678", t0).data
        assertEquals("otp_wrong", Engine.verifyOtp(d, "000000".takeIf { it != d.pendingOtp } ?: "111111", t0).error)
        assertEquals("otp_expired", Engine.verifyOtp(d, d.pendingOtp!!, t0 + 6 * 60_000L).error)
        val ok = Engine.verifyOtp(d, d.pendingOtp!!, t0 + 1000L)
        assertTrue(ok.ok)
        assertNotNull(ok.data.me)
    }

    @Test
    fun stateMachineRules() {
        assertTrue(JobFlow.canTransition(JobStatus.OPEN, JobStatus.HIRED, Actor.CLIENT))
        assertFalse(JobFlow.canTransition(JobStatus.OPEN, JobStatus.HIRED, Actor.SEEKER))
        assertFalse(JobFlow.canTransition(JobStatus.OPEN, JobStatus.COMPLETED, Actor.CLIENT))
        assertTrue(JobFlow.canTransition(JobStatus.HIRED, JobStatus.ON_THE_WAY, Actor.SEEKER))
        assertFalse(JobFlow.canTransition(JobStatus.IN_PROGRESS, JobStatus.CANCELLED, Actor.CLIENT))
        assertFalse(JobFlow.canTransition(JobStatus.COMPLETED, JobStatus.CANCELLED, Actor.CLIENT))
    }

    @Test
    fun sentiment() {
        assertTrue(Sentiment.score("Very good and polite worker, excellent") > 0.4)
        assertTrue(Sentiment.score("Came late, rude and poor work") < -0.4)
        assertTrue(Sentiment.score("খুব ভালো কাজ, দ্রুত শেষ করেছেন") > 0.3)
        assertTrue(Sentiment.score("খারাপ ব্যবহার ও দেরি") < -0.3)
        assertEquals(0.0, Sentiment.score(""), 0.0001)
    }

    @Test
    fun rankingPrefersProvenSeekers() {
        val d = freshData()
        val top = Queries.rankedOne(d, "demo_s1", t0)!!   // 4.9 rating, 22 jobs
        val newbie = Queries.rankedOne(d, "demo_s15", t0)!! // brand new
        val low = Queries.rankedOne(d, "demo_s10", t0)!!    // 4.2 rating, one cancellation
        assertTrue(top.trust > newbie.trust)
        assertTrue(top.trust > low.trust)
        assertTrue(top.trust in 0.0..100.0)
        assertTrue(Ranking.isNew(newbie.stats))
    }

    @Test
    fun bayesianShrinksSmallSamples() {
        assertTrue(Ranking.bayesianRating(5.0, 1) < Ranking.bayesianRating(5.0, 30))
        assertTrue(Ranking.bayesianRating(5.0, 1) < 4.7)
        assertEquals(Ranking.GLOBAL_AVG, Ranking.bayesianRating(0.0, 0), 0.0001)
    }

    @Test
    fun responseScoreDecays() {
        assertEquals(1.0, Ranking.responseScore(3), 0.0001)
        assertTrue(Ranking.responseScore(30) > Ranking.responseScore(120))
        assertEquals(0.0, Ranking.responseScore(400), 0.0001)
    }

    @Test
    fun rankedListForPlumberJobInDhanmondi() {
        val d = freshData()
        val list = Queries.ranked(d, "plumber", "dhanmondi", null, t0, budget = 500)
        assertTrue(list.isNotEmpty())
        assertEquals("demo_s1", list.first().user.id) // closest, highest rated plumber
        assertTrue(list.all { "plumber" in it.user.categories })
    }

    @Test
    fun clientPostsJobAndWholeLifecycleWorks() {
        var now = t0
        var d = signUp(freshData(), "01711111111", false, now = now)
        val me = d.currentUserId!!
        val post = Engine.postJob(
            d, me,
            JobDraft(title = "Fix tap", categoryId = "plumber", areaId = "dhanmondi", budget = 500, landmark = "Road 5"),
            now
        )
        assertTrue(post.error, post.ok)
        d = post.data
        val jobId = post.value!!

        val (d1, n1) = run(d, now, 40)
        d = d1; now = n1
        var job = d.job(jobId)!!
        assertTrue("sim seekers should apply", job.applications.isNotEmpty())
        assertTrue(job.applications.size <= 4)

        // pick the best-ranked applicant
        val best = job.applications.maxByOrNull { Queries.rankedOne(d, it.seekerId, now)!!.trust }!!
        val hire = Engine.hire(d, me, jobId, best.seekerId, now)
        assertTrue(hire.error, hire.ok)
        d = hire.data
        assertEquals(JobStatus.HIRED, d.job(jobId)!!.status)
        assertEquals(best.price, d.job(jobId)!!.agreedPrice)

        // the client cannot skip steps
        assertEquals("bad_transition", Engine.advance(d, me, jobId, JobStatus.ON_THE_WAY, now).error)

        val (d2, n2) = run(d, now, 60)
        d = d2; now = n2
        job = d.job(jobId)!!
        assertEquals(JobStatus.AWAITING_CONFIRMATION, job.status)

        val done = Engine.confirmCompletion(d, me, jobId, 550, PayMethod.BKASH, now)
        assertTrue(done.error, done.ok)
        d = done.data
        assertEquals(JobStatus.COMPLETED, d.job(jobId)!!.status)

        val before = Queries.rankedOne(d, best.seekerId, now)!!
        val rev = Engine.submitReview(d, me, jobId, 5, listOf("punctual", "polite"), "Very good work, thanks", now)
        assertTrue(rev.error, rev.ok)
        d = rev.data
        assertEquals("already_reviewed", Engine.submitReview(d, me, jobId, 5, emptyList(), "", now).error)
        val after = Queries.rankedOne(d, best.seekerId, now)!!
        assertEquals(before.stats.ratingCount + 1, after.stats.ratingCount)
        assertEquals(before.stats.completed, after.stats.completed)
    }

    @Test
    fun seekerMustBeVerifiedToApplyThenDemoApprovesAndJobIsWon() {
        var now = t0
        var d = signUp(freshData(), "01822222222", true, listOf("electrician"), now)
        val me = d.currentUserId!!
        val open = d.jobs.first { it.status == JobStatus.OPEN && it.categoryId == "electrician" }

        val blocked = Engine.applyToJob(d, me, open.id, 600, "I can come", 30, now)
        assertEquals("not_verified", blocked.error)

        val sub = Engine.submitVerification(d, me, listOf("nid.jpg", "selfie.jpg"), now)
        assertTrue(sub.ok)
        d = sub.data
        val (d1, n1) = run(d, now, 12)
        d = d1; now = n1
        assertEquals(VerifyStatus.APPROVED, d.me!!.verifyStatus)

        // fresh open job from a demo client
        val target = d.jobs.first { it.status == JobStatus.OPEN && it.categoryId == "electrician" && d.user(it.clientId)!!.isDemo }
        val ap = Engine.applyToJob(d, me, target.id, target.budget, "On my way", 20, now)
        assertTrue(ap.error, ap.ok)
        d = ap.data

        val (d2, n2) = run(d, now, 20)
        d = d2; now = n2
        var job = d.job(target.id)!!
        assertEquals(me, job.hiredSeekerId)

        // seeker must give the correct arrival code
        var (d3, n3) = run(d, now, 14)
        d = d3; now = n3
        job = d.job(target.id)!!
        assertEquals(JobStatus.HIRED, job.status)
        val go = Engine.advance(d, me, target.id, JobStatus.ON_THE_WAY, now)
        assertTrue(go.error, go.ok)
        d = go.data
        val wrong = Engine.advance(d, me, target.id, JobStatus.ARRIVED, now, "0001".takeIf { it != job.arrivalCode } ?: "0002")
        assertEquals("code_wrong", wrong.error)
        val arrived = Engine.advance(d, me, target.id, JobStatus.ARRIVED, now, job.arrivalCode)
        assertTrue(arrived.error, arrived.ok)
        d = arrived.data
        d = Engine.advance(d, me, target.id, JobStatus.IN_PROGRESS, now).data
        d = Engine.advance(d, me, target.id, JobStatus.AWAITING_CONFIRMATION, now).data
        val (d4, n4) = run(d, now, 30)
        d = d4; now = n4
        assertEquals(JobStatus.COMPLETED, d.job(target.id)!!.status)
        assertTrue(d.reviews.any { it.revieweeId == me && it.revieweeIsSeeker })
        assertTrue(Queries.earnings(d, me, now).total > 0)
    }

    @Test
    fun feedFiltersAndSorting() {
        val d = freshData()
        val me = User("me", "+8801700000000", "Me", "dhanmondi", t0, profileDone = true, isSeeker = true,
            categories = listOf("plumber"), verifyStatus = VerifyStatus.APPROVED)
        val all = Queries.feed(d, me, JobFilter(), t0)
        assertTrue(all.isNotEmpty())
        val plumbing = Queries.feed(d, me, JobFilter(categories = setOf("plumber")), t0)
        assertTrue(plumbing.all { it.job.categoryId == "plumber" })
        val ctg = Queries.feed(d, me, JobFilter(district = "Chattogram"), t0)
        assertTrue(ctg.all { Catalog.area(it.job.areaId).district == "Chattogram" })
        val near = Queries.feed(d, me, JobFilter(maxKm = 5, sort = SortBy.NEAREST), t0)
        assertTrue(near.all { it.distanceKm <= 5.0 })
        assertEquals(near.map { it.distanceKm }, near.map { it.distanceKm }.sorted())
        val rich = Queries.feed(d, me, JobFilter(minBudget = 3000, sort = SortBy.BUDGET), t0)
        assertTrue(rich.all { it.job.budget >= 3000 })
        val search = Queries.feed(d, me, JobFilter(query = "প্লাম্বার"), t0)
        assertTrue(search.all { it.job.categoryId == "plumber" })
    }

    @Test
    fun chatRulesAndDemoReply() {
        var now = t0
        var d = signUp(freshData(), "01733333333", false, now = now)
        val me = d.currentUserId!!
        val job = Engine.postJob(d, me, JobDraft(categoryId = "electrician", budget = 700, landmark = "x"), now)
        d = job.data
        val (d1, n1) = run(d, now, 40)
        d = d1; now = n1
        val j = d.job(job.value!!)!!
        val sid = j.applications.first().seekerId
        val chat = chatIdOf(j.id, sid)
        assertEquals("not_allowed", Engine.sendMessage(d, chatIdOf(j.id, "demo_s9"), me, "hi", now).error)
        val sent = Engine.sendMessage(d, chat, me, "What is the price?", now)
        assertTrue(sent.error, sent.ok)
        d = sent.data
        val (d2, _) = run(d, now, 10)
        val msgs = d2.messages.filter { it.chatId == chat }
        assertTrue(msgs.last().senderId == sid)
        assertEquals("empty", Engine.sendMessage(d2, chat, me, "   ", now).error)
    }

    @Test
    fun cancelAndDeleteAccount() {
        var now = t0
        var d = signUp(freshData(), "01744444444", false, now = now)
        val me = d.currentUserId!!
        val posted = Engine.postJob(d, me, JobDraft(categoryId = "cleaning", budget = 900), now)
        d = posted.data
        val c = Engine.cancel(d, me, posted.value!!, "Changed my mind", now)
        assertTrue(c.ok)
        assertEquals(JobStatus.CANCELLED, c.data.job(posted.value!!)!!.status)
        val gone = Engine.deleteAccount(c.data, me)
        assertNull(gone.user(me))
        assertTrue(gone.jobs.none { it.clientId == me })
        assertNull(gone.currentUserId)
    }

    @Test
    fun postJobValidation() {
        val d = signUp(freshData(), "01755555555", false, now = t0)
        val me = d.currentUserId!!
        assertEquals("category_required", Engine.postJob(d, me, JobDraft(categoryId = ""), t0).error)
        assertEquals("bad_budget", Engine.postJob(d, me, JobDraft(categoryId = "plumber", budget = 10), t0).error)
        assertTrue(Engine.postJob(d, me, JobDraft(categoryId = "plumber", budget = 300), t0).ok)
    }
}
