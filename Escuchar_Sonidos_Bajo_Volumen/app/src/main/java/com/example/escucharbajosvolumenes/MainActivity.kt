package com.example.escucharbajosvolumenes

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.*
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

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import java.io.*
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

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var recordingThread: Thread? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val dateTime = findViewById<TextView>(R.id.dateTime)

        val gps = findViewById<TextView>(R.id.gps)

        val btnRecord = findViewById<Button>(R.id.btnRecord)

        val btnStop = findViewById<Button>(R.id.btnStop)

        val btnPlay = findViewById<Button>(R.id.btnPlay)

        val btnSaveMp3 = findViewById<Button>(R.id.btnSaveMp3)

        graphView = findViewById(R.id.graphView)

        requestPermissions()

        dateTime.text = Date().toString()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

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

        btnSaveMp3.setOnClickListener {

            saveAsMp3()
        }
    }

    private fun requestPermissions() {

        val permissions = mutableListOf(

            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {

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

        fusedLocationClient.lastLocation.addOnSuccessListener {

                location ->

            if (location != null) {

                gps.text =
                    "Lat: ${location.latitude} Long: ${location.longitude}"

            } else {

                gps.text = "GPS no disponible"
            }
        }
    }

    private fun startRecording() {

        if (isRecording) return

        val bufferSize = AudioRecord.getMinBufferSize(
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
            getExternalFilesDir(Environment.DIRECTORY_MUSIC)

        val sdf = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        )

        outputFile = File(
            dir,
            "Audio_5db_${sdf.format(Date())}.pcm"
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

    private fun processAudio(bufferSize: Int) {

        val shortBuffer = ShortArray(bufferSize)

        val fos = FileOutputStream(outputFile)

        val dos = DataOutputStream(
            BufferedOutputStream(fos)
        )

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

                    if (amplified > Short.MAX_VALUE)
                        amplified = Short.MAX_VALUE.toInt()

                    if (amplified < Short.MIN_VALUE)
                        amplified = Short.MIN_VALUE.toInt()

                    shortBuffer[i] = amplified.toShort()

                    dos.writeShort(shortBuffer[i].toInt())

                    sum += shortBuffer[i] * shortBuffer[i]
                }

                val rms = sqrt(sum / read)

                var db =
                    (20 * log10(rms / 32767.0)).toFloat()

                if (db.isNaN() || db.isInfinite())
                    db = 0f

                val normalized =
                    ((db + 90f) / 90f) * 100f

                handler.post {

                    graphView.addAmplitude(normalized)
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

        Toast.makeText(
            this,
            "Grabación detenida",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun playAudioAmplified() {

        if (!outputFile.exists()) {

            Toast.makeText(
                this,
                "No existe grabación",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Thread {

            try {

                val bytes = outputFile.readBytes()

                val shortData =
                    ShortArray(bytes.size / 2)

                var idx = 0

                var i = 0

                while (i < bytes.size - 1) {

                    shortData[idx] =
                        (((bytes[i + 1].toInt() shl 8)
                                or
                                (bytes[i].toInt() and 0xFF))
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

    private fun saveAsMp3() {

        try {

            val current =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
                ).format(Date())

            val fileName =
                "Audio_5db_${current}"

            val resolver = contentResolver

            val contentValues = ContentValues().apply {

                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    fileName
                )

                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    "audio/mpeg"
                )

                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_MUSIC
                )
            }

            val uri: Uri? =
                resolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

            if (uri != null) {

                val out =
                    resolver.openOutputStream(uri)

                val bytes = outputFile.readBytes()

                out?.write(bytes)

                out?.flush()

                out?.close()

                Toast.makeText(
                    this,
                    "Archivo guardado en Music",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error guardando archivo",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}