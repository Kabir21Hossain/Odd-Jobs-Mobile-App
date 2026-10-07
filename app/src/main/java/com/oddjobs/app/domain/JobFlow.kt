package com.oddjobs.app.domain

/** Server-style state machine: who may move a job from one status to another. */
object JobFlow {
    private val rules: Map<Pair<JobStatus, JobStatus>, Set<Actor>> = mapOf(
        (JobStatus.OPEN to JobStatus.HIRED) to setOf(Actor.CLIENT, Actor.SYSTEM),
        (JobStatus.OPEN to JobStatus.CANCELLED) to setOf(Actor.CLIENT),
        (JobStatus.OPEN to JobStatus.EXPIRED) to setOf(Actor.SYSTEM),
        (JobStatus.HIRED to JobStatus.ON_THE_WAY) to setOf(Actor.SEEKER),
        (JobStatus.HIRED to JobStatus.CANCELLED) to setOf(Actor.CLIENT, Actor.SEEKER),
        (JobStatus.ON_THE_WAY to JobStatus.ARRIVED) to setOf(Actor.SEEKER),
        (JobStatus.ON_THE_WAY to JobStatus.CANCELLED) to setOf(Actor.CLIENT, Actor.SEEKER),
        (JobStatus.ARRIVED to JobStatus.IN_PROGRESS) to setOf(Actor.SEEKER),
        (JobStatus.IN_PROGRESS to JobStatus.AWAITING_CONFIRMATION) to setOf(Actor.SEEKER),
        (JobStatus.AWAITING_CONFIRMATION to JobStatus.COMPLETED) to setOf(Actor.CLIENT, Actor.SYSTEM)
    )

    fun canTransition(from: JobStatus, to: JobStatus, actor: Actor): Boolean =
        rules[from to to]?.contains(actor) == true

    /** The next forward step a seeker can take, if any. */
    fun nextForSeeker(from: JobStatus): JobStatus? = when (from) {
        JobStatus.HIRED -> JobStatus.ON_THE_WAY
        JobStatus.ON_THE_WAY -> JobStatus.ARRIVED
        JobStatus.ARRIVED -> JobStatus.IN_PROGRESS
        JobStatus.IN_PROGRESS -> JobStatus.AWAITING_CONFIRMATION
        else -> null
    }

    val steps = listOf(
        JobStatus.HIRED, JobStatus.ON_THE_WAY, JobStatus.ARRIVED,
        JobStatus.IN_PROGRESS, JobStatus.AWAITING_CONFIRMATION, JobStatus.COMPLETED
    )
}
