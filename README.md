# 🎵 SubManager

**SubManager** is a dedicated dashboard and management platform for sharing and tracking Spotify Premium group subscriptions.

![SubManager Banner](https://img.shields.io/badge/Spotify-SubManager-1DB954?style=for-the-badge&logo=spotify&logoColor=white)

## ✨ Features

- **📱 Android App & Web Portal:** Manage subscriptions on the go using the beautifully crafted native Android application, or access the dashboard from any device via the web portal.
- **📊 Real-time Dashboard:** Track who has paid, who is overdue, and exactly how much revenue has been collected for the month.
- **🟡 Grace Period Logic:** Intelligent payment tracking gives members an 8-day grace period each month, indicating "Due in X Days" before marking them as late.
- **💸 Advance Payments:** Automatic handling of lump-sum payments! If a user pays for multiple months in advance, the system intuitively splits it and marks future months as completely paid up.
- **🔔 Push Notifications:** Integrated Firebase Cloud Messaging (FCM) sends timely automated reminders to users who have payments due.
- **☁️ Supabase Backend:** Secure, fast, and scalable backend running entirely on Supabase PostgreSQL and Edge Functions.

## 🚀 Getting Started

### Access the Web Portal
You can view the group subscription dashboard from any browser:
**[Go to Web Portal 🌐](https://dmstyles.github.io/SubManager/web/index.html)**

### Download the Android App
For the best experience, download the latest native Android APK:
**[Download Latest APK 📦](https://github.com/DMStyles/SubManager/releases/latest)**

## 🛠️ Tech Stack

- **Android App:** Kotlin, Jetpack Compose, Coroutines
- **Web Portal:** HTML, CSS, Vanilla JS
- **Backend & Auth:** Supabase (Auth, Database, Storage)
- **Push Notifications:** Firebase Cloud Messaging & GitHub Actions (Cron Jobs)

## 📌 Release Notes (v1.9)
- Fixed retroactive advance payment allocation in the Admin dashboard.
- Redesigned status badge logic for 1st-8th grace periods.
- Added in-app OTA update checker functionality.

---
*Created and maintained by [DMStyles](https://github.com/DMStyles).*
