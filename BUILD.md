# Build Android — Grupo IDEA - Relevamientos 1.3.1

## GitHub Actions

El workflow `.github/workflows/android.yml` prepara Java 17, Android SDK 35 y Gradle 8.9, ejecuta `:app:assembleDebug` y publica `app-debug.apk` como artefacto.

## Android Studio

Abrir la carpeta raíz del proyecto, usar JDK 17 y sincronizar Gradle. El módulo de aplicación es `app` y utiliza `compileSdk 35`.

## Alcance

Esta rama no requiere servidor ni variables de entorno. La aplicación trabaja únicamente con persistencia local y archivos `.gidea`.
