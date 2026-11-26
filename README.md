# Sguardo для Android

Нативное приложение на Kotlin + Jetpack Compose (Material 3). Бэкенд: https://sguardo.ru (API `/api/...`, сессия — кука `sid`).

- Сборка только в GitHub Actions (`.github/workflows/build.yml`): debug APK и снимки экранов Roborazzi в демо-режиме.
- `app/google-services.json` в git не лежит: в CI его кладёт секрет `GOOGLE_SERVICES_JSON` (base64).
- Демо-режим: кнопка «Посмотреть демо» на экране входа, только в debug-сборке.
