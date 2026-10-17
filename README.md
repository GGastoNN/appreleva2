# Grupo IDEA - Relevamientos — V1.4.3 gestión y PDF fotográfico

> **Identidad exclusiva:** esta compilación utiliza el paquete `com.grupoidea.relevapp` y una marca Grupo IDEA fija incluida dentro del APK. El logo no puede modificarse desde la aplicación.

Aplicación Android nativa, offline-first, para relevamientos diarios de obra orientados a carpintería metálica.

## Flujo de campo

1. Abrir obra.
2. Entrar al sector.
3. Crear o duplicar un vano.
4. Cargar medidas y controles técnicos.
5. Tomar fotografías.
6. Clasificar la evidencia: Inicial / Incidencia / Corrección / Final.
7. Girar la foto si hace falta.
8. Agregar cotas, flechas, rectángulos, círculos o texto directamente sobre la imagen.
9. Registrar observaciones o eventos de obra cuando sea necesario.
10. Compartir una foto técnica individual o generar el PDF completo de obra.

## Marca corporativa

La identidad de esta compilación es exclusiva de **Grupo IDEA**. El logo oficial está incluido dentro del APK y se utiliza automáticamente en la vista de marca y en los sellos de fotografías.

Desde `Marca corporativa` se puede consultar la identidad aplicada, pero no editarla. No existe selector de logo ni cambio de nombre de empresa: la identidad de Grupo IDEA permanece fija. La marca de agua de los PDF es obligatoria y usa siempre el logo oficial. Desde `Personalización` solo se ajustan su transparencia y si se dibuja al fondo o al frente del contenido.

El archivo fotográfico original se conserva sin modificar. La marca y las anotaciones se renderizan únicamente en la visualización, al compartir la foto y al generar los documentos correspondientes.

## Herramientas incluidas

- Dashboard de obras con búsqueda y filtros.
- Obra → espacio/sector → vano → evidencias.
- Código de vano sugerido automáticamente.
- Duplicado rápido de vanos repetitivos.
- Estados Pendiente / Verificar / Relevado / Aprobado.
- Ancho, alto, antepecho, diagonales, profundidad, espesor y holguras.
- Checklist de plomo, nivel, escuadra, piso, revoque y premarco.
- Interferencias, sentido de apertura y observaciones.
- Cámara CameraX con cuadrícula y flash.
- Detector offline de candidatos de vano.
- Editor de evidencias con giro no destructivo.
- Cotas manuales con valor real.
- Flechas, rectángulos, círculos y notas de texto sobre fotografía.
- Foto principal por vano.
- Clasificación cronológica de fotos por etapa.
- Eventos y observaciones vinculados al vano y a la bitácora.
- Bitácora automática y manual.
- Notas de audio generales por obra.
- Notas de audio compartidas por tipología de vano dentro de cada obra.
- Reproducción y eliminación de notas de voz directamente desde la app.
- Compartir JPG técnico con marca y anotaciones.
- Fotos generales del edificio obligatorias para generar el PDF; se muestran en la portada.
- PDF compacto: una ficha por vano con medidas y la foto seleccionada en la misma hoja.
- Cada hoja del PDF incorpora el logo oficial de IDEA S.A. como marca de agua; desde Personalización se puede ajustar su transparencia y elegir si se dibuja al fondo o al frente del contenido.
- Antes de exportar se puede elegir qué foto incluir en cada vano; por defecto se selecciona la última tomada.
- También se pueden incluir todas las fotos de un vano: la más reciente queda en la ficha y todas las restantes se agrupan en una única hoja compacta para reducir el peso del PDF.
- Desde el menú de una obra se pueden compartir todas sus fotografías ya procesadas con cotas, anotaciones y marca corporativa.
- Se puede omitir la foto de un vano para reducir todavía más el peso del archivo.
- Eliminación con confirmación de obras, espacios y vanos; las fotos técnicas también pueden eliminarse directamente desde la galería del vano o desde su editor.
- Funcionamiento offline para relevamiento, fotos, base local y PDF.

## Flujo rápido

La ficha de vano incorpora un modo de trabajo rápido pensado para recorridos repetitivos en obra: muestra un progreso de cuatro pasos esenciales, permite avanzar al vano anterior/siguiente y dispone de `Guardar y siguiente` fijo en pantalla. El listado de vanos ahora tiene búsqueda, filtros por estado, indicadores de controles y una barra de avance del sector. El modo completo sigue disponible con un interruptor para acceder a identificación, holguras y condiciones técnicas avanzadas.

## Medición fotográfica

La detección automática ubica candidatos visuales de vano, pero no convierte por sí sola píxeles en milímetros confiables. Las cotas ingresadas por el operario son la referencia documental. Una etapa posterior puede sumar calibración por referencia conocida y ARCore/Depth en dispositivos compatibles.

## Build

- Kotlin 2.0.21
- Jetpack Compose / Material 3
- Room 2.6.1
- CameraX 1.4.1
- compileSdk / targetSdk 35
- Java 17
- versionCode 27
- versionName 1.4.3

## Transferencia entre dispositivos (v1.0)

La aplicación puede exportar una obra o todas las obras a un archivo `*.gidea` e importarlo en otro dispositivo con la misma aplicación. El respaldo contiene la estructura completa del relevamiento, fotografías originales, cotas/anotaciones y notas de audio. Las obras importadas se agregan como nuevas, sin sobrescribir las existentes.


## Personalización

Desde **Ajustes → Personalización** se puede elegir tema del sistema, claro u oscuro, cambiar el color principal de la interfaz y configurar los colores de cotas, flechas/figuras, textos, marcadores, detección automática y sello sobre fotografías. También se puede ajustar el espesor de las líneas, el tamaño de los textos y la **marca de agua de los PDF**: la marca permanece siempre activa, pero se puede regular su transparencia y elegir si queda al fondo o al frente del contenido mediante opciones visibles sin desplazamiento horizontal. Los estilos de cotas, marcas, textos y sello se aplican al editor, a las imágenes compartidas y a las fotografías renderizadas dentro del PDF; la detección automática mantiene su color configurable como ayuda visual del editor. Nada de esto modifica los JPG originales ni los datos geométricos de las cotas.


## Enfoque actual

La versión 1.4.3 funciona de forma autónoma y offline. No utiliza servicios de nube, usuarios ni sincronización remota. La persistencia necesaria para que los relevamientos no se pierdan se mantiene localmente en el dispositivo, y el intercambio/backup se realiza mediante archivos `.gidea`.
