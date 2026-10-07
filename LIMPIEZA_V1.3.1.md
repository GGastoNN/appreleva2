# Limpieza técnica V1.3.1

El árbol fue reducido a la aplicación Android local que se presenta internamente.

Se eliminaron del código activo los restos de la etapa empresarial experimental: servidor, sincronización, usuarios, roles, WorkManager, API, pantallas empresariales, plantillas administrativas y componentes Compose duplicados.

La estructura activa queda limitada a:

- aplicación Android;
- almacenamiento Room local;
- cámara y fotos;
- cotas/anotaciones;
- marca y personalización;
- generación de PDF;
- importación/exportación `.gidea`.

Se mantienen por compatibilidad algunos identificadores internos históricos (`com.illu.relevametal`, `relevametal.db` y `RELEVAMETAL_PROJECT_ARCHIVE`). No aparecen como nombre comercial de la aplicación.
