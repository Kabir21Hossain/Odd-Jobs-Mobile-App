# Architecture

Odd Jobs is a local-first Android app. User profiles, jobs, chats, and marketplace state are stored on the device by `AppRepository`. Sign-in uses a local demo OTP displayed in the app.

The marketplace simulation and business rules live in `domain/Engine.kt` and `domain/Simulation.kt`. A production multi-device service would require a separately designed backend and authentication system.
