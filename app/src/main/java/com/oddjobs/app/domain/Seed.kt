package com.oddjobs.app.domain

import kotlin.math.roundToInt
import kotlin.random.Random

data class SeedData(val users: List<User>, val jobs: List<Job>, val reviews: List<Review>)

/** Demo marketplace data so the app is alive on first launch. Replaced by a real backend in production. */
object Seed {
    private const val DAY = 86_400_000L
    private const val MIN = 60_000L

    private data class SeekerSeed(
        val id: String, val name: String, val cats: List<String>, val area: String, val exp: Int,
        val rMin: Int, val rMax: Int, val radius: Int, val rating: Double, val jobs: Int,
        val bio: String, val cancels: Int = 0, val speedMin: Int = 10
    )

    private val seekers = listOf(
        SeekerSeed("demo_s1", "Kamal Ahmed", listOf("plumber"), "dhanmondi", 9, 300, 900, 12, 4.9, 22, "Licensed plumber. Pipe, tap, tank and bathroom fittings. Clean work.", speedMin = 4),
        SeekerSeed("demo_s2", "Rahim Uddin", listOf("plumber"), "mohammadpur", 7, 250, 800, 10, 4.7, 16, "Plumbing repairs and new fittings. Available evenings too."),
        SeekerSeed("demo_s3", "Nusrat Begum", listOf("cleaning", "maid"), "dhanmondi", 6, 400, 1500, 10, 4.8, 14, "Deep cleaning and daily house help. Trusted by many families.", speedMin = 6),
        SeekerSeed("demo_s4", "Sumon Mia", listOf("electrician"), "mirpur", 11, 300, 1000, 12, 4.6, 20, "Wiring, fan, light, switch board and meter work."),
        SeekerSeed("demo_s5", "Jahid Hasan", listOf("electrician", "ac_repair"), "uttara", 8, 400, 2500, 15, 4.8, 18, "Electric work and AC servicing, gas refill and installation.", speedMin = 5),
        SeekerSeed("demo_s6", "Shirin Akter", listOf("cook", "maid"), "gulshan", 6, 400, 1500, 8, 4.5, 9, "Home cooking, party cooking and daily house help."),
        SeekerSeed("demo_s7", "Alamgir Hossain", listOf("carpenter", "painter"), "badda", 15, 600, 3000, 14, 4.4, 12, "Furniture repair, wardrobes, doors and wall painting."),
        SeekerSeed("demo_s8", "Rubel Sheikh", listOf("moving", "delivery"), "jatrabari", 5, 1500, 7000, 20, 4.3, 8, "Pickup truck and helpers for house shifting and delivery.", speedMin = 14),
        SeekerSeed("demo_s9", "Mina Khatun", listOf("beauty"), "banani", 4, 700, 2500, 10, 4.9, 11, "Parlour services at your home: facial, hair, bridal makeup."),
        SeekerSeed("demo_s10", "Faruk Ahmed", listOf("driver"), "farmgate", 12, 900, 2500, 20, 4.2, 7, "Experienced driver, knows Dhaka roads very well.", cancels = 1, speedMin = 25),
        SeekerSeed("demo_s11", "Tania Rahman", listOf("tutor"), "dhanmondi", 3, 400, 1000, 8, 4.7, 6, "Math and English tutor for Class 6 to 10."),
        SeekerSeed("demo_s12", "Imran Khan", listOf("mobile_repair"), "mirpur", 6, 300, 1800, 10, 4.6, 13, "Phone screen, battery, laptop OS and hardware repair."),
        SeekerSeed("demo_s13", "Delwar Hossain", listOf("plumber", "electrician"), "agrabad", 10, 300, 1000, 12, 4.5, 9, "Plumbing and electrical work in Chattogram city."),
        SeekerSeed("demo_s14", "Habib Rahman", listOf("mason", "painter"), "zindabazar", 9, 800, 2500, 15, 4.4, 7, "Masonry, tiles and painting in Sylhet."),
        SeekerSeed("demo_s15", "Sabbir Hossain", listOf("cleaning"), "rampura", 1, 400, 1200, 10, 4.5, 0, "New on Odd Jobs. Careful, punctual and affordable.", speedMin = 8),
        SeekerSeed("demo_s16", "Monir Hossain", listOf("pest", "security"), "tejgaon", 7, 900, 3500, 15, 4.6, 8, "Pest control treatments with safe chemicals.")
    )

    private data class ClientSeed(val id: String, val name: String, val area: String)

    private val clients = listOf(
        ClientSeed("demo_c1", "Farhana Islam", "dhanmondi"),
        ClientSeed("demo_c2", "Tanvir Rahman", "uttara"),
        ClientSeed("demo_c3", "Mehnaz Chowdhury", "gulshan"),
        ClientSeed("demo_c4", "Arif Hossain", "mirpur"),
        ClientSeed("demo_c5", "Sadia Karim", "mohammadpur"),
        ClientSeed("demo_c6", "Nazmul Haque", "agrabad"),
        ClientSeed("demo_c7", "Rokeya Sultana", "banani"),
        ClientSeed("demo_c8", "Shafiq Alam", "badda")
    )

    private data class Template(
        val client: String, val cat: String, val area: String, val title: String, val desc: String,
        val landmark: String, val budget: Int, val urgency: Urgency
    )

    private val templates = listOf(
        Template("demo_c1", "electrician", "dhanmondi", "Ceiling fan not working", "Bedroom fan stopped suddenly and makes a humming sound. Need check and repair or capacitor change.", "Near Star Kabab, Road 5", 600, Urgency.NOW),
        Template("demo_c5", "plumber", "mohammadpur", "Kitchen sink is leaking", "Water leaks under the kitchen sink, the pipe joint looks loose. Please bring spare fittings.", "Beside Town Hall market", 500, Urgency.TODAY),
        Template("demo_c2", "cleaning", "uttara", "Deep cleaning of 3-bedroom flat", "Full flat cleaning before moving in, around 1600 sqft. We need 2 people for one day.", "Sector 10, near Uttara Club", 3500, Urgency.THIS_WEEK),
        Template("demo_c3", "ac_repair", "gulshan", "AC not cooling (1.5 ton split)", "AC cooling is very low. Maybe gas refill and filter cleaning is needed.", "Gulshan 2, Road 90", 1800, Urgency.TODAY),
        Template("demo_c4", "moving", "mirpur", "Shift 2-bedroom flat to Uttara", "Need a pickup truck and 3 helpers. Second floor, no lift. Fragile items included.", "Mirpur 10 circle", 7500, Urgency.THIS_WEEK),
        Template("demo_c1", "tutor", "dhanmondi", "Math tutor for Class 9", "English medium student, 3 evenings a week, 1.5 hours per session. Budget is per session.", "Dhanmondi Road 27", 800, Urgency.FLEXIBLE),
        Template("demo_c7", "painter", "banani", "Paint 2 bedrooms", "Two bedrooms, paint will be provided by us. Wall putty work needed first.", "Banani Road 11", 6000, Urgency.THIS_WEEK),
        Template("demo_c8", "delivery", "badda", "Parcel pickup Motijheel to Badda", "One small parcel, need pickup from Motijheel office and delivery to my home today.", "Merul Badda", 250, Urgency.NOW),
        Template("demo_c6", "maid", "agrabad", "Part-time house help (mornings)", "Cleaning and light cooking for 3 hours every morning, 6 days a week.", "Agrabad commercial area", 1500, Urgency.FLEXIBLE),
        Template("demo_c5", "mobile_repair", "mohammadpur", "Laptop is slow, needs Windows reinstall", "Laptop takes 10 minutes to boot. Need OS reinstall and basic software setup.", "Mohammadpur Bus Stand", 700, Urgency.TODAY),
        Template("demo_c8", "carpenter", "rampura", "Fix wardrobe door hinge", "Wardrobe door is loose and does not close properly. Small repair work.", "Rampura Bridge", 450, Urgency.TODAY),
        Template("demo_c2", "pest", "uttara", "Cockroach treatment for 2-bed flat", "Gel and spray treatment for kitchen and bathrooms. Pets at home so safe chemicals only.", "Sector 4, Uttara", 1500, Urgency.THIS_WEEK),
        Template("demo_c6", "electrician", "agrabad", "Install 2 new light points", "Need two new ceiling light points and switch in the living room.", "Agrabad Access Road", 900, Urgency.TODAY),
        Template("demo_c3", "cook", "gulshan", "Cook for small family dinner", "Need an experienced cook to prepare dinner for 8 people on Friday evening.", "Gulshan 1", 1800, Urgency.THIS_WEEK)
    )

    private val goodEn = listOf(
        "Very good work and polite behaviour.", "Came on time and finished quickly. Thanks!",
        "Excellent, clean and professional.", "Great job, I will recommend this worker.",
        "Fair price and very helpful.", "Perfect work, honest and skilled."
    )
    private val goodBn = listOf(
        "খুব ভালো কাজ, ব্যবহারও চমৎকার।", "সময়মতো এসেছেন এবং দ্রুত কাজ শেষ করেছেন। ধন্যবাদ!",
        "অসাধারণ কাজ, পরিষ্কার ও দক্ষ।", "দারুণ কাজ, সবাইকে রেকমেন্ড করব।"
    )
    private val okEn = listOf("Work was okay, a bit slow but fine.", "Good enough for the price.")
    private val okBn = listOf("কাজ মোটামুটি ভালো ছিল।", "দামের তুলনায় ঠিকঠাক।")
    private val badEn = listOf("Came late and the work was poor.", "Rude behaviour and delay.")
    private val badBn = listOf("দেরিতে এসেছেন, কাজ খারাপ হয়েছে।", "বাজে ব্যবহার ও দেরি।")

    fun commentFor(rating: Int, r: Random): String = when {
        rating >= 5 -> if (r.nextInt(100) < 45) goodBn.random(r) else goodEn.random(r)
        rating == 4 -> if (r.nextInt(100) < 45) goodBn.random(r) else goodEn.random(r)
        rating == 3 -> if (r.nextBoolean()) okBn.random(r) else okEn.random(r)
        else -> if (r.nextBoolean()) badBn.random(r) else badEn.random(r)
    }

    fun tagsFor(rating: Int, r: Random): List<String> = when {
        rating >= 5 -> Ranking.positiveTags.shuffled(r).take(3)
        rating == 4 -> Ranking.positiveTags.shuffled(r).take(2)
        rating == 3 -> listOf(Ranking.positiveTags.random(r))
        else -> Ranking.negativeTags.shuffled(r).take(2)
    }

    fun initial(now: Long): SeedData {
        val r = Random(7)
        val users = ArrayList<User>()
        val jobs = ArrayList<Job>()
        val reviews = ArrayList<Review>()

        for (c in clients) {
            users.add(User(id = c.id, phone = "+88017000000" + c.id.last(), name = c.name, areaId = c.area,
                createdAt = now - 200 * DAY, profileDone = true, isDemo = true, verifyStatus = VerifyStatus.APPROVED))
        }

        for (s in seekers) {
            users.add(User(
                id = s.id, phone = "+88018000000" + s.id.removePrefix("demo_s").padStart(2, '0'), name = s.name,
                areaId = s.area, createdAt = now - 300 * DAY, profileDone = true, isSeeker = true,
                categories = s.cats, bio = s.bio, experienceYears = s.exp, rateMin = s.rMin, rateMax = s.rMax,
                radiusKm = s.radius, availableNow = true, verifyStatus = VerifyStatus.APPROVED, isDemo = true
            ))
            for (i in 0 until s.jobs) {
                val client = clients.random(r)
                val cat = s.cats.random(r)
                val c = Catalog.category(cat)
                val price = ((c.minPrice + (c.maxPrice - c.minPrice) * r.nextDouble() * 0.5) / 50).roundToInt() * 50
                val doneAt = now - (2 + r.nextInt(400)) * DAY - r.nextInt(1000) * MIN
                val id = "h_${s.id}_$i"
                val createdAt = doneAt - DAY
                val resp = (s.speedMin / 2 + r.nextInt(s.speedMin + 3)).coerceAtLeast(1)
                jobs.add(Job(
                    id = id, clientId = client.id, title = c.en, description = "", categoryId = cat, areaId = s.area,
                    landmark = "", urgency = Urgency.TODAY, budget = price, negotiable = true, payMethod = PayMethod.CASH,
                    arrivalCode = "0000", createdAt = createdAt, status = JobStatus.COMPLETED, hiredSeekerId = s.id,
                    agreedPrice = price,
                    applications = listOf(Application(s.id, price, "", 30, createdAt + resp * MIN, resp, AppStatus.ACCEPTED)),
                    events = listOf(JobEvent(JobStatus.OPEN, client.id, createdAt), JobEvent(JobStatus.COMPLETED, client.id, doneAt)),
                    completedAt = doneAt, paidConfirmed = true, onTime = r.nextInt(100) < 92,
                    clientReviewed = true, seekerReviewed = false
                ))
                val rating = (s.rating + (r.nextDouble() - 0.5) * 1.6).roundToInt().coerceIn(2, 5)
                reviews.add(Review(
                    id = "r_${s.id}_$i", jobId = id, reviewerId = client.id, revieweeId = s.id, revieweeIsSeeker = true,
                    rating = rating, tags = tagsFor(rating, r),
                    comment = if (r.nextInt(100) < 70) commentFor(rating, r) else "", createdAt = doneAt + 3600_000L
                ))
            }
            for (k in 0 until s.cancels) {
                val client = clients.random(r)
                val at = now - (20 + r.nextInt(100)) * DAY
                jobs.add(Job(
                    id = "x_${s.id}_$k", clientId = client.id, title = "Cancelled job", description = "",
                    categoryId = s.cats.first(), areaId = s.area, landmark = "", urgency = Urgency.TODAY, budget = 1000,
                    negotiable = true, payMethod = PayMethod.CASH, arrivalCode = "0000", createdAt = at,
                    status = JobStatus.CANCELLED, hiredSeekerId = s.id, cancelledBy = s.id, cancelReason = "Personal reason",
                    events = listOf(JobEvent(JobStatus.OPEN, client.id, at), JobEvent(JobStatus.CANCELLED, s.id, at + 3600_000L))
                ))
            }
        }

        jobs.addAll(openFromTemplates(now, 10, 0, r, users))
        return SeedData(users, jobs, reviews)
    }

    /** Creates fresh open jobs from demo clients, with a few pre-existing offers for realism. */
    fun openFromTemplates(now: Long, count: Int, start: Int, r: Random, users: List<User>): List<Job> {
        val out = ArrayList<Job>()
        for (i in 0 until count) {
            val t = templates[(start + i) % templates.size]
            val created = now - (3 + r.nextInt(120)) * MIN
            val id = "j_" + now + "_" + (start + i) + "_" + r.nextInt(100000)
            val apps = users.filter {
                it.isDemo && it.isSeeker && t.cat in it.categories &&
                    Geo.between(it.areaId, t.area) <= it.radiusKm && r.nextInt(100) < 55
            }.take(2).map { sk ->
                val resp = (2 + r.nextInt(20))
                Application(sk.id, (t.budget * (0.9 + r.nextDouble() * 0.3) / 10).roundToInt() * 10,
                    "I can do this work today.", 20 + r.nextInt(60), created + resp * MIN, resp)
            }
            out.add(Job(
                id = id, clientId = t.client, title = t.title, description = t.desc, categoryId = t.cat,
                areaId = t.area, landmark = t.landmark, urgency = t.urgency, budget = t.budget, negotiable = true,
                payMethod = if (r.nextInt(100) < 25) PayMethod.BKASH else PayMethod.CASH,
                arrivalCode = Random.nextInt(1000, 10000).toString(), createdAt = created,
                applications = apps, events = listOf(JobEvent(JobStatus.OPEN, t.client, created))
            ))
        }
        return out
    }
}
