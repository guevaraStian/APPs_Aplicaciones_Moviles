package com.example.escucharbajosvolumenes

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.media.*
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.createBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.*
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private val SAMPLE_RATE = 44100

    private var isRecording = false

    private var audioRecord: AudioRecord? = null

    private lateinit var graphView: GraphView

    private lateinit var outputFile: File

    private lateinit var wavFile: File

    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    private var recordingThread: Thread? = null

    private val handler =
        Handler(Looper.getMainLooper())

    private var startDate = ""

    private var startLocation = ""

    private val dbValues =
        mutableListOf<Float>()

    private val timeValues =
        mutableListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val dateTime =
            findViewById<TextView>(R.id.dateTime)

        val gps =
            findViewById<TextView>(R.id.gps)

        val btnRecord =
            findViewById<Button>(R.id.btnRecord)

        val btnStop =
            findViewById<Button>(R.id.btnStop)

        val btnPlay =
            findViewById<Button>(R.id.btnPlay)

        val btnSaveWav =
            findViewById<Button>(R.id.btnSaveMp3)

        graphView =
            findViewById(R.id.graphView)

        requestPermissions()

        dateTime.text =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(this)

        getLocation(gps)

        btnRecord.setOnClickListener {

            startRecording()
        }

        btnStop.setOnClickListener {

            stopRecording()
        }

        btnPlay.setOnClickListener {

            playAudioAmplified()
        }

        btnSaveWav.setOnClickListener {

            saveAsWavAndVideo()
        }
    }

    private fun requestPermissions() {

        val permissions =
            mutableListOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        if (
            Build.VERSION.SDK_INT <=
            Build.VERSION_CODES.P
        ) {

            permissions.add(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        ActivityCompat.requestPermissions(
            this,
            permissions.toTypedArray(),
            1
        )
    }

    private fun getLocation(gps: TextView) {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient
            .lastLocation
            .addOnSuccessListener {

                    location: Location? ->

                if (location != null) {

                    startLocation =
                        "Lat: ${location.latitude}  " +
                                "Long: ${location.longitude}"

                    gps.text = startLocation

                } else {

                    gps.text =
                        "GPS no disponible"
                }
            }
    }

    private fun startRecording() {

        if (isRecording) return

        dbValues.clear()

        timeValues.clear()

        startDate =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

        val bufferSize =
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val dir =
            getExternalFilesDir(
                Environment.DIRECTORY_MUSIC
            )

        val sdf =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            )

        outputFile = File(
            dir,
            "Audio_${sdf.format(Date())}.pcm"
        )

        wavFile = File(
            dir,
            "Audio_${sdf.format(Date())}.wav"
        )

        audioRecord?.startRecording()

        isRecording = true

        Toast.makeText(
            this,
            "Grabando...",
            Toast.LENGTH_SHORT
        ).show()

        recordingThread = Thread {

            processAudio(bufferSize)
        }

        recordingThread?.start()
    }

    private fun processAudio(
        bufferSize: Int
    ) {

        val shortBuffer =
            ShortArray(bufferSize)

        val fos =
            FileOutputStream(outputFile)

        val dos =
            DataOutputStream(
                BufferedOutputStream(fos)
            )

        val startTime =
            System.currentTimeMillis()

        while (isRecording) {

            val read =
                audioRecord?.read(
                    shortBuffer,
                    0,
                    shortBuffer.size
                ) ?: 0

            if (read > 0) {

                var sum = 0.0

                for (i in 0 until read) {

                    var amplified =
                        (shortBuffer[i] * 5).toInt()

                    if (
                        amplified >
                        Short.MAX_VALUE
                    ) {
                        amplified =
                            Short.MAX_VALUE.toInt()
                    }

                    if (
                        amplified <
                        Short.MIN_VALUE
                    ) {
                        amplified =
                            Short.MIN_VALUE.toInt()
                    }

                    shortBuffer[i] =
                        amplified.toShort()

                    dos.writeShort(
                        shortBuffer[i].toInt()
                    )

                    sum +=
                        shortBuffer[i] *
                                shortBuffer[i]
                }

                val rms =
                    sqrt(sum / read)

                var db =
                    (
                            20 *
                                    log10(
                                        rms / 32767.0
                                    )
                            ).toFloat()

                if (
                    db.isNaN() ||
                    db.isInfinite()
                ) {
                    db = 0f
                }

                db += 25f

                if (db < 0f) db = 0f

                if (db > 120f) db = 120f

                val currentTime =
                    (
                            System.currentTimeMillis()
                                    - startTime
                            ) / 1000f

                dbValues.add(db)

                timeValues.add(currentTime)

                handler.post {

                    graphView.addPoint(
                        currentTime,
                        db
                    )
                }
            }
        }

        dos.close()
    }

    private fun stopRecording() {

        if (!isRecording) return

        isRecording = false

        audioRecord?.stop()

        audioRecord?.release()

        audioRecord = null

        recordingThread?.join()

        convertPcmToWav()

        Toast.makeText(
            this,
            "Grabación detenida",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun convertPcmToWav() {

        val pcmData =
            outputFile.readBytes()

        val output =
            FileOutputStream(wavFile)

        val totalDataLen =
            pcmData.size + 36

        val byteRate =
            16 * SAMPLE_RATE / 8

        val header =
            ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        writeInt(
            header,
            4,
            totalDataLen
        )

        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        writeInt(header, 16, 16)

        writeShort(header, 20, 1.toShort())

        writeShort(header, 22, 1.toShort())

        writeInt(header, 24, SAMPLE_RATE)

        writeInt(header, 28, byteRate)

        writeShort(header, 32, 2.toShort())

        writeShort(header, 34, 16.toShort())

        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        writeInt(header, 40, pcmData.size)

        output.write(header)

        output.write(pcmData)

        output.flush()

        output.close()
    }

    private fun writeInt(
        data: ByteArray,
        offset: Int,
        value: Int
    ) {

        data[offset] =
            (value and 0xff).toByte()

        data[offset + 1] =
            (value shr 8 and 0xff).toByte()

        data[offset + 2] =
            (value shr 16 and 0xff).toByte()

        data[offset + 3] =
            (value shr 24 and 0xff).toByte()
    }

    private fun writeShort(
        data: ByteArray,
        offset: Int,
        value: Short
    ) {

        data[offset] =
            (value.toInt() and 0xff).toByte()

        data[offset + 1] =
            (
                    value.toInt() shr 8
                            and 0xff
                    ).toByte()
    }

    private fun playAudioAmplified() {

        if (!wavFile.exists()) {

            Toast.makeText(
                this,
                "No existe grabación",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Thread {

            try {

                val bytes =
                    wavFile.readBytes()

                val audioData =
                    bytes.copyOfRange(
                        44,
                        bytes.size
                    )

                val shortData =
                    ShortArray(
                        audioData.size / 2
                    )

                var idx = 0

                var i = 0

                while (
                    i <
                    audioData.size - 1
                ) {

                    shortData[idx] =
                        (
                                (
                                        audioData[i + 1]
                                            .toInt() shl 8
                                        )
                                        or
                                        (
                                                audioData[i]
                                                    .toInt()
                                                        and 0xFF
                                                )
                                ).toShort()

                    idx++

                    i += 2
                }

                val minBuffer =
                    AudioTrack.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )

                val audioTrack =
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBuffer,
                        AudioTrack.MODE_STREAM
                    )

                audioTrack.play()

                audioTrack.write(
                    shortData,
                    0,
                    shortData.size
                )

                audioTrack.stop()

                audioTrack.release()

            } catch (e: Exception) {

                e.printStackTrace()
            }

        }.start()
    }

    private fun saveAsWavAndVideo() {

        try {

            val current =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
                ).format(Date())

            val resolver =
                contentResolver

            val audioValues =
                ContentValues().apply {

                    put(
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        "Audio_$current.wav"
                    )

                    put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        "audio/wav"
                    )

                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_MUSIC
                    )
                }

            val audioUri: Uri? =
                resolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    audioValues
                )

            if (audioUri != null) {

                val out =
                    resolver.openOutputStream(audioUri)

                out?.write(
                    wavFile.readBytes()
                )

                out?.flush()

                out?.close()
            }

            val bitmap =
                createBitmap(
                    graphView.width,
                    graphView.height
                )

            val canvas =
                Canvas(bitmap)

            graphView.draw(canvas)

            val imageFile =
                File(
                    getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES
                    ),
                    "grafica_$current.png"
                )

            val imageOut =
                FileOutputStream(imageFile)

            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                imageOut
            )

            imageOut.flush()

            imageOut.close()

            Toast.makeText(
                this,
                "WAV e imagen guardados",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error guardando",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}