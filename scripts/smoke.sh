#!/usr/bin/env bash
# Smoke checklist runner for #7's 12-item owner checklist (issue #16).
# Runs against one attached device (Pixel 8), in order, and prints PASS/FAIL
# per item. Steps that need human eyes prompt and wait.
#
# Usage: scripts/smoke.sh [first-digit-or-step-name...]  (no args = all steps)
# Requires: adb on PATH, device attached + authorized.
set -uo pipefail

PKG=dev.nag
OUT=${OUT:-/tmp/opencode/nag-smoke}
APK=app/build/outputs/apk/debug/app-debug.apk
DUMP_XML=/sdcard/window_dump.xml
PASS=0
FAIL=0
MANUAL=0
declare -a RESULTS

mkdir -p "$OUT"

say() { printf '\n=== %s\n' "$*"; }
shot() { adb exec-out screencap -p > "$OUT/$1.png" && echo "screenshot: $OUT/$1.png"; }
record() { # record PASS|FAIL|MANUAL "note"
  RESULTS+=("$1: $2")
  case "$1" in
    PASS) PASS=$((PASS + 1)) ;;
    FAIL) FAIL=$((FAIL + 1)) ;;
    MANUAL) MANUAL=$((MANUAL + 1)) ;;
  esac
  printf '%s: %s\n' "$1" "$2"
}
confirm() { # confirm "prompt" -> waits for Enter
  read -r -p "$1 [press Enter] " _
}

dump_ui() {
  adb shell uiautomator dump "$DUMP_XML" >/dev/null 2>&1 || true
  adb exec-out cat "$DUMP_XML" 2>/dev/null
}

has_text() { dump_ui | grep -q "text=\"$1\"" ; }

tap_text() { # tap_text "label" [occurrence-from-1]
  local xml n="${2:-1}"
  xml=$(dump_ui)
  echo "$xml" | grep -o 'text="'"$1"'"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' \
    | sed -n "${n}p" | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | {
    read -r bounds || exit 1
    local l t r b
    l=$(echo "$bounds" | sed -E 's/bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\1/')
    t=$(echo "$bounds" | sed -E 's/bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\2/')
    r=$(echo "$bounds" | sed -E 's/bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\3/')
    b=$(echo "$bounds" | sed -E 's/bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\4/')
    adb shell input tap $(( (l + r) / 2 )) $(( (t + b) / 2 ))
  }
}

tap_desc() { # tap by contentDescription, e.g. "Open queue"
  local xml
  xml=$(dump_ui)
  echo "$xml" | grep -o 'content-desc="'"$1"'"[^>]*bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' \
    | head -1 | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | {
    read -r bounds || { echo "no node: $1" >&2; return 1; }
    local l t r b
    l=$(echo "$bounds" | sed -E 's/.*"\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\1/')
    t=$(echo "$bounds" | sed -E 's/.*"\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\2/')
    r=$(echo "$bounds" | sed -E 's/.*"\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\3/')
    b=$(echo "$bounds" | sed -E 's/.*"\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]"/\4/')
    adb shell input tap $(( (l + r) / 2 )) $(( (t + b) / 2 ))
  }
}

swipe_card() { # swipe_card right|left|left-short
  case "$1" in
    right)      adb shell input swipe 320 1150 1000 1150 120 ;;
    left)       adb shell input swipe 760 1150 100 1150 120 ;;
    left-short) adb shell input swipe 540 1150 600 1150 700 ;;
  esac
}

allow_dialog() { # tap "Allow" on a system permission dialog
  sleep 1
  if has_text "Allow"; then tap_text "Allow"; sleep 1; fi
}

# --- slot-table expectation (mirror of domain NagSchedule, for verification) ---
slots_for_dow() { # 0=Sun..6=Sat -> "minute:level minute:level ..."
  local dow="$1"
  if [ "$dow" = 0 ] || [ "$dow" = 6 ]; then
    echo "600:gentle 720:gentle 840:frequent 960:frequent 1080:frequent 1200:frequent 1260:last-chance 1290:last-chance"
  else
    echo "1080:gentle 1140:frequent 1200:frequent 1260:last-chance 1290:last-chance"
  fi
}

device_field() { device_field_out=$(adb shell "$1" | tr -d '\r'); }

expected_next_slot() { # echoes "YYYY-MM-DD HH:MM level" for the DEVICE clock
  local now_date now_min now_dow d_min d_dow s best="" best_min=99999 level
  device_field "date '+%F'"; now_date=$device_field_out
  device_field "date '+%-H'"; local h=$device_field_out
  device_field "date '+%-M'"; local m=$device_field_out
  device_field "date '+%w'"; now_dow=$device_field_out
  now_min=$((10#$h * 60 + 10#$m))
  d_min=$now_min; d_dow=$now_dow
  for _ in 1 2; do
    for s in $(slots_for_dow "$d_dow"); do
      local sm="${s%%:*}" lv="${s##*:}"
      if [ "$d_min" -lt "$sm" ] && [ "$sm" -lt "$best_min" ]; then best_min=$sm; best="$sm"; level=$lv; fi
    done
    [ -n "$best" ] && break
    d_min=-1; d_dow=$(( (d_dow + 1) % 7 )); best_min=99999
  done
  local hhmm
  hhmm=$(printf '%02d:%02d' $((best_min / 60)) $((best_min % 60)))
  if [ "$d_dow" = "$now_dow" ]; then
    echo "$now_date $hhmm $level"
  else
    # tomorrow in the device's timezone: device epoch + 24h, formatted on the host
    device_field "date '+%s'"; local epoch_s=$device_field_out
    echo "$(date -d "@$((epoch_s + 86400))" '+%F') $hhmm $level"
  fi
}

armed_alarm_wallclock() { # first RTC_WAKEUP epoch-ms for dev.nag from dumpsys alarm
  adb shell dumpsys alarm | grep -A1 "$PKG" | grep -oE 'when[Ea-z]*[=: ]+[0-9]{12,}' \
    | head -1 | grep -oE '[0-9]{12,}'
}

check_armed_matches_table() { # check_armed_matches_table "note"
  local note="$1" want got epoch_ms device_tz
  want=$(expected_next_slot)
  device_field "getprop persist.sys.timezone"; device_tz=$device_field_out
  epoch_ms=$(armed_alarm_wallclock)
  if [ -z "$epoch_ms" ]; then
    adb shell dumpsys alarm | grep -i "$PKG" > "$OUT/alarm-$(date +%s).txt"
    record MANUAL "$note — could not parse alarm time; raw grep saved. Expected next slot: $want"
    return
  fi
  # the armed epoch is the device's wall clock — compare in the device's timezone
  got=$(TZ="$device_tz" date -d "@$((epoch_ms / 1000))" '+%F %H:%M')
  if [ "$got" = "$(echo "$want" | cut -d' ' -f1-2)" ]; then
    record PASS "$note — armed $got ($device_tz) matches slot table (${want##* })"
  else
    record FAIL "$note — armed $got ($device_tz), expected $want"
  fi
}

launch_app() {
  adb shell am start -n "$PKG/.MainActivity" >/dev/null
  sleep 2
}

posted_notification_text() { # latest android.text for a dev.nag notification
  adb shell dumpsys notification --noredact | grep -oE 'android\.text=[^ ]*(.*)' | tail -5
}

no_fatal() { # true if no FATAL EXCEPTION for our process since mark
  adb logcat -d | grep -E "FATAL EXCEPTION.*($PKG|nag)" >/dev/null && return 1 || return 0
}

# ---------------------------------------------------------------- steps ----

step1_install_first_launch() {
  say "1. adb install -r; first launch shows empty deck + POST_NOTIFICATIONS prompt"
  adb uninstall "$PKG" >/dev/null 2>&1 || true
  adb install -r "$APK" || { record FAIL "install failed"; return; }
  launch_app
  confirm "Handle the battery-exemption dialog if shown (Allow) — accept, then press Enter"
  sleep 1
  shot 01-first-launch
  if has_text "POST_NOTIFICATIONS" || has_text "send you notifications" || has_text "Allow nag"; then
    confirm "POST_NOTIFICATIONS prompt is visible on screen — press Enter after tapping Allow"
    record PASS "item 1 — permission prompt shown on first launch"
  else
    record FAIL "item 1 — POST_NOTIFICATIONS prompt not found in UI dump"
  fi
  if has_text "Nothing due." && has_text "day streak"; then
    record PASS "item 1 — empty deck state (streak + 'Nothing due.') shown"
  else
    record FAIL "item 1 — empty deck state not found"
  fi
}

step2_add_chore() {
  say "2. Add a chore via the queue; it appears in today's deck immediately"
  tap_desc "Open queue"; sleep 1
  has_text "Queue" || { record FAIL "item 2 — queue screen not open"; return; }
  tap_text "Name"; sleep 1
  adb shell input text "dishes"
  shot 02-queue-filled
  tap_text "Add"; sleep 1
  tap_desc "Back to deck"; sleep 1
  shot 02-deck-with-card
  if has_text "dishes"; then
    record PASS "item 2 — 'dishes' due immediately in deck"
  else
    record FAIL "item 2 — 'dishes' not in deck after add"
  fi
}

step3_swipe_right() {
  say "3. Swipe right completes; deck empties and shows streak = 1"
  swipe_card right; sleep 2
  shot 03-after-complete
  if has_text "Nothing due." && has_text "day streak"; then
    local n
    n=$(dump_ui | grep -o 'text="[0-9]*"' | head -1 | grep -o '[0-9]*')
    if [ "$n" = "1" ]; then
      record PASS "item 3 — completed; empty deck shows streak 1"
    else
      record FAIL "item 3 — streak shows '$n', expected 1"
    fi
  else
    record FAIL "item 3 — deck did not empty after swipe right"
  fi
}

step4_discard_budget() {
  say "4. Two left-swipes discard; third springs back with 'No discards left'"
  tap_desc "Open queue"; sleep 1
  for chore in laundry bins; do
    tap_text "Name"; sleep 1
    adb shell input text "$chore"
    tap_text "Add"; sleep 1
  done
  tap_desc "Back to deck"; sleep 1
  swipe_card left; sleep 2   # 1 of 2
  swipe_card left; sleep 2   # 2 of 2
  shot 04-before-third
  swipe_card left; sleep 2   # 3rd must spring back + snackbar
  shot 04-after-third
  if has_text "No discards left"; then
    record PASS "item 4 — third left rejected with 'No discards left'"
  else
    record FAIL "item 4 — snackbar 'No discards left' not found"
  fi
  if has_text "laundry" || has_text "bins"; then
    record PASS "item 4 — card still on deck (spring-back)"
  else
    record FAIL "item 4 — no card remains after rejected swipe"
  fi
}

step5_alarm_matches_table() {
  say "5. dumpsys alarm shows next slot matching today's table"
  adb shell dumpsys alarm | grep -i nag | tee "$OUT/05-dumpsys-alarm.txt"
  check_armed_matches_table "item 5"
}

step6_notification_fires() {
  say "6. At the slot time the notification arrives with pinned copy; tap opens deck"
  local want
  want=$(expected_next_slot)
  echo "Expected next slot: $want"
  confirm "Wait for the slot time ($(echo "$want" | cut -d' ' -f2)) — when the notification shows, press Enter"
  shot 06-notification
  adb shell dumpsys notification --noredact | grep -B3 -A12 "$PKG" > "$OUT/06-notification.txt" || true
  if adb shell dumpsys notification --noredact | grep -q "android.title=nag"; then
    record PASS "item 6 — notification 'nag' posted (copy in $OUT/06-notification.txt)"
  else
    record MANUAL "item 6 — check $OUT/06-notification.txt for the posted record"
  fi
  confirm "Tap the notification — deck should open. Press Enter when deck is visible"
  shot 06-tap-opens-deck
  if has_text "dishes" || has_text "laundry" || has_text "bins"; then
    record PASS "item 6 — tapping notification opened the deck"
  else
    record FAIL "item 6 — deck not visible after notification tap"
  fi
}

step7_completion_silences_day() {
  say "7. Complete once; further nags that day do not appear"
  swipe_card right; sleep 2
  shot 07-completed
  adb logcat -c
  echo "Now wait for the next slot to pass (it must not post)."
  confirm "After the next slot time has passed, press Enter"
  adb shell dumpsys notification --noredact | grep -q "android.title=nag" \
    && record FAIL "item 7 — a nag posted after completion" \
    || record PASS "item 7 — no nag posted after completion"
  no_fatal && record PASS "item 7 — no crash after skip path" || record FAIL "item 7 — crash in logcat"
  adb shell dumpsys alarm | grep -i nag > "$OUT/07-alarm-after-skip.txt" || true
  check_armed_matches_table "item 7 (rescheduled after skip)"
}

step8_reboot() {
  say "8. adb reboot; BOOT_COMPLETED re-arms the next slot"
  adb reboot
  adb wait-for-device
  until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
  sleep 5
  confirm "Unlock the phone if it is locked, then press Enter"
  adb shell dumpsys alarm | grep -i nag > "$OUT/08-alarm-after-reboot.txt" || true
  check_armed_matches_table "item 8 (after reboot)"
}

step9_update_survival() {
  say "9. adb install -r of rebuilt APK; alarm re-armed and data survives"
  (./gradlew assembleDebug --console=plain -q >/dev/null 2>&1)
  adb install -r "$APK" >/dev/null
  launch_app
  allow_dialog
  sleep 2
  tap_desc "Open queue"; sleep 1
  local data_ok=1
  has_text "dishes" || has_text "laundry" || has_text "bins" || data_ok=0
  shot 09-queue-after-update
  tap_desc "Back to deck" 2>/dev/null; sleep 1
  adb shell dumpsys alarm | grep -i nag > "$OUT/09-alarm-after-update.txt" || true
  local after
  after=$(adb shell dumpsys alarm | grep -i "$PKG" | head -1)
  [ "$data_ok" = 1 ] && record PASS "item 9 — Room data survived update (chores still listed)" \
                    || record FAIL "item 9 — chores missing after update"
  [ -n "$after" ] && record PASS "item 9 — alarm present after MY_PACKAGE_REPLACED" \
                  || record FAIL "item 9 — no alarm armed after update"
  check_armed_matches_table "item 9 (re-armed)"
}

step10_clock_timezone() {
  say "10. Timezone change recomputes next slot to new local time"
  local tz_before
  tz_before=$(adb shell getprop persist.sys.timezone | tr -d '\r')
  adb shell cmd alarm set-timezone Pacific/Auckland 2>/dev/null || {
    confirm "cmd alarm failed — change timezone in Settings manually, then press Enter"
  }
  sleep 2
  echo "Device timezone now: $(adb shell getprop persist.sys.timezone)"
  adb shell dumpsys alarm | grep -i nag > "$OUT/10-alarm-after-tz.txt" || true
  check_armed_matches_table "item 10 (timezone changed, re-armed)"
  confirm "Also observe: no late post of the missed slot — press Enter"
  adb shell cmd alarm set-timezone "$tz_before" 2>/dev/null || true
  sleep 2
  check_armed_matches_table "item 10 (timezone restored)"
}

step11_revoke_notifications() {
  say "11. Deny notifications; no crash and nothing posts"
  adb shell pm revoke "$PKG" android.permission.POST_NOTIFICATIONS
  adb logcat -c
  adb shell am force-stop "$PKG"
  launch_app
  sleep 3
  if adb shell pidof "$PKG" >/dev/null; then
    record PASS "item 11 — process alive with permission revoked"
  else
    record FAIL "item 11 — process not running after revoke"
  fi
  no_fatal && record PASS "item 11 — no crash in logcat" || record FAIL "item 11 — crash in logcat"
  shot 11-revoked
}

step12_battery_whitelist() {
  say "12. Battery exemption accepted; package on deviceidle whitelist"
  if adb shell dumpsys deviceidle whitelist | grep -q "$PKG"; then
    record PASS "item 12 — $PKG in deviceidle whitelist"
  else
    confirm "Not whitelisted — accept the exemption (relaunch app) or set Unrestricted, then press Enter"
    adb shell dumpsys deviceidle whitelist | grep -q "$PKG" \
      && record PASS "item 12 — whitelisted after manual step" \
      || record FAIL "item 12 — not on whitelist"
  fi
  adb shell dumpsys deviceidle whitelist | grep "$PKG" > "$OUT/12-whitelist.txt" || true
}

# ------------------------------------------------------------------ main ----

adb get-serialno >/dev/null 2>&1 || { echo "no device attached" >&2; exit 1; }
echo "Device: $(adb shell getprop ro.product.model | tr -d '\r') (Android $(adb shell getprop ro.build.version.release | tr -d '\r'))"
echo "Evidence directory: $OUT"

STEPS=(step1_install_first_launch step2_add_chore step3_swipe_right step4_discard_budget
  step5_alarm_matches_table step6_notification_fires step7_completion_silences_day
  step8_reboot step9_update_survival step10_clock_timezone step11_revoke_notifications
  step12_battery_whitelist)

if [ $# -gt 0 ]; then
  STEPS=("$@")
fi

for s in "${STEPS[@]}"; do
  "$s"
done

printf '\n===== SUMMARY =====\n'
printf '%s\n' "${RESULTS[@]}"
printf 'PASS=%d FAIL=%d MANUAL=%d\n' "$PASS" "$FAIL" "$MANUAL"
[ "$FAIL" = 0 ]
