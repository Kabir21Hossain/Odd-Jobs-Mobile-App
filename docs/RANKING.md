# Worker ranking

Implemented in `domain/Ranking.kt` and covered by `DomainTest`.

**TrustScore (0–100)** = 100 × (0.35·R + 0.20·C + 0.15·T + 0.10·V + 0.10·Q + 0.10·A) − penalties

| Term | Meaning |
|---|---|
| R | Bayesian-adjusted star rating, recency-weighted (half-life 180 days). `(v·avg + 5·4.3)/(v+5)` → 0..1. A single 5★ review cannot beat 30 good reviews. |
| C | Completion rate `(completed+2)/(completed+cancelledBySeeker+2)` |
| T | Responsiveness: median minutes to first offer. ≤5 min = 1.0, decays to 0 at 6 h |
| V | Verification: phone only 0.3, ID verified 0.7 |
| Q | Review quality: 50% positive-vs-negative tag ratio + 50% comment sentiment (English + Bangla lexicon, `Sentiment`) |
| A | Activity: jobs completed in the last 30 days (cap 5) and on-time arrival rate |
| Penalties | 3 points per job the worker cancelled (max 15) |

**Match score** for a specific job or client (worker lists, offers sorted for a client):
`0.55·Trust/100 + 0.20·Proximity + 0.15·CategoryExperience + 0.10·PriceFit`
Proximity is 1.0 within 1 km and falls to 0 at the worker's service radius.

**Fairness:** workers with fewer than 3 reviews are labelled *New*; their rating is shrunk toward the platform mean so they are neither buried nor over-promoted.

**Integrity rules enforced in `Engine`:** reviews only after a COMPLETED job, one per party per job, only by the client or the hired worker.
Upgrade path: replace the lexicon with an ML sentiment model, add review-ring detection server-side.
