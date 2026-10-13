package com.illu.relevametal.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var startedAt: Long = 0L
    private var currentFile: File? = null

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

    fun start(output: File) {
        stopSilently(deleteFile = false)
        output.parentFile?.mkdirs()
        val next = newRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(output.absolutePath)
            prepare()
            this.start()
        }
        currentFile = output
        startedAt = System.currentTimeMillis()
        recorder = next
    }

    fun stop(): Result<Pair<File, Long>> = runCatching {
        val active = requireNotNull(recorder) { "No hay una grabación activa" }
        val file = requireNotNull(currentFile) { "No se encontró el archivo de audio" }
        val duration = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
        try {
            active.stop()
        } finally {
            active.release()
            recorder = null
            currentFile = null
            startedAt = 0L
        }
        require(file.isFile && file.length() > 0L) { "La nota de audio quedó vacía" }
        file to duration
    }.onFailure {
        stopSilently(deleteFile = true)
    }

    fun cancel() = stopSilently(deleteFile = true)

    private fun stopSilently(deleteFile: Boolean) {
        val active = recorder
        recorder = null
        runCatching { active?.stop() }
        runCatching { active?.release() }
        if (deleteFile) runCatching { currentFile?.delete() }
        currentFile = null
        startedAt = 0L
    }
}
