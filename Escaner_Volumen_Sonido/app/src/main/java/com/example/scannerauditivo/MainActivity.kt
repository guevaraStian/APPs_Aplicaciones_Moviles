package com.example.scannerauditivo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.log10

class MainActivity : AppCompatActivity() {

    private lateinit var chart: LineChart

    private val sampleRate = 44100

    private val bufferSize = 2048

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        chart = findViewById(R.id.chart)

        pedirPermisos()

        configurarGrafica()

        iniciarMicrofono()
    }

    private fun pedirPermisos() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1
        )
    }

    private fun configurarGrafica() {

        chart.description.isEnabled = false

        chart.setBackgroundColor(Color.BLACK)

        chart.legend.textColor = Color.GREEN

        chart.axisRight.isEnabled = false

        val ejeX = chart.xAxis

        ejeX.position = XAxis.XAxisPosition.BOTTOM

        ejeX.textColor = Color.GREEN

        ejeX.axisMinimum = 20f

        ejeX.axisMaximum = 20000f

        val ejeY = chart.axisLeft

        ejeY.textColor = Color.GREEN

        ejeY.axisMinimum = 100f

        ejeY.axisMaximum = 190f
    }

    private fun iniciarMicrofono() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord.startRecording()

        thread {

            val buffer = ShortArray(bufferSize)

            while (true) {

                audioRecord.read(buffer, 0, bufferSize)

                val entries = ArrayList<Entry>()

                for (i in 1 until bufferSize / 2) {

                    val frecuencia = i * sampleRate / bufferSize

                    if (frecuencia in 20..20000) {

                        val amplitud = abs(buffer[i].toInt())

                        val db =
                            (20 * log10(amplitud.toDouble() + 1))
                                .toFloat() + 100

                        entries.add(
                            Entry(
                                frecuencia.toFloat(),
                                db.coerceIn(100f, 190f)
                            )
                        )
                    }
                }

                runOnUiThread {

                    val dataSet =
                        LineDataSet(entries, "Audio")

                    dataSet.color = Color.GREEN

                    dataSet.setDrawCircles(false)

                    dataSet.lineWidth = 1.5f

                    dataSet.valueTextColor = Color.GREEN

                    chart.data = LineData(dataSet)

                    chart.invalidate()
                }

                Thread.sleep(100)
            }
        }
    }
}