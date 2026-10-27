package com.fdev.nutrimindai.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class AndroidAudioRecorder @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AudioRecorder {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var recordingJob: Job? = null
    private var timerJob: Job? = null

    private var audioRecord: AudioRecord? = null
    private var tempPcmFile: File? = null
    private var targetWavFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    override val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    override val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    @SuppressLint("MissingPermission")
    override fun startRecording(outputFile: File): Result<Unit> {
        if (_isRecording.value) {
            return Result.failure(IllegalStateException("Already recording"))
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure(SecurityException("RECORD_AUDIO permission not granted"))
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return Result.failure(IllegalStateException("Unable to get valid buffer size for AudioRecord"))
        }

        val bufferSize = (minBufferSize * 2).coerceAtLeast(2048)

        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return Result.failure(IllegalStateException("AudioRecord failed to initialize"))
            }

            audioRecord = record
            targetWavFile = outputFile
            tempPcmFile = File(context.cacheDir, "temp_recording_${System.currentTimeMillis()}.pcm")

            record.startRecording()
            _isRecording.value = true
            _durationSeconds.value = 0
            _amplitude.value = 0f

            startTimerJob()
            startReadingJob(record, bufferSize, tempPcmFile!!)

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error starting voice recording")
            cleanup()
            Result.failure(e)
        }
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _isRecording.value) {
                delay(1000L)
                _durationSeconds.value += 1
            }
        }
    }

    private fun startReadingJob(record: AudioRecord, bufferSize: Int, pcmFile: File) {
        recordingJob?.cancel()
        recordingJob = scope.launch {
            val buffer = ShortArray(bufferSize / 2)
            val byteBuffer = ByteArray(bufferSize)

            try {
                FileOutputStream(pcmFile).use { fos ->
                    while (isActive && _isRecording.value) {
                        val readShorts = record.read(buffer, 0, buffer.size)
                        if (readShorts > 0) {
                            var sumSquares = 0.0
                            for (i in 0 until readShorts) {
                                val s = buffer[i]
                                sumSquares += (s * s).toDouble()

                                // Convert short to 2 bytes (Little Endian)
                                byteBuffer[i * 2] = (s.toInt() and 0xff).toByte()
                                byteBuffer[i * 2 + 1] = ((s.toInt() shr 8) and 0xff).toByte()
                            }

                            fos.write(byteBuffer, 0, readShorts * 2)

                            // Calculate normalized RMS amplitude (0.0 to 1.0)
                            val rms = sqrt(sumSquares / readShorts)
                            val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                            _amplitude.value = normalized
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error reading audio stream into PCM file")
            }
        }
    }

    override fun stopRecording(): File? {
        if (!_isRecording.value) return null

        _isRecording.value = false
        recordingJob?.cancel()
        timerJob?.cancel()

        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Timber.w(e, "Error stopping AudioRecord")
        } finally {
            audioRecord = null
        }

        val pcm = tempPcmFile
        val wav = targetWavFile

        if (pcm == null || !pcm.exists() || pcm.length() == 0L || wav == null) {
            cleanup()
            return null
        }

        return try {
            pcmToWav(pcm, wav, SAMPLE_RATE, 1, 16)
            pcm.delete()
            wav
        } catch (e: Exception) {
            Timber.e(e, "Failed to convert PCM to WAV")
            cleanup()
            null
        }
    }

    override fun cancelRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        timerJob?.cancel()

        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Timber.w(e, "Error canceling AudioRecord")
        } finally {
            audioRecord = null
        }

        cleanup()
    }

    private fun cleanup() {
        _isRecording.value = false
        _amplitude.value = 0f
        _durationSeconds.value = 0
        tempPcmFile?.delete()
        tempPcmFile = null
        targetWavFile = null
    }

    private fun pcmToWav(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val pcmSize = pcmFile.length()
        FileInputStream(pcmFile).use { fis ->
            FileOutputStream(wavFile).use { fos ->
                writeWavHeader(fos, pcmSize, sampleRate.toLong(), channels, bitsPerSample)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    fos.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    private fun writeWavHeader(
        out: OutputStream,
        pcmDataLength: Long,
        sampleRate: Long,
        channels: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalDataLen = pcmDataLength + 36

        val header = ByteArray(44)
        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size = 16 for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = blockAlign.toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmDataLength and 0xff).toByte()
        header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
        header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
        header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}
