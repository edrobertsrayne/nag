# Research: Android nag-notification plumbing

**Ticket:** edrobertsrayne/fine#4 (wayfinder research)
**Date:** 2026-08-28
**Target device:** sideloaded personal app, Pixel 8, Android 17 (API level 37)
**Requirement:** escalating, time-of-day-aware daily notification schedule — weekday pre-8am and evening windows, hourly evening escalation, weekend schedule, silent 08:00–18:00 Mon–Fri.

All claims below are verified against current official Android developer documentation (developer.android.com), except where the source is the AOSP documentation site (source.android.com). Sources cited inline.

---

## TL;DR — recommended approach

Use **AlarmManager exact alarms, self-rescheduling** (after each fire, schedule the next event), plus a **BOOT_COMPLETED receiver** to re-arm after reboot. Do **not** use WorkManager for the firing schedule. Request **POST_NOTIFICATIONS** at first launch, declare **USE_EXACT_ALARM** (simplest for a sideloaded personal app), and put the app on the **battery-optimisation allowlist ("Unrestricted")** once, manually or via the direct request dialog.

Concretely, the app needs:

1. **Runtime permission:** `POST_NOTIFICATIONS` requested in-context on first launch (Android 13+ behaviour).
2. **Scheduler:** one `AlarmManager` alarm per "next event" in the daily schedule, set with `setExactAndAllowWhileIdle()` (or `setAlarmClock()` if you want the pre-8am one treated like a user alarm). Each receiver fires the notification, computes the next schedule slot, and re-arms.
3. **Persistence across reboot/update/time changes:** manifest receivers for `ACTION_BOOT_COMPLETED`, `ACTION_MY_PACKAGE_REPLACED`, `ACTION_TIME_SET` (user sets clock), and `ACTION_TIMEZONE_CHANGED`, all of which re-run the same "schedule next event" function.
4. **Battery:** request `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (fine for a sideloaded personal app; forbidden-ish on Play) or direct the user to Settings → Apps → *app* → App battery usage → **Unrestricted**.
5. **Channels:** create 2–3 channels up front by intrusiveness (e.g. `nag_quiet` = IMPORTANCE_LOW, `nag_default` = IMPORTANCE_DEFAULT, `nag_urgent` = IMPORTANCE_HIGH). Channel importance is immutable after creation, so "escalation" is implemented by choosing a louder channel per event, not by mutating one channel.

---

## 1. POST_NOTIFICATIONS runtime permission (sideloaded, non-Play)

- Android 13 (API 33) introduced the runtime permission `POST_NOTIFICATIONS`. Declare it in the manifest: `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`. ([Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission), [Behavior changes: all apps, Android 13](https://developer.android.com/about/versions/13/behavior-changes-all))
- On Android 13+, **a new install's notifications are off by default**; the app must request the permission and the user must grant it before any notification appears. ([Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission), [AOSP: Notification permission for opt-in notifications](https://source.android.com/docs/core/display/notification-perm))
- Timing of the dialog depends on targetSdk:
  - **Target 33+:** the app controls when the dialog appears — request it in context on first launch (recommended).
  - **Target 32 or lower:** the system shows the dialog automatically on the first activity launch after the app creates its first notification channel. ([Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission))
- Request it like any runtime permission: `ActivityResultContracts.RequestPermission()` in Compose, or `requestPermissions()` in Views. Guard every `notify()` call with a permission check / `NotificationManagerCompat.areNotificationsEnabled()`, because the user can revoke it at any time in Settings. ([Create a notification](https://developer.android.com/develop/ui/views/notifications/build-notification), [Top Tips for Adopting Android's Notification Permission](https://medium.com/androiddevelopers/top-tips-for-adopting-androids-notification-permission-bf69afd677b8))
- **Sideloaded (non-Play) makes no difference to this flow.** The permission prompt, the off-by-default state, and the revocation path are all the same; there is no Play-specific pre-grant involved. The only sideload-relevant wrinkle: nobody but you is nudged to grant it, so build a first-launch screen that asks.

## 2. WorkManager vs AlarmManager

**Verdict: AlarmManager (exact) for the firing schedule; WorkManager is not suitable as the trigger primitive.**

- WorkManager is the recommended API for *deferrable, guaranteed* background work and it **persists across app restarts and device reboots** (work is stored in an internal SQLite database and rescheduled after reboot). ([Task scheduling](https://developer.android.com/develop/background-work/background-tasks/persistent))
- But it is explicitly **inexact**: periodic work has a **minimum interval of 15 minutes**, execution "may be delayed because WorkManager is subject to OS battery optimizations, such as doze mode", and the actual run time is chosen by the system within the interval/flex window. ([PeriodicWorkRequest](https://developer.android.com/reference/kotlin/androidx/work/PeriodicWorkRequest), [Define work requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work))
- A recent WorkManager addition, `setNextScheduleTimeOverride()` on `PeriodicWorkRequest.Builder`, nominally lets you pin the next periodic run time ("without drift"), but its own documentation states: **"Work will almost never run at this exact time in the real world"** — actual run times remain subject to the system scheduler, Doze, and constraints. It is a hint, not a trigger guarantee. ([PeriodicWorkRequest.Builder](https://developer.android.com/reference/kotlin/androidx/work/PeriodicWorkRequest.Builder))
- A nag schedule needs to fire *at* 07:xx, *at* the evening window edges, and *roughly hourly* in the evening. AlarmManager exact alarms are the primitive designed for "user-intentioned notifications at a precise time" — the docs' own example of appropriate exact-alarm use is precisely a calendar/notification that must make sound at a requested time. ([Schedule alarms](https://developer.android.com/develop/background-work/services/alarms), [AlarmManagerCompat.setExactAndAllowWhileIdle](https://developer.android.com/reference/androidx/core/app/AlarmManagerCompat))
- **Exact vs inexact choice:** `setExactAndAllowWhileIdle()` fires at the requested time even in Doze (throttled to at most once per 9 minutes per app while idle — irrelevant for hourly evening nags, plenty for this schedule). Plain `setExact()` is deferred to the next Doze maintenance window, so do not use it as the only mechanism for a schedule that must survive overnight idle. `setAlarmClock()` fires normally during Doze and the system exits Doze shortly before it fires — strongest guarantee, and gives the user-visible "alarm" affordance. ([Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby))
- Exact alarm APIs (`setExact()`, `setExactAndAllowWhileIdle()`, `setAlarmClock()`) require the exact-alarm special access on Android 12+ (API 31+) for apps targeting 31+; calling them without it throws `SecurityException`. ([Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms), [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms))
- **Exact-alarm permission, API-level behaviour:**
  - **API 31–32 (target ≤32 on Android 12/12L):** `SCHEDULE_EXACT_ALARM` pre-granted at install.
  - **API 33:** `USE_EXACT_ALARM` introduced for targetSdk 33+ — **granted automatically at install and cannot be revoked by the user**, but Play policy limits it to calendar/alarm-clock apps. ([Schedule alarms](https://developer.android.com/develop/background-work/services/alarms))
  - **Android 14 (API 34) and later:** `SCHEDULE_EXACT_ALARM` is **denied by default** for newly installed apps targeting 33+ (unless they're calendar/alarm-clock category). Apps must call `AlarmManager.canScheduleExactAlarms()` before scheduling, and request access with `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; the system then sends `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` when granted (a foreground broadcast — reschedule alarms there). If the user revokes the permission, **all scheduled exact alarms are deleted**. ([Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms), [AlarmManager reference](https://developer.android.com/reference/android/app/AlarmManager))
- **Permission choice for this app:** the app's core function *is* precisely-timed reminders, i.e. the alarm-clock category of use case. Because it is **sideloaded and never Play-distributed**, `USE_EXACT_ALARM` is the simplest reliable option: auto-granted, non-revocable, zero settings trips. (Play policy restricting `USE_EXACT_ALARM` to calendar/alarm apps is enforced at Play review, which this app never goes through.) If you'd rather stay on the sanctioned-in-any-context path, use `SCHEDULE_EXACT_ALARM` + the `canScheduleExactAlarms()` / `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` flow. Either way, code the graceful-degradation branch: if `canScheduleExactAlarms()` is false, fall back to `setWindow()`/`setAndAllowWhileIdle()` rather than crashing. ([Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms), [Request special permissions](https://developer.android.com/training/permissions/requesting-special))

## 3. Surviving reboots (and updates, and clock changes)

- **All AlarmManager alarms are cleared when the device shuts down or reboots** — this is not optional plumbing. ([Schedule alarms — Start an alarm when the device restarts](https://developer.android.com/develop/background-work/services/alarms), [AlarmManager reference](https://developer.android.com/reference/android/app/AlarmManager))
- Pattern: declare `RECEIVE_BOOT_COMPLETED`, add a manifest receiver for `ACTION_BOOT_COMPLETED`, and re-run the "compute next event and set exact alarm" function in `onReceive()`. The receiver does nothing but reschedule — keep it fast. (WorkManager, by contrast, persists across reboots for free, but as shown above it cannot hit times — so reboot-survival must be hand-rolled here.) ([Schedule alarms](https://developer.android.com/develop/background-work/services/alarms))
- **BOOT_COMPLETED is only delivered if the app has been launched by the user at least once since install** — launch the app once after sideloading. ([Schedule alarms](https://developer.android.com/develop/background-work/services/alarms))
- If the app is in the **"restricted" app standby bucket** (Android 13+), the system withholds `BOOT_COMPLETED` until the app is started for other reasons. Not opening the app for a long time can get you there; the battery-exemption step below prevents it. ([Background optimization](https://developer.android.com/topic/performance/background-optimization))
- **Sideload gotcha:** updates do **not** reboot the device, so `BOOT_COMPLETED` won't fire after installing an updated APK. Add a receiver for `ACTION_MY_PACKAGE_REPLACED` (sent to your own package after an update) and reschedule there. ([Broadcasts overview](https://developer.android.com/develop/background-work/background-tasks/broadcasts) — manifest-declared receivers; `ACTION_MY_PACKAGE_REPLACED` is one of the exempt implicit broadcasts still deliverable to manifest receivers)
- A time-of-day schedule must also handle the user changing the clock or timezone: listen for `ACTION_TIME_CHANGED` / `ACTION_TIMEZONE_CHANGED` and recompute. (Standard practice for time-of-day alarms; these remain manifest-deliverable exempt implicit broadcasts per [Broadcasts overview](https://developer.android.com/develop/background-work/background-tasks/broadcasts).)
- **Android 15 (API 35) note:** `BOOT_COMPLETED` receivers can no longer launch certain foreground service types (`dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection`, `microphone`). Not a problem for this design — the boot receiver only reschedules alarms and posts nothing itself. ([Behavior changes: Apps targeting Android 15+](https://developer.android.com/about/versions/15/behavior-changes-15))

## 4. Doze / battery optimisation

- **Doze** (device stationary, screen off, on battery — a Pixel 8 left overnight) suspends network and wake locks and **defers standard alarms including `setExact()` and `setWindow()` to the next maintenance window**. Alarms set with `setAndAllowWhileIdle()` / `setExactAndAllowWhileIdle()` still fire in Doze (max once per 9 minutes per app), and **`setAlarmClock()` alarms continue to fire normally — the system exits Doze shortly before them**. ([Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby))
- **Standby buckets** add frequency limits on alarms per bucket; "user manually unrestricts app battery" removes alarm execution limits entirely. ([Power management resource limits](https://developer.android.com/topic/performance/power/power-details))
- **Exemption for a personal sideloaded app — minimal steps:**
  1. Check `PowerManager.isIgnoringBatteryOptimizations(packageName)`.
  2. Request the exemption with `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (requires the `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` manifest permission) — a single direct "Allow app to always run in background?" dialog; **or** send the user to the list screen with `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`; **or** the fully manual route: **Settings → Apps → All apps → *app* → App battery usage → select "Unrestricted"**. ([PowerManager.isIgnoringBatteryOptimizations](https://developer.android.com/reference/android/os/PowerManager), [Settings reference](https://developer.android.com/reference/kotlin/android/provider/Settings), [Optimize for Doze](https://developer.android.com/training/monitoring-device-state/doze-standby))
  - Google Play policies prohibit requesting direct exemption unless core function demands it — **irrelevant for a sideloaded personal app**, and the docs' own use-case table lists "task automation app: core function is scheduling automated actions" as an acceptable exemption case anyway.
- Note that being on the power allowlist also makes the app **always allowed** to call `setExact()`/`setExactAndAllowWhileIdle()` regardless of the exact-alarm permission state — a useful belt-and-braces with `USE_EXACT_ALARM`. ([Schedule exact alarms are denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms))

## 5. Notification channel basics

- Channels are mandatory on API 26+; **create them before posting any notification**, ideally as soon as the app starts. Re-creating an existing channel with the same values is a no-op, so it is safe to call on every launch. ([Create a notification](https://developer.android.com/develop/ui/views/notifications/build-notification), [Create and manage notification channels](https://developer.android.com/develop/ui/compose/notifications/channels))
- `NotificationChannel(id, name, importance)` — importance runs `IMPORTANCE_NONE(0)` → `IMPORTANCE_HIGH(4)`: HIGH = sound + heads-up; DEFAULT = sound; LOW = silent; NONE = hidden entirely. Post with `NotificationManagerCompat.notify(id, notification)`. ([Create and manage notification channels](https://developer.android.com/develop/ui/compose/notifications/channels))
- **Importance is immutable after creation** — you cannot raise it programmatically later, and the user has final control over every channel's settings. `createNotificationChannel()` on an existing channel will only ever *lower* importance, and only if the user hasn't touched it. ([NotificationManager.createNotificationChannel](https://developer.android.com/reference/android/app/NotificationManager), [NotificationChannel.setImportance — only modifiable before submission](https://developer.android.com/reference/android/app/NotificationChannel))
- Design implication for "escalating" nags: **use multiple channels created up front** (e.g. quiet → default → urgent) and escalate by selecting the louder channel per event. Don't try to mutate one channel's importance.
- For "silent 08:00–18:00 Mon–Fri", either simply don't post during that window, or post to a `IMPORTANCE_LOW` (silent, no status-bar icon... actually LOW is silent but visible; MIN is not in the status bar) channel if a passive indicator is wanted. Channel choice is per-notification at post time. ([Importance table](https://developer.android.com/develop/ui/compose/notifications/channels))

---

## Recommended architecture (build-spec summary)

```
manifest:
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
  <uses-permission android:name="android.permission.USE_EXACT_ALARM"/>   (or SCHEDULE_EXACT_ALARM)
  <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>
  receivers: ScheduleReceiver (exact-alarm fired), BootReceiver
             (BOOT_COMPLETED | MY_PACKAGE_REPLACED | TIME_SET | TIMEZONE_CHANGED)

core function: scheduleNext(context)
  computes the next event in the daily schedule from "now"
  (weekday/weekend table; silent window simply yields no post, just next slot)
  → alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, t, pendingIntent)
    (or setAlarmClock() for the pre-8am slot)

ScheduleReceiver.onReceive:
  → post notification on the channel matching the event's escalation level
  → scheduleNext(context)   // self-rescheduling
```

## Exact caveats the build spec MUST mention

1. **POST_NOTIFICATIONS is off by default on Android 13+**; request on first launch in-context, and guard every `notify()` with `areNotificationsEnabled()` (user can revoke any time).
2. **`SCHEDULE_EXACT_ALARM` is denied by default on Android 14+ for new installs targeting 33+** — if that permission is chosen instead of `USE_EXACT_ALARM`, the spec must include the `canScheduleExactAlarms()` guard, the `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` flow, the `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` reschedule receiver, and a `setWindow()` fallback when denied. `USE_EXACT_ALARM` avoids all of this but must never ship to Play.
3. **All alarms are erased on reboot** — `BOOT_COMPLETED` rescheduling is required, and it only fires if the app has been **launched at least once** since install/update.
4. **APK updates don't reboot:** reschedule on `ACTION_MY_PACKAGE_REPLACED` too, and on `TIME_SET`/`TIMEZONE_CHANGED` (time-of-day schedule).
5. **Do not start a foreground service from the boot receiver** (Android 15+ restrictions); the receiver should only reschedule and return.
6. **Doze:** use `setExactAndAllowWhileIdle()` (9-minute-per-app throttle in Doze — fine for hourly nags) or `setAlarmClock()`; plain `setExact()` is deferred overnight. Battery-optimisation **exemption ("Unrestricted") must be applied once manually or via the request dialog**; without it the Pixel may defer alarms and can place the app in the restricted bucket, which also withholds `BOOT_COMPLETED`.
7. **Channel importance is immutable after creation** — create quiet/default/urgent channels up front and escalate by channel selection; the user can override any channel's behaviour in Settings.
8. **Android 17 (API 37), targeting 37+:** stricter memory checks on notifications using custom views — stick to standard notification templates. ([Android 17 features and changes](https://developer.android.com/about/versions/17/summary))

## Sources

- [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission) · [Behavior changes: all apps (13)](https://developer.android.com/about/versions/13/behavior-changes-all) · [AOSP: Notification permission](https://source.android.com/docs/core/display/notification-perm)
- [Schedule alarms (AlarmManager guide)](https://developer.android.com/develop/background-work/services/alarms) · [Schedule exact alarms are denied by default (14)](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms) · [Request special permissions](https://developer.android.com/training/permissions/requesting-special) · [AlarmManager reference](https://developer.android.com/reference/android/app/AlarmManager) · [AlarmManagerCompat](https://developer.android.com/reference/androidx/core/app/AlarmManagerCompat)
- [Task scheduling / WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent) · [Define work requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) · [PeriodicWorkRequest](https://developer.android.com/reference/kotlin/androidx/work/PeriodicWorkRequest) · [PeriodicWorkRequest.Builder (setNextScheduleTimeOverride)](https://developer.android.com/reference/kotlin/androidx/work/PeriodicWorkRequest.Builder)
- [Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby) · [Power management resource limits](https://developer.android.com/topic/performance/power/power-details) · [PowerManager](https://developer.android.com/reference/android/os/PowerManager) · [Settings (battery exemption intents)](https://developer.android.com/reference/kotlin/android/provider/Settings)
- [Background optimization (restricted bucket / BOOT_COMPLETED)](https://developer.android.com/topic/performance/background-optimization) · [Broadcasts overview](https://developer.android.com/develop/background-work/background-tasks/broadcasts) · [Behavior changes: Apps targeting Android 15+](https://developer.android.com/about/versions/15/behavior-changes-15)
- [Create a notification](https://developer.android.com/develop/ui/views/notifications/build-notification) · [Create and manage notification channels](https://developer.android.com/develop/ui/compose/notifications/channels) · [NotificationManager](https://developer.android.com/reference/android/app/NotificationManager) · [NotificationChannel](https://developer.android.com/reference/android/app/NotificationChannel)
- [Android 17 features and changes](https://developer.android.com/about/versions/17/summary) · [Android 17 behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17)
