package com.example.towerscanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.telephony.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    lateinit var map: MapView

    lateinit var txtInfo: TextView

    lateinit var btnScan: Button
    lateinit var btnStop: Button
    lateinit var btnPdf: Button

    lateinit var telephonyManager: TelephonyManager

    val handler = Handler(Looper.getMainLooper())

    var scanning = false

    var greenData = ""
    var redData = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this,
            getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        txtInfo = findViewById(R.id.txtInfo)

        btnScan = findViewById(R.id.btnScan)
        btnStop = findViewById(R.id.btnStop)
        btnPdf = findViewById(R.id.btnPdf)

        map.setMultiTouchControls(true)

        telephonyManager =
            getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        requestPermissions()

        btnScan.setOnClickListener {

            scanning = true

            startScanning()
        }

        btnStop.setOnClickListener {

            scanning = false
        }

        btnPdf.setOnClickListener {

            generatePdf()
        }
    }

    private fun requestPermissions() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            ),
            1
        )
    }

    private fun startScanning() {

        handler.post(object : Runnable {

            override fun run() {

                if (scanning) {

                    scanCell()

                    handler.postDelayed(this, 5000)
                }
            }
        })
    }

    private fun scanCell() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val cells = telephonyManager.allCellInfo

        for (cell in cells) {

            when (cell) {

                is CellInfoLte -> {

                    val rsrp = cell.cellSignalStrength.rsrp
                    val rssi = cell.cellSignalStrength.rssi

                    val cid = cell.cellIdentity.ci

                    val lat = 3.4516
                    val lon = -76.5320

                    val distance = calculateDistance(rsrp, rssi)

                    val towerLat =
                        lat + (distance / 111111.0)

                    val towerLon =
                        lon + (distance / 111111.0)

                    showUser(lat, lon)

                    showTower(
                        towerLat,
                        towerLon,
                        Color.GREEN
                    )

                    greenData =
                        """
                        4G LTE
                        CID: $cid
                        RSSI: $rssi
                        RSRP: $rsrp
                        Torre Lat: $towerLat
                        Torre Lon: $towerLon
                        """.trimIndent()

                    txtInfo.text =
                        greenData + "\n\n" + redData
                }

                is CellInfoGsm -> {

                    val rssi =
                        cell.cellSignalStrength.dbm

                    val cid =
                        cell.cellIdentity.cid

                    val lat = 3.4516
                    val lon = -76.5320

                    val distance =
                        calculateDistance(-90, rssi)

                    val towerLat =
                        lat - (distance / 111111.0)

                    val towerLon =
                        lon - (distance / 111111.0)

                    showTower(
                        towerLat,
                        towerLon,
                        Color.RED
                    )

                    redData =
                        """
                        GSM / 2G
                        CID: $cid
                        RSSI: $rssi
                        Torre Lat: $towerLat
                        Torre Lon: $towerLon
                        """.trimIndent()

                    txtInfo.text =
                        greenData + "\n\n" + redData
                }
            }
        }
    }

    private fun calculateDistance(
        rsrp: Int,
        rssi: Int
    ): Double {

        val signal =
            (rsrp + rssi) / 2.0

        return when {

            signal > -70 -> 100.0

            signal > -90 -> 300.0

            signal > -110 -> 700.0

            else -> 1500.0
        }
    }

    private fun showUser(
        lat: Double,
        lon: Double
    ) {

        val point =
            GeoPoint(lat, lon)

        map.controller.setZoom(16.0)
        map.controller.setCenter(point)

        val marker = Marker(map)

        marker.position = point

        marker.title = "Mi ubicación"

        map.overlays.add(marker)

        map.invalidate()
    }

    private fun showTower(
        lat: Double,
        lon: Double,
        color: Int
    ) {

        val point =
            GeoPoint(lat, lon)

        val marker = Marker(map)

        marker.position = point

        marker.title = "Torre"

        if (color == Color.GREEN) {

            marker.icon =
                getDrawable(android.R.drawable.presence_online)
        }
        else {

            marker.icon =
                getDrawable(android.R.drawable.presence_busy)
        }

        map.overlays.add(marker)

        map.invalidate()
    }

    private fun generatePdf() {

        val pdf = PdfDocument()

        val pageInfo =
            PdfDocument.PageInfo.Builder(
                1200,
                1800,
                1
            ).create()

        val page =
            pdf.startPage(pageInfo)

        val canvas =
            page.canvas

        val paint =
            android.graphics.Paint()

        paint.textSize = 30f

        canvas.drawText(
            "Tower Scanner",
            50f,
            50f,
            paint
        )

        canvas.drawText(
            greenData,
            50f,
            500f,
            paint
        )

        canvas.drawText(
            redData,
            50f,
            900f,
            paint
        )

        pdf.finishPage(page)

        val file =
            File(
                Environment
                    .getExternalStorageDirectory(),
                "tower_report.pdf"
            )

        pdf.writeTo(
            FileOutputStream(file)
        )

        pdf.close()
    }
}