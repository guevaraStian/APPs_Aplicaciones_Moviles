
// ===============================
package com.example.towerscanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.telephony.*
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    lateinit var map: MapView

    lateinit var txtInfo: TextView

    lateinit var btnScan: Button
    lateinit var btnStop: Button
    lateinit var btnPdf: Button

    lateinit var telephonyManager: TelephonyManager

    lateinit var fusedLocationClient:
            FusedLocationProviderClient

    var currentLat = 3.4516
    var currentLon = -76.5320

    val handler = Handler(Looper.getMainLooper())

    var scanning = false

    var greenData = ""
    var redData = ""

    var staticGreenData = ""

    // ===============================
    // MARCADOR DINAMICO ANTENA
    // ===============================
    var towerCircle: Polygon? = null

    // ===============================
    // COLOR ACTUAL ANTENA
    // ===============================
    var currentTowerColor = Color.GREEN

    // ===============================
    // POSICION ACTUAL ANTENA
    // ===============================
    var currentTowerLat = 0.0
    var currentTowerLon = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        title = "Escaner Imsi Catcher"

        Configuration.getInstance().load(
            this,
            getSharedPreferences(
                "osmdroid",
                MODE_PRIVATE
            )
        )

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        txtInfo = findViewById(R.id.txtInfo)

        btnScan = findViewById(R.id.btnScan)
        btnStop = findViewById(R.id.btnStop)
        btnPdf = findViewById(R.id.btnPdf)

        findViewById<View>(
            android.R.id.content
        ).setBackgroundColor(Color.BLACK)

        btnScan.setBackgroundColor(Color.GRAY)
        btnStop.setBackgroundColor(Color.GRAY)
        btnPdf.setBackgroundColor(Color.GRAY)

        btnScan.setTextColor(Color.RED)
        btnStop.setTextColor(Color.RED)
        btnPdf.setTextColor(Color.RED)

        txtInfo.setTextColor(Color.GREEN)

        map.setMultiTouchControls(true)

        telephonyManager =
            getSystemService(
                TELEPHONY_SERVICE
            ) as TelephonyManager

        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(this)

        requestPermissions()

        getCurrentLocation()

        btnScan.setOnClickListener {

            Toast.makeText(
                this,
                "ESCANEANDO...",
                Toast.LENGTH_SHORT
            ).show()

            scanning = true

            startScanning()
        }

        btnStop.setOnClickListener {

            scanning = false

            Toast.makeText(
                this,
                "SE DETUVO",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnPdf.setOnClickListener {

            try {

                generatePdf()

                Toast.makeText(
                    this,
                    "PDF DESCARGADO",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "ERROR PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                e.printStackTrace()
            }
        }
    }

    private fun requestPermissions() {

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

        if (Build.VERSION.SDK_INT <= 32) {

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

    private fun getCurrentLocation() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->

                if (location != null) {

                    currentLat =
                        location.latitude

                    currentLon =
                        location.longitude

                    showUser(
                        currentLat,
                        currentLon
                    )
                }
            }
    }

    private fun startScanning() {

        handler.post(object : Runnable {

            override fun run() {

                if (scanning) {

                    getCurrentLocation()

                    scanCell()

                    handler.postDelayed(
                        this,
                        5000
                    )
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

        val currentTime =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

        val cells =
            telephonyManager.allCellInfo

        for (cell in cells) {

            when (cell) {

                // ===============================
                // 4G LTE
                // ===============================
                is CellInfoLte -> {

                    val rsrp =
                        cell.cellSignalStrength.rsrp

                    val rsrq =
                        cell.cellSignalStrength.rsrq

                    val rssi =
                        cell.cellSignalStrength.rssi

                    val ci =
                        cell.cellIdentity.ci

                    val tac =
                        cell.cellIdentity.tac

                    val pci =
                        cell.cellIdentity.pci

                    val earfcn =
                        cell.cellIdentity.earfcn

                    val bandwidth =
                        cell.cellIdentity.bandwidth

                    val distance =
                        calculateDistance(
                            rsrp,
                            rssi
                        )

                    val lat = currentLat
                    val lon = currentLon

                    val towerLat =
                        lat + (distance / 111111.0)

                    val towerLon =
                        lon + (distance / 111111.0)

                    currentTowerLat = towerLat
                    currentTowerLon = towerLon

                    showUser(lat, lon)

                    // ===============================
                    // BOLA VERDE DINAMICA
                    // ===============================
                    updateTowerCircle(
                        towerLat,
                        towerLon,
                        Color.GREEN
                    )

                    greenData =
                        """
🟢 ANTENA 4G LTE
🟢 FECHA: $currentTime
🟢 DISTANCIA: $distance m
🟢 CI: $ci
🟢 TAC: $tac
🟢 PCI: $pci
🟢 EARFCN: $earfcn
🟢 BANDWIDTH: $bandwidth
🟢 RSRP: $rsrp
🟢 RSRQ: $rsrq
🟢 RSSI: $rssi
🟢 LATITUD: $lat
🟢 LONGITUD: $lon
🟢 TORRE LAT: $towerLat
🟢 TORRE LON: $towerLon
                        """.trimIndent()

                    // ===============================
                    // GUARDAR INFO 4G ESTATICA
                    // ===============================
                    staticGreenData = greenData

                    txtInfo.text =
                        """
$greenData

🔴 ANTENA 2G GSM
🔴 ESPERANDO ACTUALIZACION...
                        """.trimIndent()
                }

                // ===============================
                // 2G GSM
                // ===============================
                is CellInfoGsm -> {

                    val rssi =
                        cell.cellSignalStrength.dbm

                    val ci =
                        cell.cellIdentity.cid

                    val lac =
                        cell.cellIdentity.lac

                    val distance =
                        calculateDistance(
                            -90,
                            rssi
                        )

                    val lat = currentLat
                    val lon = currentLon

                    val towerLat =
                        lat - (distance / 111111.0)

                    val towerLon =
                        lon - (distance / 111111.0)

                    currentTowerLat = towerLat
                    currentTowerLon = towerLon

                    // ===============================
                    // CAMBIAR BOLA A ROJA
                    // ===============================
                    updateTowerCircle(
                        towerLat,
                        towerLon,
                        Color.RED
                    )

                    redData =
                        """
🔴 ANTENA 2G GSM
🔴 FECHA: $currentTime
🔴 DISTANCIA: $distance m
🔴 CI: $ci
🔴 TAC: $lac
🔴 PCI: ---
🔴 EARFCN: ---
🔴 BANDWIDTH: ---
🔴 RSRP: ---
🔴 RSRQ: ---
🔴 RSSI: $rssi
🔴 LATITUD: $lat
🔴 LONGITUD: $lon
🔴 TORRE LAT: $towerLat
🔴 TORRE LON: $towerLon
                        """.trimIndent()

                    // ===============================
                    // 4G QUEDA ESTATICO
                    // 2G ACTUALIZA ABAJO
                    // ===============================
                    txtInfo.text =
                        """
$staticGreenData

$redData
                        """.trimIndent()
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

    // ===============================
    // MOSTRAR USUARIO
    // ===============================
    private fun showUser(
        lat: Double,
        lon: Double
    ) {

        val point =
            GeoPoint(lat, lon)

        map.controller.setZoom(16.0)

        map.controller.setCenter(point)

        // ===============================
        // LIMPIAR SOLO MARCADORES USUARIO
        // ===============================
        map.overlays.removeAll {
            it is Marker
        }

        val marker =
            Marker(map)

        marker.position = point

        marker.title =
            "Mi ubicación exacta"

        marker.icon =
            getDrawable(
                android.R.drawable
                    .ic_menu_mylocation
            )

        map.overlays.add(marker)

        map.invalidate()
    }

    // ===============================
    // ACTUALIZAR BOLA DINAMICA
    // ===============================
    private fun updateTowerCircle(
        lat: Double,
        lon: Double,
        color: Int
    ) {

        if (towerCircle != null) {

            map.overlays.remove(towerCircle)
        }

        val circle =
            Polygon(map)

        circle.points =
            Polygon.pointsAsCircle(
                GeoPoint(lat, lon),
                30.0
            )

        circle.fillPaint.color = color
        circle.strokeColor = color
        circle.strokeWidth = 2f

        towerCircle = circle

        map.overlays.add(circle)

        currentTowerColor = color

        map.invalidate()
    }

    // ===============================
    // PDF COMPLETO
    // ===============================
    private fun generatePdf() {

        val pdf =
            PdfDocument()

        val pageInfo =
            PdfDocument.PageInfo.Builder(
                1200,
                2600,
                1
            ).create()

        val page =
            pdf.startPage(pageInfo)

        val canvas =
            page.canvas

        val titlePaint =
            Paint()

        titlePaint.color = Color.RED
        titlePaint.textSize = 40f

        val infoPaint =
            Paint()

        infoPaint.color = Color.WHITE
        infoPaint.textSize = 24f

        val greenPaint =
            Paint()

        greenPaint.color = Color.GREEN
        greenPaint.textSize = 22f

        val redPaint =
            Paint()

        redPaint.color = Color.RED
        redPaint.textSize = 22f

        val currentTime =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

        // ===============================
        // TITULO PDF
        // ===============================
        canvas.drawText(
            "ESCANER IMSI CATCHER",
            50f,
            60f,
            titlePaint
        )

        // ===============================
        // FECHA HORA
        // ===============================
        canvas.drawText(
            "FECHA Y HORA: $currentTime",
            50f,
            100f,
            infoPaint
        )

        // ===============================
        // UBICACION ACTUAL
        // ===============================
        canvas.drawText(
            "UBICACION ACTUAL: LAT $currentLat  LON $currentLon",
            50f,
            140f,
            infoPaint
        )

        // ===============================
        // MAPA PDF
        // ===============================
        try {

            val bitmap =
                Bitmap.createBitmap(
                    map.width,
                    map.height,
                    Bitmap.Config.ARGB_8888
                )

            val mapCanvas =
                Canvas(bitmap)

            map.draw(mapCanvas)

            val scaledBitmap =
                Bitmap.createScaledBitmap(
                    bitmap,
                    1000,
                    650,
                    false
                )

            canvas.drawBitmap(
                scaledBitmap,
                80f,
                200f,
                null
            )

        } catch (e: Exception) {

            e.printStackTrace()
        }

        drawCompactText(
            canvas,
            staticGreenData,
            50f,
            950f,
            greenPaint
        )

        if (redData.isNotEmpty()) {

            drawCompactText(
                canvas,
                redData,
                50f,
                1600f,
                redPaint
            )
        }

        pdf.finishPage(page)

        // ===============================
        // NOMBRE PDF
        // ===============================
        val fileDate =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())

        val fileName =
            "Escaner_Imsi_Catcher_${fileDate}.pdf"

        val documentsFolder =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            )

        if (!documentsFolder.exists()) {

            documentsFolder.mkdirs()
        }

        val file =
            File(
                documentsFolder,
                fileName
            )

        val output =
            FileOutputStream(file)

        pdf.writeTo(output)

        output.flush()

        output.close()

        pdf.close()
    }

    private fun drawCompactText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint
    ) {

        var yy = y

        text.split("\n").forEach {

            if (it.trim().isNotEmpty()) {

                canvas.drawText(
                    it,
                    x,
                    yy,
                    paint
                )

                yy += 30f
            }
        }
    }
}
