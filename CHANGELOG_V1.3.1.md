# Grupo IDEA - Relevamientos 1.3.1

Versión de saneamiento técnico para presentación interna.

- Eliminados del árbol activo los restos de la etapa empresarial/nube: servidor, sincronización, usuarios, roles, WorkManager y pantallas asociadas.
- Eliminadas pantallas y componentes duplicados que coexistían con `Screens.kt` y provocaban `Conflicting overloads`.
- Eliminados tests y utilidades correspondientes a módulos que ya no forman parte de la aplicación local.
- La app queda enfocada exclusivamente en relevamiento local: obras, sectores, vanos, fotos, cotas, bitácora, PDF, personalización y archivos `.gidea`.
- Se conserva `applicationId`, nombre de base Room y firma interna del formato histórico para mantener compatibilidad con instalaciones y archivos existentes.
- `versionCode` 15 / `versionName` 1.3.1.
