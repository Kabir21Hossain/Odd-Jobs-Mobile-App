# Decisions
* **Local-first, no backend yet** so the app runs the moment you open it; rules are isolated for a later server.
* **Java serialization to a file** instead of Room: zero setup, fine for demo-size data. If the data classes change, the old file is discarded and demo data re-seeded.
* **Inline bilingual strings `tr(en, bn)`** instead of XML resources: instant language switch, no activity restart, one place per screen. Move to `strings.xml` if you add more languages.
* **Navigation Compose**, single activity, role-aware bottom bar (Hire / Work modes on one account).
* **Payments are recorded, not processed** (cash / bKash / Nagad as method). No escrow wording until licensing is clarified.
* **targetSdk 34**: fine for installing and testing. Google Play requires a newer target for new apps — use Android Studio's *Tools → AGP Upgrade Assistant* and raise `compileSdk/targetSdk` before publishing.
