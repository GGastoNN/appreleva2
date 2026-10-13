# Revisión de entrega v1.3.6

## Cambios verificados estáticamente

- `applicationId` y `namespace`: `com.grupoidea.relevapp`.
- Código Kotlin movido al paquete `com.grupoidea.relevapp`.
- Logo oficial de Grupo IDEA incluido como `grupoidea_brand_logo.png`.
- La configuración editable de marca fue retirada del ViewModel y de la interfaz.
- `Marca corporativa` queda como vista informativa de solo lectura.
- El sello de fotografías usa la identidad fija cargada desde el recurso del APK.
- El workflow `Android APK` continúa generando `app-debug.apk`.
- Se conserva `RELEVAMETAL_PROJECT_ARCHIVE` / formato 1 para compatibilidad `.gidea`.
- XML y estructura de recursos validados estáticamente.

## Limitación de esta revisión

Este entorno no dispone del Android SDK/Gradle completo para ejecutar `assembleDebug`. La compilación real debe confirmarse con el workflow de GitHub Actions incluido en el proyecto.

## Migración por cambio de package

Al pasar de `com.illu.relevametal` a `com.grupoidea.relevapp`, Android instala esta versión como una aplicación nueva. Para trasladar relevamientos existentes, exportar `.gidea` desde la app anterior e importarlo en esta versión.
