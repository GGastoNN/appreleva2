# Revisión de entrega v1.3.7

## Cambio principal

Se agregó la sección **Sobre esta app** al menú principal de la aplicación.

La pantalla muestra:

- Nombre: Grupo IDEA - Relevamientos.
- Empresa: IDEA S.A.
- Plataforma: Android.
- Paquete instalado, leído desde Android en tiempo de ejecución.
- N° de versión y código de versión, leídos desde el paquete instalado.
- Funcionamiento local/offline.
- Formato de intercambio `.gidea`.
- Resumen funcional de la aplicación.

## Compatibilidad

No se modificó el esquema de Room ni el formato `.gidea`. Se mantiene el paquete `com.grupoidea.relevapp` y la marca corporativa fija.

## Build

- versionCode: 21
- versionName: 1.3.7
- compileSdk / targetSdk: 35
- Java: 17
- Workflow: `.github/workflows/android.yml`
