# Revisión técnica — V1.3.1

Esta versión parte del árbol local V1.3 y elimina archivos residuales de la rama empresarial que seguían compilándose en GitHub Actions.

## Alcance activo

- Android / Kotlin / Compose
- Room local
- CameraX
- Fotos y anotaciones no destructivas
- PDF
- Personalización
- Importación/exportación `.gidea`

## Fuera de alcance

No se incluyen servidor, sincronización remota, usuarios, autenticación, roles, aprobaciones empresariales, WorkManager de sync ni API.

La compatibilidad histórica se conserva manteniendo los identificadores internos `com.illu.relevametal`, `relevametal.db` y `RELEVAMETAL_PROJECT_ARCHIVE`; no son nombres visibles del producto.
