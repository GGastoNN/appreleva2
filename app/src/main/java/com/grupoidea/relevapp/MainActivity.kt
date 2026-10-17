package com.grupoidea.relevapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.grupoidea.relevapp.camera.CameraCapture
import com.grupoidea.relevapp.ui.*

class MainActivity : ComponentActivity() {
    private val vm by viewModels<AppViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val personalization by vm.personalization.collectAsState()
            GrupoIdeaTheme(personalization) {
                App(vm = vm)
            }
        }
    }
}

private sealed interface Screen {
    data object Projects : Screen
    data class Project(val projectId: Long) : Screen
    data class Space(val projectId: Long, val spaceId: Long) : Screen
    data class Opening(val projectId: Long, val spaceId: Long, val openingId: Long) : Screen
    data class Camera(val projectId: Long, val spaceId: Long, val openingId: Long) : Screen
    data class Evidence(
        val projectId: Long,
        val spaceId: Long,
        val openingId: Long,
        val evidenceId: Long
    ) : Screen
}

@Composable
private fun App(vm: AppViewModel) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Projects) }

    fun goBack() {
        screen = when (val current = screen) {
            Screen.Projects -> Screen.Projects
            is Screen.Project -> Screen.Projects
            is Screen.Space -> Screen.Project(current.projectId)
            is Screen.Opening -> Screen.Space(current.projectId, current.spaceId)
            is Screen.Camera -> Screen.Opening(current.projectId, current.spaceId, current.openingId)
            is Screen.Evidence -> Screen.Opening(current.projectId, current.spaceId, current.openingId)
        }
    }

    BackHandler(enabled = screen !is Screen.Projects) { goBack() }

    when (val current = screen) {
        Screen.Projects -> {
            ProjectsScreen(
                vm = vm,
                onOpen = { projectId -> screen = Screen.Project(projectId) },
                onShareData = { file ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.files",
                        file
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, "Compartir datos de relevamiento")
                    )
                }
            )
        }

        is Screen.Project -> {
            ProjectScreen(
                vm = vm,
                projectId = current.projectId,
                onBack = { screen = Screen.Projects },
                onOpenSpace = { spaceId ->
                    screen = Screen.Space(current.projectId, spaceId)
                },
                onSharePdf = { file ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.files",
                        file
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, "Compartir informe de relevamiento")
                    )
                },
                onShareData = { file ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.files",
                        file
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, "Compartir datos de la obra")
                    )
                },
                onShareImages = { files ->
                    val uris = ArrayList(
                        files.map { file ->
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.files",
                                file
                            )
                        }
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "image/jpeg"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, "Compartir fotos de la obra")
                    )
                },
                onDeleted = { screen = Screen.Projects }
            )
        }

        is Screen.Space -> {
            SpaceScreen(
                vm = vm,
                projectId = current.projectId,
                spaceId = current.spaceId,
                onBack = { screen = Screen.Project(current.projectId) },
                onOpenOpening = { openingId ->
                    screen = Screen.Opening(
                        current.projectId,
                        current.spaceId,
                        openingId
                    )
                },
                onDeleted = { screen = Screen.Project(current.projectId) }
            )
        }

        is Screen.Opening -> {
            OpeningScreen(
                vm = vm,
                projectId = current.projectId,
                openingId = current.openingId,
                onBack = {
                    screen = Screen.Space(current.projectId, current.spaceId)
                },
                onCamera = {
                    screen = Screen.Camera(
                        current.projectId,
                        current.spaceId,
                        current.openingId
                    )
                },
                onEditEvidence = { evidenceId ->
                    screen = Screen.Evidence(
                        current.projectId,
                        current.spaceId,
                        current.openingId,
                        evidenceId
                    )
                },
                onDuplicated = { newOpeningId ->
                    screen = Screen.Opening(current.projectId, current.spaceId, newOpeningId)
                },
                onNavigateOpening = { openingId ->
                    screen = Screen.Opening(current.projectId, current.spaceId, openingId)
                },
                onDeleted = {
                    screen = Screen.Space(current.projectId, current.spaceId)
                }
            )
        }

        is Screen.Camera -> {
            CameraCapture(
                onCaptured = { path ->
                    vm.addEvidence(
                        projectId = current.projectId,
                        openingId = current.openingId,
                        path = path
                    ) { evidenceId ->
                        screen = Screen.Evidence(
                            current.projectId,
                            current.spaceId,
                            current.openingId,
                            evidenceId
                        )
                    }
                },
                onCancel = {
                    screen = Screen.Opening(
                        current.projectId,
                        current.spaceId,
                        current.openingId
                    )
                }
            )
        }

        is Screen.Evidence -> {
            EvidenceEditorScreen(
                vm = vm,
                projectId = current.projectId,
                evidenceId = current.evidenceId,
                onBack = {
                    screen = Screen.Opening(
                        current.projectId,
                        current.spaceId,
                        current.openingId
                    )
                },
                onDeleted = {
                    screen = Screen.Opening(
                        current.projectId,
                        current.spaceId,
                        current.openingId
                    )
                },
                onShareImage = { file ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.files",
                        file
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, "Compartir fotografía técnica")
                    )
                }
            )
        }
    }
}
