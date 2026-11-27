#!/usr/bin/env bash
# Дымовая проверка релизной (R8) сборки на эмуляторе: приложение запускается и не падает.
set -u
APK=app/build/outputs/apk/release/app-release.apk
adb install -r "$APK" || exit 1
adb logcat -c
adb shell am start -W -n ru.sguardo.app/.MainActivity
sleep 25
adb shell screencap -p /sdcard/smoke1.png && adb pull /sdcard/smoke1.png smoke/1-start.png
# «Позже» и «Не сейчас» на экранах первого запуска: доходим до экрана входа
adb shell input keyevent KEYCODE_BACK >/dev/null 2>&1 || true
adb shell am start -W -n ru.sguardo.app/.MainActivity
sleep 8
adb shell screencap -p /sdcard/smoke2.png && adb pull /sdcard/smoke2.png smoke/2-again.png
# FCM: ждём до 60 с токен (нужны сервисы Google Play на образе google_apis)
for i in $(seq 1 12); do
  adb logcat -d -s SGPUSH:V > smoke/push.txt
  grep -q "token ok" smoke/push.txt && break
  grep -q "SGPUSH" smoke/push.txt && grep -qv "token ok" smoke/push.txt && [ $i -ge 6 ] && break
  sleep 5
done
echo "--- результат push ---"; cat smoke/push.txt
adb logcat -d > smoke/logcat.txt
if grep -nE "FATAL EXCEPTION|AndroidRuntime.*ru\.sguardo\.app" smoke/logcat.txt; then echo "Приложение упало"; exit 1; fi
adb shell pidof ru.sguardo.app || { echo "Процесс не жив"; exit 1; }
echo "Релиз запускается без падений"
