# Revisión de build v1.3.4

Se agregó soporte local para notas de audio generales y por tipología.

Cambios estructurales principales:

- permiso `RECORD_AUDIO`;
- grabación M4A/AAC mediante `MediaRecorder`;
- reproducción local mediante `MediaPlayer`;
- entidad Room `AudioNoteEntity` y migración 5 → 6;
- exportación/importación de audios dentro de `.gidea`;
- nueva pestaña Audio en Obra y sección de audio por tipología en cada vano.

Se realizaron comprobaciones estáticas de estructura, referencias y contenido ZIP. Este entorno no incluye el Android SDK/Gradle completo del proyecto, por lo que la validación final debe realizarse con `:app:assembleDebug` en GitHub Actions o Android Studio.
