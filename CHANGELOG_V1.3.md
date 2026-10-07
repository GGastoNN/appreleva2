# Grupo IDEA - Relevamientos 1.3.0

Versión de pulido visual y experiencia de uso pensada para presentación interna y trabajo de campo.

## Cambios visibles

- Paleta visual alineada al nuevo logo azul de Grupo IDEA.
- Inicio más ejecutivo, con resumen de obras y mensaje de funcionamiento 100% local.
- Estado vacío de bienvenida para una primera apertura más cuidada.
- Tarjetas de obra con jerarquía visual, indicador lateral por estado y acceso claro.
- Resumen de obra reorganizado en una cuadrícula 2x2, evitando desplazamiento horizontal innecesario.
- Tarjetas de vano con código destacado, medidas, controles y estado más legibles.
- Menú superior más limpio y acceso a transferencia renombrado como "Archivos".
- Diálogo de importación/exportación presentado explícitamente como archivos `.gidea`.
- Cámara reorganizada para evitar superposición entre ayuda y controles; botones sobre fondo oscuro y etiqueta de captura.
- Estados vacíos más consistentes con la identidad visual.

## Arquitectura

- La app continúa funcionando offline.
- No se agregaron servicios de nube, cuentas, login ni sincronización remota.
- Se conserva el almacenamiento local del dispositivo para no perder relevamientos al cerrar la app.
- Se mantiene compatibilidad con archivos `.gidea` y con el formato de datos de versiones anteriores.
