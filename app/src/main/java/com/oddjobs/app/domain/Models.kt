package com.oddjobs.app.domain

import java.io.Serializable

enum class Mode { CLIENT, SEEKER }
enum class Urgency { NOW, TODAY, THIS_WEEK, FLEXIBLE }
enum class PayMethod { CASH, BKASH, NAGAD }
enum class VerifyStatus { NONE, SUBMITTED, APPROVED, REJECTED }
enum class AppStatus { PENDING, ACCEPTED, REJECTED, WITHDRAWN }
enum class SortBy { BEST_MATCH, NEAREST, NEWEST, BUDGET }
enum class Actor { CLIENT, SEEKER, SYSTEM }

enum class JobStatus {
    OPEN, HIRED, ON_THE_WAY, ARRIVED, IN_PROGRESS, AWAITING_CONFIRMATION, COMPLETED, CANCELLED, EXPIRED
}

data class Category(
    val id: String,
    val en: String,
    val bn: String,
    val emoji: String,
    val minPrice: Int,
    val maxPrice: Int
) : Serializable {
    fun name(lang: String) = if (lang == "bn") bn else en
}

data class Area(
    val id: String,
    val district: String,
    val districtBn: String,
    val en: String,
    val bn: String,
    val lat: Double,
    val lng: Double
) : Serializable {
    fun name(lang: String) = if (lang == "bn") bn else en
    fun districtName(lang: String) = if (lang == "bn") districtBn else district
    fun full(lang: String) = name(lang) + ", " + districtName(lang)
}

data class User(
    val id: String,
    val phone: String,
    val name: String,
    val areaId: String,
    val createdAt: Long,
    val photoPath: String? = null,
    val profileDone: Boolean = false,
    val isSeeker: Boolean = false,
    val categories: List<String> = emptyList(),
    val bio: String = "",
    val experienceYears: Int = 0,
    val rateMin: Int = 300,
    val rateMax: Int = 800,
    val radiusKm: Int = 10,
    val availableNow: Boolean = true,
    val verifyStatus: VerifyStatus = VerifyStatus.NONE,
    val verifySubmittedAt: Long = 0L,
    val idPhotos: List<String> = emptyList(),
    val isDemo: Boolean = false,
    val savedJobIds: Set<String> = emptySet(),
    val hiddenJobIds: Set<String> = emptySet(),
    val favoriteSeekerIds: Set<String> = emptySet(),
    val blockedIds: Set<String> = emptySet()
) : Serializable {
    /** 1 = phone verified, 2 = identity verified. */
    val level: Int get() = when {
        verifyStatus == VerifyStatus.APPROVED -> 2
        phone.isNotBlank() -> 1
        else -> 0
    }
}

data class Application(
    val seekerId: String,
    val price: Int,
    val message: String,
    val etaMin: Int,
    val createdAt: Long,
    val responseMin: Int,
    val status: AppStatus = AppStatus.PENDING
) : Serializable

data class JobEvent(val status: JobStatus, val actorId: String, val at: Long) : Serializable

data class Job(
    val id: String,
    val clientId: String,
    val title: String,
    val description: String,
    val categoryId: String,
    val areaId: String,
    val landmark: String,
    val urgency: Urgency,
    val budget: Int,
    val negotiable: Boolean,
    val payMethod: PayMethod,
    val arrivalCode: String,
    val createdAt: Long,
    val photos: List<String> = emptyList(),
    val scheduledAt: Long = 0L,
    val onlyVerified: Boolean = false,
    val minRating: Double = 0.0,
    val invitedSeekerId: String? = null,
    val status: JobStatus = JobStatus.OPEN,
    val hiredSeekerId: String? = null,
    val agreedPrice: Int = 0,
    val applications: List<Application> = emptyList(),
    val events: List<JobEvent> = emptyList(),
    val completedAt: Long = 0L,
    val cancelledBy: String? = null,
    val cancelReason: String = "",
    val paidConfirmed: Boolean = false,
    val onTime: Boolean = true,
    val clientReviewed: Boolean = false,
    val seekerReviewed: Boolean = false
) : Serializable {
    val lastEventAt: Long get() = events.lastOrNull()?.at ?: createdAt
    val isActive: Boolean
        get() = status in setOf(
            JobStatus.HIRED, JobStatus.ON_THE_WAY, JobStatus.ARRIVED,
            JobStatus.IN_PROGRESS, JobStatus.AWAITING_CONFIRMATION
        )
    val isClosed: Boolean
        get() = status == JobStatus.COMPLETED || status == JobStatus.CANCELLED || status == JobStatus.EXPIRED
}

data class Review(
    val id: String,
    val jobId: String,
    val reviewerId: String,
    val revieweeId: String,
    val revieweeIsSeeker: Boolean,
    val rating: Int,
    val tags: List<String>,
    val comment: String,
    val createdAt: Long
) : Serializable

data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val at: Long
) : Serializable

data class Notice(
    val id: String,
    val userId: String,
    val titleEn: String,
    val titleBn: String,
    val bodyEn: String,
    val bodyBn: String,
    val at: Long,
    val read: Boolean = false,
    val jobId: String? = null,
    val chatId: String? = null
) : Serializable {
    fun title(lang: String) = if (lang == "bn") titleBn else titleEn
    fun body(lang: String) = if (lang == "bn") bodyBn else bodyEn
}

data class Report(
    val id: String,
    val reporterId: String,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val at: Long
) : Serializable

data class AppData(
    val schema: Int = SCHEMA,
    val currentUserId: String? = null,
    val mode: Mode = Mode.CLIENT,
    val lang: String = "bn",
    val langChosen: Boolean = false,
    val demoSim: Boolean = true,
    val pendingPhone: String? = null,
    val pendingOtp: String? = null,
    val otpSentAt: Long = 0L,
    val users: List<User> = emptyList(),
    val jobs: List<Job> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val reports: List<Report> = emptyList(),
    val chatRead: Map<String, Long> = emptyMap()
) : Serializable {
    val me: User? get() = users.firstOrNull { it.id == currentUserId }
    fun user(id: String?): User? = if (id == null) null else users.firstOrNull { it.id == id }
    fun job(id: String?): Job? = if (id == null) null else jobs.firstOrNull { it.id == id }

    companion object {
        const val SCHEMA = 2
    }
}

data class Out(val data: AppData, val error: String? = null, val value: String? = null) {
    val ok: Boolean get() = error == null
}

fun chatIdOf(jobId: String, seekerId: String) = "$jobId~$seekerId"
fun jobIdOfChat(chatId: String) = chatId.substringBefore('~')
fun seekerIdOfChat(chatId: String) = chatId.substringAfter('~')
