package com.example.cellradar

import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var radar: RadarView
    private lateinit var error: ErrorOverlay
    private lateinit var progress: ProgressBar
    private lateinit var list: TextView
    private lateinit var time: TextView

    private var running = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        radar = findViewById(R.id.radar)
        error = findViewById(R.id.error)
        progress = findViewById(R.id.progress)
        list = findViewById(R.id.list)
        time = findViewById(R.id.time)

        val start = findViewById<Button>(R.id.start)
        val stop = findViewById<Button>(R.id.stop)

        start.setOnClickListener {
            running = true
            scanLoop()
        }

        stop.setOnClickListener {
            running = false
        }

        updateClock()
    }

    private fun updateClock() {

        handler.post(object : Runnable {
            override fun run() {

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                time.text = sdf.format(Date())

                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun scanLoop() {

        handler.post(object : Runnable {
            override fun run() {

                if (!running) return

                try {
                    val data = simulateCells()

                    radar.cells = data
                    radar.invalidate()

                    progress.progress = Random.nextInt(10, 100)

                    list.text = data.joinToString("\n") {
                        "${it.name} | RSRP ${it.rsrp} | ~${it.distance.toInt()} m"
                    }

                } catch (e: Exception) {
                    error.error = e.toString()
                    error.invalidate()
                }

                handler.postDelayed(this, 3000)
            }
        })
    }

    /**
     * SIMULACIÓN REALISTA de celdas LTE
     * (para garantizar que SIEMPRE haya puntos)
     */
    private fun simulateCells(): List<RadarView.Cell> {

        val list = mutableListOf<RadarView.Cell>()

        for (i in 0..6) {

            val rsrp = Random.nextInt(-120, -60)

            list.add(
                RadarView.Cell(
                    distance = SignalEstimator.estimateFromRsrp(rsrp),
                    angle = Random.nextFloat() * 360,
                    rsrp = rsrp,
                    name = "LTE-$i"
                )
            )
        }

        return list
    }
}