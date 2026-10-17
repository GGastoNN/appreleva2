package com.grupoidea.relevapp.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grupoidea.relevapp.R
import com.grupoidea.relevapp.branding.BrandingStore
import com.grupoidea.relevapp.data.*
import com.grupoidea.relevapp.detection.OpeningDetector
import com.grupoidea.relevapp.pdf.ReportPdf
import com.grupoidea.relevapp.personalization.PersonalizationSettings
import com.grupoidea.relevapp.personalization.PdfWatermarkLayer
import com.grupoidea.relevapp.personalization.PersonalizationStore
import com.grupoidea.relevapp.render.PhotoRenderer
import com.grupoidea.relevapp.transfer.ProjectArchiveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

data class ProjectStats(
    val spaces: Int = 0,
    val openings: Int = 0,
    val pending: Int = 0,
    val photos: Int = 0
)


data class PdfOpeningPhotoChoice(
    val openingId: Long,
    val openingCode: String,
    val spaceName: String,
    val photos: List<EvidenceEntity>
) {
    val defaultEvidenceId: Long?
        get() = photos.maxByOrNull { it.createdAt }?.id
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val db = AppDatabase.get(app)
    private val detector = OpeningDetector()
    private val brandingStore = BrandingStore(app)
    private val personalizationStore = PersonalizationStore(app)
    private val archiveManager = ProjectArchiveManager(app, db)
    val branding = MutableStateFlow(brandingStore.load())
    val personalization = MutableStateFlow(personalizationStore.load())

    val projects = db.projects()
        .observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun spaces(projectId: Long) = db.spaces().observe(projectId)
    fun openings(spaceId: Long) = db.openings().observe(spaceId)
    fun evidence(openingId: Long) = db.evidence().observe(openingId)
    fun events(projectId: Long) = db.events().observe(projectId)
    fun projectReferencePhotos(projectId: Long) = db.projectReferencePhotos().observe(projectId)
    fun projectAudioNotes(projectId: Long) = db.audioNotes().observeProject(projectId)
    fun typologyAudioNotes(projectId: Long, typology: String) =
        db.audioNotes().observeTypology(projectId, typology.trim().uppercase(Locale.ROOT))

    fun projectStats(projectId: Long): Flow<ProjectStats> = combine(
        db.spaces().count(projectId),
        db.openings().countForProject(projectId),
        db.openings().pendingForProject(projectId),
        db.evidence().countForProject(projectId)
    ) { spaces, openings, pending, photos ->
        ProjectStats(spaces, openings, pending, photos)
    }

    suspend fun project(id: Long) = db.projects().project(id)
    suspend fun space(id: Long) = db.spaces().get(id)
    suspend fun opening(id: Long) = db.openings().get(id)
    suspend fun evidenceItem(id: Long) = db.evidence().get(id)

    fun addProjectReferencePhotos(
        projectId: Long,
        uris: List<Uri>,
        onReady: (Result<Unit>) -> Unit = {}
    ) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                require(uris.isNotEmpty()) { "Seleccioná al menos una imagen del edificio" }
                val resolver = getApplication<Application>().contentResolver
                val dir = File(getApplication<Application>().filesDir, "project_reference_photos/$projectId").apply { mkdirs() }
                uris.forEachIndexed { index, uri ->
                    val mime = resolver.getType(uri).orEmpty()
                    val extension = when {
                        mime.contains("png", ignoreCase = true) -> "png"
                        mime.contains("webp", ignoreCase = true) -> "webp"
                        mime.contains("heic", ignoreCase = true) || mime.contains("heif", ignoreCase = true) -> "heic"
                        else -> "jpg"
                    }
                    val target = File(dir, "building_${System.currentTimeMillis()}_${index}.$extension")
                    resolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "No se pudo leer una de las imágenes seleccionadas" }
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(target.absolutePath, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                        target.delete()
                        error("Una de las imágenes seleccionadas no es válida")
                    }
                    db.projectReferencePhotos().insert(
                        ProjectReferencePhotoEntity(
                            projectId = projectId,
                            filePath = target.absolutePath
                        )
                    )
                }
                touchProject(projectId)
            }
        }
        onReady(result)
    }

    fun deleteProjectReferencePhoto(projectId: Long, item: ProjectReferencePhotoEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            db.projectReferencePhotos().delete(item)
            runCatching { File(item.filePath).delete() }
            touchProject(projectId)
        }

    fun addProject(
        name: String,
        client: String,
        address: String,
        responsible: String,
        notes: String,
        onCreated: ((Long) -> Unit)? = null
    ) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val id = db.projects().insert(
            ProjectEntity(
                name = name.trim(),
                client = client.trim(),
                address = address.trim(),
                responsible = responsible.trim(),
                notes = notes.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
        db.events().insert(
            EventEntity(
                projectId = id,
                kind = "CREATED",
                title = "Obra creada",
                detail = name.trim()
            )
        )
        onCreated?.invoke(id)
    }

    fun updateProject(project: ProjectEntity) = viewModelScope.launch {
        db.projects().update(project.copy(updatedAt = System.currentTimeMillis()))
    }

    fun deleteProject(projectId: Long, onDeleted: () -> Unit = {}) =
        viewModelScope.launch(Dispatchers.IO) {
            val project = db.projects().project(projectId) ?: return@launch

            // Los registros relacionados se eliminan por CASCADE. Antes borramos los
            // archivos físicos para no dejar fotografías o audios huérfanos.
            db.projectReferencePhotos().list(projectId).forEach { photo ->
                runCatching { File(photo.filePath).delete() }
            }
            db.spaces().list(projectId).forEach { space ->
                db.openings().list(space.id).forEach { opening ->
                    db.evidence().list(opening.id).forEach { evidence ->
                        runCatching { File(evidence.filePath).delete() }
                    }
                }
            }
            db.audioNotes().listForProject(projectId).forEach { note ->
                runCatching { File(note.filePath).delete() }
            }

            db.projects().delete(project)
            runCatching {
                File(getApplication<Application>().filesDir, "project_reference_photos/$projectId").deleteRecursively()
            }
            runCatching {
                File(getApplication<Application>().filesDir, "audio_notes/project_$projectId").deleteRecursively()
            }
            withContext(Dispatchers.Main) { onDeleted() }
        }

    fun addSpace(
        projectId: Long,
        name: String,
        level: String = "",
        sector: String = "",
        notes: String = "",
        onCreated: ((Long) -> Unit)? = null
    ) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val id = db.spaces().insert(
            SpaceEntity(
                projectId = projectId,
                name = name.trim(),
                level = level.trim(),
                sector = sector.trim(),
                notes = notes.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
        touchProject(projectId)
        db.events().insert(
            EventEntity(
                projectId = projectId,
                kind = "SPACE",
                title = "Espacio agregado",
                detail = name.trim()
            )
        )
        onCreated?.invoke(id)
    }

    fun deleteSpace(projectId: Long, spaceId: Long, onDeleted: () -> Unit = {}) =
        viewModelScope.launch(Dispatchers.IO) {
            val space = db.spaces().get(spaceId) ?: return@launch
            db.openings().list(spaceId).forEach { opening ->
                db.evidence().list(opening.id).forEach { evidence ->
                    runCatching { File(evidence.filePath).delete() }
                }
            }
            db.spaces().delete(space)
            db.events().insert(
                EventEntity(
                    projectId = projectId,
                    kind = "SPACE_DELETED",
                    title = "Espacio eliminado",
                    detail = space.name
                )
            )
            touchProject(projectId)
            withContext(Dispatchers.Main) { onDeleted() }
        }

    suspend fun nextOpeningCode(spaceId: Long): String {
        val existing = db.openings().list(spaceId)
            .map { it.code.uppercase() }
            .toSet()
        var n = 1
        while (true) {
            val candidate = "V%02d".format(n)
            if (candidate !in existing) return candidate
            n++
        }
    }

    fun addOpening(
        projectId: Long,
        spaceId: Long,
        code: String,
        type: String,
        onCreated: ((Long) -> Unit)? = null
    ) = viewModelScope.launch {
        val id = db.openings().insert(
            OpeningEntity(
                spaceId = spaceId,
                code = code.trim().uppercase(),
                type = type.trim().uppercase().ifBlank { "VANO" }
            )
        )
        touchProject(projectId)
        db.events().insert(
            EventEntity(
                projectId = projectId,
                openingId = id,
                kind = "OPENING",
                title = "Vano agregado",
                detail = code.trim().uppercase()
            )
        )
        onCreated?.invoke(id)
    }


    fun duplicateOpening(
        projectId: Long,
        source: OpeningEntity,
        onCreated: (Long) -> Unit
    ) = viewModelScope.launch {
        val code = nextOpeningCode(source.spaceId)
        val id = db.openings().insert(
            source.copy(
                id = 0,
                code = code,
                status = "PENDIENTE",
                plumbState = "NO_VERIFICADO",
                levelState = "NO_VERIFICADO",
                squareState = "NO_VERIFICADO",
                floorState = "NO_VERIFICADO",
                plasterState = "NO_VERIFICADO",
                premarcoState = "NO_VERIFICADO",
                interference = "",
                notes = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        touchProject(projectId)
        db.events().insert(
            EventEntity(
                projectId = projectId,
                openingId = id,
                kind = "OPENING_DUPLICATED",
                title = "$code: vano duplicado",
                detail = "Base copiada desde ${source.code}; controles reiniciados para nueva verificación."
            )
        )
        onCreated(id)
    }

    fun addOpeningEvent(
        projectId: Long,
        openingId: Long,
        title: String,
        detail: String,
        severity: String = "ALERTA"
    ) = viewModelScope.launch {
        val code = db.openings().get(openingId)?.code ?: "Vano"
        db.events().insert(
            EventEntity(
                projectId = projectId,
                openingId = openingId,
                kind = "INCIDENCE",
                title = "$code: ${title.trim()}",
                detail = detail.trim(),
                severity = severity
            )
        )
        touchProject(projectId)
    }


    fun saveOpeningDraft(projectId: Long, opening: OpeningEntity) = viewModelScope.launch {
        val old = db.openings().get(opening.id)
        val updated = opening.copy(updatedAt = System.currentTimeMillis())
        db.openings().update(updated)
        touchProject(projectId)
        if (old != null && old.status != updated.status) {
            db.events().insert(
                EventEntity(
                    projectId = projectId,
                    openingId = updated.id,
                    kind = "STATUS",
                    title = "${updated.code}: ${updated.status}"
                )
            )
        }
    }

    fun saveOpening(projectId: Long, opening: OpeningEntity) = viewModelScope.launch {
        val old = db.openings().get(opening.id)
        val updated = opening.copy(updatedAt = System.currentTimeMillis())
        db.openings().update(updated)
        touchProject(projectId)

        val statusChanged = old != null && old.status != updated.status
        db.events().insert(
            EventEntity(
                projectId = projectId,
                openingId = updated.id,
                kind = if (statusChanged) "STATUS" else "MEASURE",
                title = if (statusChanged) {
                    "${updated.code}: ${updated.status}"
                } else {
                    "${updated.code}: relevamiento actualizado"
                },
                detail = buildString {
                    if (updated.widthMm != null && updated.heightMm != null) {
                        append("${updated.widthMm} × ${updated.heightMm} mm")
                    }
                    if (updated.notes.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(updated.notes.take(120))
                    }
                }
            )
        )
    }

    fun deleteOpening(projectId: Long, openingId: Long, onDeleted: () -> Unit = {}) =
        viewModelScope.launch(Dispatchers.IO) {
            val opening = db.openings().get(openingId) ?: return@launch
            db.evidence().list(openingId).forEach { evidence ->
                runCatching { File(evidence.filePath).delete() }
            }
            db.openings().delete(opening)
            db.events().insert(
                EventEntity(
                    projectId = projectId,
                    kind = "OPENING_DELETED",
                    title = "${opening.code}: vano eliminado",
                    detail = opening.type
                )
            )
            touchProject(projectId)
            withContext(Dispatchers.Main) { onDeleted() }
        }

    fun addEvidence(
        projectId: Long,
        openingId: Long,
        path: String,
        onReady: (Long) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > 1600 * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        val bitmap = BitmapFactory.decodeFile(path, options)
        val detected = bitmap?.let { detector.detect(it) }.orEmpty()
        val scaleX = if (bitmap != null && bitmap.width > 0) bounds.outWidth.toFloat() / bitmap.width else 1f
        val scaleY = if (bitmap != null && bitmap.height > 0) bounds.outHeight.toFloat() / bitmap.height else 1f
        val json = detected.joinToString(prefix = "[", postfix = "]") { c ->
            "{\"l\":${c.rect.left * scaleX},\"t\":${c.rect.top * scaleY},\"r\":${c.rect.right * scaleX},\"b\":${c.rect.bottom * scaleY},\"confidence\":${c.confidence}}"
        }
        bitmap?.recycle()
        val id = db.evidence().insert(
            EvidenceEntity(
                openingId = openingId,
                filePath = path,
                detectedJson = json
            )
        )
        touchProject(projectId)
        val code = db.openings().get(openingId)?.code ?: "Vano"
        db.events().insert(
            EventEntity(
                projectId = projectId,
                openingId = openingId,
                kind = "PHOTO",
                title = "$code: fotografía agregada",
                detail = if (detected.isEmpty()) {
                    "Sin detección automática"
                } else {
                    "${detected.size} candidato(s) de vano detectado(s)"
                }
            )
        )
        withContext(Dispatchers.Main) { onReady(id) }
    }

    fun saveEvidence(
        projectId: Long,
        item: EvidenceEntity,
        logChange: Boolean = false
    ) = viewModelScope.launch {
        val updated = item.copy(updatedAt = System.currentTimeMillis())
        db.evidence().update(updated)
        touchProject(projectId)
        if (logChange) {
            val code = db.openings().get(item.openingId)?.code ?: "Vano"
            db.events().insert(
                EventEntity(
                    projectId = projectId,
                    openingId = item.openingId,
                    kind = "ANNOTATION",
                    title = "$code: foto documentada",
                    detail = item.caption.take(140)
                )
            )
        }
    }

    fun setPrimaryEvidence(projectId: Long, item: EvidenceEntity) = viewModelScope.launch {
        db.evidence().clearPrimary(item.openingId)
        db.evidence().update(item.copy(isPrimary = true, updatedAt = System.currentTimeMillis()))
        touchProject(projectId)
    }

    fun deleteEvidence(projectId: Long, item: EvidenceEntity) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { File(item.filePath).delete() }
        db.evidence().delete(item)
        touchProject(projectId)
        val code = db.openings().get(item.openingId)?.code ?: "Vano"
        db.events().insert(
            EventEntity(
                projectId = projectId,
                openingId = item.openingId,
                kind = "PHOTO_DELETED",
                title = "$code: fotografía eliminada"
            )
        )
    }

    fun newAudioNoteFile(projectId: Long): File {
        val dir = File(getApplication<Application>().filesDir, "audio_notes/project_$projectId").apply { mkdirs() }
        return File(dir, "audio_${System.currentTimeMillis()}_${UUID.randomUUID()}.m4a")
    }

    fun addProjectAudioNote(
        projectId: Long,
        path: String,
        durationMs: Long
    ) = viewModelScope.launch(Dispatchers.IO) {
        val file = File(path)
        if (!file.isFile || file.length() <= 0L) return@launch
        db.audioNotes().insert(
            AudioNoteEntity(
                projectId = projectId,
                scope = "PROJECT",
                filePath = file.absolutePath,
                durationMs = durationMs
            )
        )
        db.events().insert(
            EventEntity(
                projectId = projectId,
                kind = "AUDIO",
                title = "Nota de audio general agregada",
                detail = "Duración ${formatAudioDuration(durationMs)}"
            )
        )
        touchProject(projectId)
    }

    fun addTypologyAudioNote(
        projectId: Long,
        openingId: Long,
        typology: String,
        openingCode: String,
        path: String,
        durationMs: Long
    ) = viewModelScope.launch(Dispatchers.IO) {
        val file = File(path)
        if (!file.isFile || file.length() <= 0L) return@launch
        val normalizedType = typology.trim().uppercase(Locale.ROOT).ifBlank { "VANO" }
        db.audioNotes().insert(
            AudioNoteEntity(
                projectId = projectId,
                scope = "TYPOLOGY",
                typology = normalizedType,
                openingId = openingId,
                contextLabel = openingCode.trim().uppercase(Locale.ROOT),
                filePath = file.absolutePath,
                durationMs = durationMs
            )
        )
        db.events().insert(
            EventEntity(
                projectId = projectId,
                openingId = openingId,
                kind = "AUDIO",
                title = "$normalizedType: nota de audio agregada",
                detail = listOf(openingCode.trim(), formatAudioDuration(durationMs)).filter { it.isNotBlank() }.joinToString(" · ")
            )
        )
        touchProject(projectId)
    }

    fun deleteAudioNote(item: AudioNoteEntity) = viewModelScope.launch(Dispatchers.IO) {
        db.audioNotes().delete(item)
        runCatching { File(item.filePath).delete() }
        touchProject(item.projectId)
    }

    private fun formatAudioDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(Locale.ROOT, minutes, seconds)
    }

    fun addEvent(
        projectId: Long,
        title: String,
        detail: String,
        severity: String
    ) = viewModelScope.launch {
        db.events().insert(
            EventEntity(
                projectId = projectId,
                kind = "NOTE",
                title = title.trim(),
                detail = detail.trim(),
                severity = severity
            )
        )
        touchProject(projectId)
    }

    fun loadPdfPhotoChoices(
        projectId: Long,
        onReady: (Result<List<PdfOpeningPhotoChoice>>) -> Unit
    ) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val project = db.projects().project(projectId) ?: error("Obra inexistente")
                val referencePhotos = db.projectReferencePhotos()
                    .list(project.id)
                    .filter { File(it.filePath).isFile }
                require(referencePhotos.isNotEmpty()) {
                    "Para generar el PDF agregá al menos una imagen válida del edificio en la pestaña Obra."
                }

                db.spaces().list(projectId).flatMap { space ->
                    db.openings().list(space.id).map { opening ->
                        val photos = db.evidence().list(opening.id)
                            .filter { File(it.filePath).isFile }
                            .sortedByDescending { it.createdAt }
                        PdfOpeningPhotoChoice(
                            openingId = opening.id,
                            openingCode = opening.code,
                            spaceName = space.name,
                            photos = photos
                        )
                    }
                }
            }
        }
        onReady(result)
    }

    fun exportProject(
        projectId: Long,
        selectedEvidenceIds: Map<Long, List<Long>>,
        onReady: (Result<File>) -> Unit
    ) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val project = db.projects().project(projectId) ?: error("Obra inexistente")
                val spaces = db.spaces().list(projectId).map { space ->
                    val openings = db.openings().list(space.id).map { opening ->
                        val allEvidence = db.evidence().list(opening.id)
                            .filter { File(it.filePath).isFile }
                            .sortedByDescending { it.createdAt }
                        val selectedIds = if (selectedEvidenceIds.containsKey(opening.id)) {
                            selectedEvidenceIds[opening.id].orEmpty()
                        } else {
                            listOfNotNull(allEvidence.firstOrNull()?.id)
                        }
                        val selected = selectedIds.mapNotNull { id ->
                            allEvidence.firstOrNull { it.id == id }
                        }
                        ReportPdf.OpeningBundle(opening, selected)
                    }
                    ReportPdf.SpaceBundle(space, openings)
                }
                val events = db.events().list(projectId)
                val referencePhotos = db.projectReferencePhotos()
                    .list(projectId)
                    .filter { File(it.filePath).isFile }
                require(referencePhotos.isNotEmpty()) {
                    "Para generar el PDF agregá al menos una imagen válida del edificio en la pestaña Obra."
                }
                ReportPdf(getApplication()).generate(project, spaces, events, branding.value, referencePhotos, personalization.value)
            }
        }
        onReady(result)
    }


    fun exportDataArchive(
        projectIds: List<Long>,
        onReady: (Result<File>) -> Unit
    ) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching { archiveManager.export(projectIds) }
        }
        onReady(result)
    }

    fun updatePersonalization(settings: PersonalizationSettings) {
        val normalized = personalizationStore.normalize(settings)
        personalizationStore.save(normalized)
        personalization.value = normalized
    }

    fun importDataArchive(
        uri: Uri,
        onReady: (Result<ProjectArchiveManager.ImportResult>) -> Unit
    ) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching { archiveManager.import(uri) }
        }
        onReady(result)
    }

    fun exportEvidenceImage(
        projectId: Long,
        evidence: EvidenceEntity,
        onReady: (Result<File>) -> Unit
    ) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val opening = db.openings().get(evidence.openingId) ?: error("Vano inexistente")
                val space = db.spaces().get(opening.spaceId) ?: error("Espacio inexistente")
                val project = db.projects().project(projectId) ?: error("Obra inexistente")
                val bitmap = PhotoRenderer.render(
                    evidence = evidence,
                    project = project,
                    space = space,
                    opening = opening,
                    branding = branding.value,
                    style = personalization.value,
                    maxSide = 2600
                ) ?: error("No se pudo procesar la fotografía")
                val dir = File(getApplication<Application>().filesDir, "shared_photos").apply { mkdirs() }
                val out = File(dir, "GrupoIDEA_${opening.code}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(out).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 93, stream)
                }
                bitmap.recycle()
                out
            }
        }
        onReady(result)
    }

    fun exportProjectImages(
        projectId: Long,
        onReady: (Result<List<File>>) -> Unit
    ) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val app = getApplication<Application>()
                val project = db.projects().project(projectId) ?: error("Obra inexistente")
                val brand = branding.value
                val style = personalization.value
                val outDir = File(
                    app.filesDir,
                    "shared_project_photos/project_${projectId}_${System.currentTimeMillis()}"
                ).apply { mkdirs() }
                val files = mutableListOf<File>()
                var sequence = 1

                // Fotografías generales de la obra. Se comparten con una firma corporativa
                // fija para que también queden identificadas fuera del informe PDF.
                db.projectReferencePhotos().list(projectId)
                    .filter { File(it.filePath).isFile }
                    .forEach { reference ->
                        val bitmap = renderReferencePhotoForShare(
                            path = reference.filePath,
                            project = project,
                            style = style,
                            maxSide = 1800
                        ) ?: return@forEach
                        val out = File(
                            outDir,
                            "%03d_Obra_general.jpg".format(Locale.ROOT, sequence++)
                        )
                        FileOutputStream(out).use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                        }
                        bitmap.recycle()
                        files += out
                    }

                // Todas las fotografías de vanos se procesan con las mismas cotas,
                // anotaciones, sello y estilo usados por el informe PDF.
                db.spaces().list(projectId).forEach { space ->
                    db.openings().list(space.id).forEach { opening ->
                        db.evidence().list(opening.id)
                            .filter { File(it.filePath).isFile }
                            .sortedBy { it.createdAt }
                            .forEach evidenceLoop@ { evidence ->
                                val bitmap = PhotoRenderer.render(
                                    evidence = evidence,
                                    project = project,
                                    space = space,
                                    opening = opening,
                                    branding = brand,
                                    style = style,
                                    maxSide = 1800
                                ) ?: return@evidenceLoop
                                if (style.watermarkLayer == PdfWatermarkLayer.FOREGROUND) {
                                    drawShareWatermark(bitmap, style)
                                }
                                val safeSpace = safeFilePart(space.name, "Espacio")
                                val safeOpening = safeFilePart(opening.code, "Vano")
                                val out = File(
                                    outDir,
                                    "%03d_%s_%s.jpg".format(Locale.ROOT, sequence++, safeSpace, safeOpening)
                                )
                                FileOutputStream(out).use { stream ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                                }
                                bitmap.recycle()
                                files += out
                            }
                    }
                }

                require(files.isNotEmpty()) { "La obra no tiene fotografías disponibles para compartir." }
                files
            }
        }
        onReady(result)
    }

    private fun renderReferencePhotoForShare(
        path: String,
        project: ProjectEntity,
        style: PersonalizationSettings,
        maxSide: Int
    ): Bitmap? {
        val source = decodeShareBitmap(path, maxSide) ?: return null
        val output = source.copy(Bitmap.Config.ARGB_8888, true) ?: run {
            source.recycle()
            return null
        }
        if (output !== source) source.recycle()
        drawReferenceCorporateFooter(output, project)
        if (style.watermarkLayer == PdfWatermarkLayer.FOREGROUND) {
            drawShareWatermark(output, style)
        }
        return output
    }

    private fun decodeShareBitmap(path: String, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val largest = maxOf(bounds.outWidth, bounds.outHeight)
        while (largest / sample > maxSide * 2) sample *= 2
        val decoded = BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        ) ?: return null
        val decodedLargest = maxOf(decoded.width, decoded.height)
        if (decodedLargest <= maxSide) return decoded
        val scale = maxSide.toFloat() / decodedLargest.toFloat()
        val width = (decoded.width * scale).toInt().coerceAtLeast(1)
        val height = (decoded.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(decoded, width, height, true).also { scaled ->
            if (scaled !== decoded) decoded.recycle()
        }
    }

    private fun drawReferenceCorporateFooter(bitmap: Bitmap, project: ProjectEntity) {
        val canvas = Canvas(bitmap)
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val footerHeight = (height * 0.13f).coerceIn(110f, 260f)
        val top = height - footerHeight
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(205, 10, 22, 48) }
        canvas.drawRect(0f, top, width, height, bg)

        var textLeft = (width * 0.035f).coerceAtLeast(18f)
        val logoPath = branding.value.logoPath
        val logo = logoPath.takeIf { it.isNotBlank() && File(it).isFile }
            ?.let { BitmapFactory.decodeFile(it) }
        if (logo != null) {
            val maxLogoHeight = footerHeight * 0.68f
            val maxLogoWidth = width * 0.23f
            val scale = minOf(maxLogoWidth / logo.width, maxLogoHeight / logo.height)
            val drawW = logo.width * scale
            val drawH = logo.height * scale
            val left = width * 0.028f
            val logoTop = top + (footerHeight - drawH) / 2f
            canvas.drawBitmap(logo, null, RectF(left, logoTop, left + drawW, logoTop + drawH), null)
            textLeft = left + drawW + width * 0.025f
            logo.recycle()
        }

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (width / 34f).coerceIn(28f, 58f)
            typeface = Typeface.DEFAULT_BOLD
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(230, 235, 244)
            textSize = (width / 53f).coerceIn(20f, 38f)
        }
        canvas.drawText("Grupo IDEA", textLeft, top + footerHeight * 0.43f, title)
        val projectText = project.name.let { if (it.length > 55) it.take(52) + "…" else it }
        canvas.drawText("Obra: $projectText", textLeft, top + footerHeight * 0.72f, body)
    }

    private fun drawShareWatermark(bitmap: Bitmap, style: PersonalizationSettings) {
        val app = getApplication<Application>()
        val watermark = runCatching {
            BitmapFactory.decodeResource(app.resources, R.drawable.grupoidea_watermark)
        }.getOrNull() ?: return
        if (watermark.width <= 0 || watermark.height <= 0) {
            watermark.recycle()
            return
        }
        val canvas = Canvas(bitmap)
        val maxWidth = bitmap.width * 0.68f
        val maxHeight = bitmap.height * 0.30f
        val scale = minOf(
            maxWidth / watermark.width.toFloat(),
            maxHeight / watermark.height.toFloat()
        )
        val drawWidth = watermark.width * scale
        val drawHeight = watermark.height * scale
        val left = (bitmap.width - drawWidth) / 2f
        val top = (bitmap.height - drawHeight) / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (style.watermarkOpacity.coerceIn(0.05f, 0.80f) * 255f)
                .toInt()
                .coerceIn(13, 204)
        }
        canvas.drawBitmap(
            watermark,
            null,
            RectF(left, top, left + drawWidth, top + drawHeight),
            paint
        )
        watermark.recycle()
    }

    private fun safeFilePart(value: String, fallback: String): String = value
        .trim()
        .replace(Regex("[^A-Za-z0-9áéíóúÁÉÍÓÚñÑ_-]+"), "_")
        .trim('_')
        .take(36)
        .ifBlank { fallback }

    private suspend fun touchProject(projectId: Long) {
        val project = db.projects().project(projectId) ?: return
        db.projects().update(project.copy(updatedAt = System.currentTimeMillis()))
    }
}
