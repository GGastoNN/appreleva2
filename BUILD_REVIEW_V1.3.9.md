# Revisión de entrega v1.3.9

Cambio principal: protección visual de los informes PDF con marca de agua corporativa fija.

- Todas las páginas pasan por el mismo cierre de página en `ReportPdf`, que dibuja la marca antes de finalizarla.
- Recurso: `drawable-nodpi/grupoidea_watermark.png`, derivado del logo corporativo fijo con fondo transparente.
- Opacidad aproximada: 13 %.
- La imagen conserva su proporción y se centra en la página.
- No se agregaron opciones de usuario para quitar ni sustituir la marca.
- No se modificó la base Room ni la estructura `.gidea`.

Versión: 1.3.9
Version code: 23
Paquete: com.grupoidea.relevapp
