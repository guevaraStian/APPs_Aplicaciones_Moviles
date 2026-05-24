package com.example.bluetoothradar

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.location.LocationManager
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    lateinit var radarView: RadarView
    lateinit var txtDevices: TextView

    private var scanning = false

    private val bluetoothAdapter by lazy {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val scanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val handler = Handler(Looper.getMainLooper())

    // =========================
    // CALLBACK BLE
    // =========================
    private val callback = object : ScanCallback() {

        override fun onScanResult(type: Int, result: ScanResult) {

            val device = result.device
            val record = result.scanRecord

            val rssi = result.rssi
            val txPower = result.txPower

            val distanceMeters = calculateDistance(rssi, txPower)

            val distanceKm = (distanceMeters / 1000.0)
                .coerceIn(0.0, 10.0)
                .toFloat()

            val angle = Random.nextFloat() * 6.28f

            radarView.devices.add(
                RadarView.DevicePoint(
                    angle = angle,
                    distanceKm = distanceKm,
                    name = device.name ?: "Desconocido",
                    address = device.address
                )
            )

            radarView.invalidate()

            val info = """
                NAME: ${device.name ?: "N/A"}
                MAC: ${device.address}
                RSSI: $rssi
                TX POWER: $txPower
                DIST KM: $distanceKm
                UUID: ${record?.serviceUuids}
                MANUFACTURER: ${record?.manufacturerSpecificData}
            """.trimIndent()

            txtDevices.append("\n$info\n")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        radarView = findViewById(R.id.radarView)
        txtDevices = findViewById(R.id.txtDevices)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ),
            1
        )

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            startScan()
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopScan()
        }

        findViewById<Button>(R.id.btnPdf).setOnClickListener {
            exportPdf()
        }
    }

    // =========================
    // START SCAN
    // =========================
    private fun startScan() {

        if (scanning) return

        scanning = true
        scanner.startScan(callback)

        handler.postDelayed({
            if (scanning) {
                stopScan()
                startScan()
            }
        }, 10000)
    }

    // =========================
    // STOP SCAN
    // =========================
    private fun stopScan() {

        scanning = false
        scanner.stopScan(callback)
    }

    // =========================
    // DISTANCIA (BLE MODELO)
    // =========================
    private fun calculateDistance(rssi: Int, txPower: Int): Double {
        if (txPower == Int.MIN_VALUE) return 0.0
        return 10.0.pow((txPower - rssi) / 20.0)
    }

    // =========================
    // EXPORT PDF (MEJORADO)
    // =========================
    private fun exportPdf() {

        val snapshotList = radarView.devices.toList()

        // Captura pantalla (radar + UI)
        val bitmap = Bitmap.createBitmap(
            window.decorView.width,
            window.decorView.height,
            Bitmap.Config.ARGB_8888
        )

        val canvasBitmap = Canvas(bitmap)
        window.decorView.draw(canvasBitmap)

        val pdf = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(
            bitmap.width,
            bitmap.height,
            1
        ).create()

        val page = pdf.startPage(pageInfo)

        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val paint = Paint().apply {
            color = Color.RED
            textSize = 26f
        }

        // =========================
        // FECHA + GPS
        // =========================
        val date = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        var lat = "N/A"
        var lon = "N/A"

        try {
            val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc != null) {
                lat = loc.latitude.toString()
                lon = loc.longitude.toString()
            }
        } catch (_: Exception) {}

        // =========================
        // LISTA DE DISPOSITIVOS (EN PÁGINA 1)
        // =========================
        var y = bitmap.height - 450f

        canvas.drawText("=== BLUETOOTH DETECTADOS ===", 50f, y, paint)
        y += 40f

        for (d in snapshotList) {

            val line = "${d.name} | ${d.address} | ${d.distanceKm} km"

            canvas.drawText(line, 50f, y, paint)

            y += 35f

            if (y > bitmap.height - 100f) break
        }

        y += 40f
        canvas.drawText("FECHA: $date", 50f, y, paint); y += 35f
        canvas.drawText("GPS: $lat , $lon", 50f, y, paint); y += 35f
        canvas.drawText("TOTAL: ${snapshotList.size}", 50f, y, paint)

        pdf.finishPage(page)

        // =========================
        // PÁGINA 2 (LISTA COMPLETA)
        // =========================
        val page2Info = PdfDocument.PageInfo.Builder(
            bitmap.width,
            bitmap.height,
            2
        ).create()

        val page2 = pdf.startPage(page2Info)
        val canvas2 = page2.canvas

        val paint2 = Paint().apply {
            color = Color.RED
            textSize = 24f
        }

        var y2 = 80f

        canvas2.drawText("=== LISTA COMPLETA ===", 50f, y2, paint2)
        y2 += 40f

        for (d in snapshotList) {

            val line = "${d.name} | ${d.address} | ${d.distanceKm} km"

            canvas2.drawText(line, 50f, y2, paint2)

            y2 += 35f

            if (y2 > bitmap.height - 100f) {
                break
            }
        }

        pdf.finishPage(page2)

        // =========================
        // GUARDAR PDF
        // =========================
        val file = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            ),
            "Escaner_Bluetooth_$date.pdf"
        )

        pdf.writeTo(FileOutputStream(file))
        pdf.close()

        Toast.makeText(
            this,
            "PDF generado correctamente",
            Toast.LENGTH_LONG
        ).show()
    }
}