# Revisión de build v1.4.0

Controles estáticos realizados:

- Preferencias nuevas de marca de agua persistidas en `SharedPreferences`.
- Valores normalizados: opacidad 5 % a 80 %.
- Dos capas de render: fondo al iniciar página y frente al finalizar página.
- Opción de desactivar la marca sin afectar la identidad fija de Grupo IDEA.
- `ReportPdf` recibe la personalización ya existente desde `AppViewModel`.
- Sin migración Room y sin cambios en `.gidea`.
- Recursos XML y estructura de paquete conservados.

La compilación Android real debe validarse con GitHub Actions (`:app:assembleDebug`).
