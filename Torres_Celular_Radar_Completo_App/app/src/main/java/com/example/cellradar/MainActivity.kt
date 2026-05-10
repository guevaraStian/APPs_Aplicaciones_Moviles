package com.example.cellradar

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.telephony.*
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var radar: RadarView
    private lateinit var table: TextView
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        radar = findViewById(R.id.radarView)
        table = findViewById(R.id.txtTable)
        progress = findViewById(R.id.progressBar)
        status = findViewById(R.id.txtStatus)

        requestPermissions()

        findViewById<Button>(R.id.btnStart)
            .setOnClickListener {

                performScan()
            }

        findViewById<Button>(R.id.btnClear)
            .setOnClickListener {

                radar.update(emptyList())

                table.text = ""

                status.text = "Datos borrados"
            }
    }

    private fun requestPermissions() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            ),
            100
        )
    }

    private fun performScan() {

        progress.visibility = View.VISIBLE

        progress.progress = 0

        status.text = "Escaneando LTE..."

        Thread {

            for (i in 0..100 step 10) {

                Thread.sleep(100)

                runOnUiThread {

                    progress.progress = i
                }
            }

            val result = scanLTE()

            runOnUiThread {

                radar.update(result.first)

                table.text = result.second

                progress.visibility = View.GONE

                status.text = "Escaneo finalizado"
            }

        }.start()
    }

    private fun scanLTE():
            Pair<List<RadarView.Signal>, String> {

        val tm =
            getSystemService(
                TELEPHONY_SERVICE
            ) as TelephonyManager

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return Pair(
                emptyList(),
                "Permisos denegados"
            )
        }

        val cells = tm.allCellInfo

        if (cells.isNullOrEmpty()) {

            return Pair(
                emptyList(),
                "No se encontraron celdas LTE"
            )
        }

        val signals =
            mutableListOf<RadarView.Signal>()

        val sb = StringBuilder()

        sb.append(
            "======= ESCANEO RF =======\n\n"
        )

        for ((index, cell) in cells.withIndex()) {

            if (cell is CellInfoLte) {

                val signal =
                    cell.cellSignalStrength

                val dbm =
                    signal.dbm

                val rsrp =
                    if (Build.VERSION.SDK_INT >= 29)
                        signal.rsrp
                    else
                        dbm

                val rsrq =
                    if (Build.VERSION.SDK_INT >= 29)
                        signal.rsrq
                    else
                        -1

                val id =
                    cell.cellIdentity

                val ci = id.ci

                val tac = id.tac

                val pci = id.pci

                // DISTANCIA ESTIMADA
                val distance =
                    estimateDistance(rsrp)

                signals.add(

                    RadarView.Signal(

                        label = "LTE-$index",

                        rsrp = rsrp,

                        distance = distance
                    )
                )

                sb.append(
                    """
                    CELDA LTE #$index
                    ----------------------
                    CELL ID: $ci
                    TAC: $tac
                    PCI: $pci
                    
                    RSSI/dBm: $dbm
                    RSRP: $rsrp
                    RSRQ: $rsrq
                    
                    DISTANCIA:
                    ${distance} metros
                    
                    """.trimIndent()
                )

                sb.append("\n\n")
            }
        }

        signals.sortBy { it.distance }

        return Pair(
            signals,
            sb.toString()
        )
    }

    // CALCULO POR RSSI/RSRP
    private fun estimateDistance(
        rsrp: Int
    ): Int {

        val txPower = -30

        val pathLoss = 2.3

        val distance =
            10.0.pow(
                (
                        txPower - rsrp
                        ) / (
                        10 * pathLoss
                        )
            )

        return distance.toInt()
            .coerceIn(1, 5000)
    }
}