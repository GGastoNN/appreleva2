# Revisión de entrega v1.3.8

Correcciones basadas en el job fallido de GitHub Actions `38400015394 / 115304918948`:

1. El repositorio contenía simultáneamente el paquete nuevo `com.grupoidea.relevapp` y el paquete histórico `com.illu.relevametal`. Este ZIP solo contiene el paquete nuevo.
2. `Screens.kt` llamaba a `vm.updatePersonalization(draft)` pero el método no estaba en `AppViewModel`. Se restauró y normaliza/persiste el ajuste antes de actualizar el `StateFlow`.

Para subir esta versión al repositorio, conviene borrar primero `app/src/main/java/com/illu/relevametal` si todavía existe en GitHub. Copiar archivos encima sin borrar esa carpeta vuelve a introducir el error de compilación.

Versión: 1.3.8
Version code: 22
Paquete: com.grupoidea.relevapp
