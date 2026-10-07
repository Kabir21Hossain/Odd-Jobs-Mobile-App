# Play Store checklist

| Item | Status |
|---|---|
| Launcher icon (adaptive + legacy + round) | Done |
| 512×512 store icon | Done — `docs/store/play_store_icon_512.png` |
| Bangla + English UI | Done |
| Report / block users, content moderation hooks | Done in app (needs moderation backend) |
| In-app account deletion | Done (Settings) — Play also needs a **web link** for deletion requests |
| Terms & Privacy text | **Draft only** — have a Bangladeshi lawyer review; host Privacy Policy on a public URL |
| Data Safety form | Fill in: phone, name, photos, location (area), ID documents, messages, crash data |
| Raise `targetSdk` / `compileSdk` | **TODO** to Play's current requirement |
| Release signing + AAB | **TODO** Build → Generate Signed Bundle, keep the keystore safe |
| Real backend (SMS OTP, multi-user, push) | **TODO** — demo mode is not acceptable for a public release |
| Replace placeholder support email in `SharedScreens.kt` | **TODO** |
| Screenshots (phone), feature graphic 1024×500, descriptions | **TODO** |
| New personal developer accounts must run a closed test before production | Check Play Console's current rule |
| Enable R8 (`isMinifyEnabled = true`) and test the release build | **TODO** |
