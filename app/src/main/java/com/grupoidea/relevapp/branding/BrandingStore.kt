package com.grupoidea.relevapp.branding

import android.content.Context
import com.grupoidea.relevapp.R
import java.io.File

data class BrandingSettings(
    val companyName: String = "Grupo IDEA",
    val logoPath: String = "",
    val stampEnabled: Boolean = true,
    val showTimestamp: Boolean = true,
    val showProject: Boolean = true,
    val showSpace: Boolean = true,
    val showOpening: Boolean = true
)

/**
 * Identidad corporativa fija de la aplicación.
 *
 * No se leen preferencias ni se admite un logo elegido por el usuario: el recurso
 * incluido en el APK es la única marca utilizada por fotografías, vistas previas y PDF.
 */
class BrandingStore(private val context: Context) {

    fun load(): BrandingSettings = BrandingSettings(
        companyName = "Grupo IDEA",
        logoPath = ensureFixedLogoPath(),
        stampEnabled = true,
        showTimestamp = true,
        showProject = true,
        showSpace = true,
        showOpening = true
    )

    private fun ensureFixedLogoPath(): String = runCatching {
        val dir = File(context.filesDir, "branding").apply { mkdirs() }
        val target = File(dir, "grupoidea_brand_logo.png")

        // Se sobrescribe desde el recurso empaquetado para que una actualización de la
        // aplicación también actualice la marca y no pueda quedar un logo anterior.
        context.resources.openRawResource(R.drawable.grupoidea_brand_logo).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target.absolutePath
    }.getOrDefault("")
}
