package com.example.cellradar

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.provider.MediaStore
import android.telephony.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var radar: RadarView
    private lateinit var table: TextView
    private lateinit var latTxt: TextView
    private lateinit var lonTxt: TextView
    private lateinit var dateTxt: TextView

    private lateinit var btnScan: Button
    private lateinit var btnStop: Button
    private lateinit var btnExport: Button

    private var scanning = false

    private val handler = Handler(Looper.getMainLooper())

    private var lastData = ""

    private val scanRunnable = object : Runnable {
        override fun run() {

            if (scanning) {
                scan()
                handler.postDelayed(this, 5000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        radar = findViewById(R.id.radarView)
        table = findViewById(R.id.txtTable)

        latTxt = findViewById(R.id.txtLat)
        lonTxt = findViewById(R.id.txtLon)
        dateTxt = findViewById(R.id.txtDateTime)

        btnScan = findViewById(R.id.btnScan)
        btnStop = findViewById(R.id.btnStop)
        btnExport = findViewById(R.id.btnExport)

        requestPermissions()
        startGPS()
        updateTime()

        btnScan.setOnClickListener {
            startScanning()
        }

        btnStop.setOnClickListener {
            stopScanning()
        }

        btnExport.setOnClickListener {
            exportPDF()
        }
    }

    // ================= INICIAR ESCANEO =================

    private fun startScanning() {

        if (!scanning) {

            scanning = true
            handler.post(scanRunnable)

            Toast.makeText(
                this,
                "Escaneo iniciado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ================= DETENER ESCANEO =================

    private fun stopScanning() {

        scanning = false
        handler.removeCallbacks(scanRunnable)

        Toast.makeText(
            this,
            "Escaneo detenido",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ================= ESCANEAR =================

    private fun scan() {

        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        val cells = tm.allCellInfo ?: return

        val list = mutableListOf<RadarView.Signal>()

        val sb = StringBuilder()

        cells.filterIsInstance<CellInfoLte>()
            .sortedByDescending { it.cellSignalStrength.rsrp }
            .forEachIndexed { i, c ->

                val rsrp = c.cellSignalStrength.rsrp
                val rsrq = c.cellSignalStrength.rsrq
                val dbm = c.cellSignalStrength.dbm
                val asu = c.cellSignalStrength.asuLevel

                val ci = c.cellIdentity.ci
                val tac = c.cellIdentity.tac
                val pci = c.cellIdentity.pci
                val earfcn = c.cellIdentity.earfcn
                val bandwidth = c.cellIdentity.bandwidth

                val dist = estimate(rsrp)

                val angle = (i * 45f) % 360f

                val name = "LTE-$i"

                list.add(
                    RadarView.Signal(
                        name,
                        dist,
                        angle
                    )
                )

                sb.append("""
ANTENA: $name
DISTANCIA: $dist m
CI: $ci
TAC: $tac
PCI: $pci
EARFCN: $earfcn
BANDWIDTH: $bandwidth
RSRP: $rsrp
RSRQ: $rsrq
RSSI: $dbm
ASU: $asu

""")
            }

        lastData = sb.toString()

        radar.update(list)

        table.text = lastData

        updateTime()
    }

    // ================= ESTIMAR DISTANCIA =================

    private fun estimate(rsrp: Int): Int {

        val tx = -30

        val n = 2.2

        return (
                10.0.pow(
                    (tx - rsrp) / (10 * n)
                )
                ).toInt().coerceIn(1, 7000)
    }

    // ================= FECHA =================

    private fun updateTime() {

        dateTxt.text =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())
    }

    // ================= EXPORTAR PDF =================

    private fun exportPDF() {

        val time =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())

        val lat =
            latTxt.text.toString()
                .replace("LAT: ", "")
                .replace(".", "_")

        val lon =
            lonTxt.text.toString()
                .replace("LON: ", "")
                .replace(".", "_")

        val fileName =
            "Radar_Antenas_4G_${time}_LAT_${lat}_LON_${lon}.pdf"

        val values = ContentValues().apply {

            put(
                MediaStore.Downloads.DISPLAY_NAME,
                fileName
            )

            put(
                MediaStore.Downloads.MIME_TYPE,
                "application/pdf"
            )

            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "Download/"
            )
        }

        val uri =
            contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            )

        uri?.let {

            val out: OutputStream =
                contentResolver.openOutputStream(it)!!

            val pdf = PdfDocument()

            // =====================================================
            // NUEVO:
            // SEPARAR TODO EL CONTENIDO EN MUCHAS LINEAS/PAGINAS
            // PARA MOSTRAR TODAS LAS ANTENAS
            // =====================================================

            val allLines = lastData.split("\n")

            val pageWidth = 1200
            val pageHeight = 2200

            var currentPage = 1

            var pageInfo =
                PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    currentPage
                ).create()

            var page = pdf.startPage(pageInfo)

            var canvas = page.canvas

            val paint = Paint()

            paint.color = Color.RED
            paint.textSize = 24f
            paint.isAntiAlias = true

            var y = 60

            // ================= TITULO =================

            canvas.drawText(
                "RADAR ANTENAS 4G",
                420f,
                y.toFloat(),
                paint
            )

            y += 50

            // ================= FECHA =================

            canvas.drawText(
                dateTxt.text.toString(),
                40f,
                y.toFloat(),
                paint
            )

            y += 40

            // ================= LAT =================

            canvas.drawText(
                latTxt.text.toString(),
                40f,
                y.toFloat(),
                paint
            )

            y += 35

            // ================= LON =================

            canvas.drawText(
                lonTxt.text.toString(),
                40f,
                y.toFloat(),
                paint
            )

            y += 60

            // ================= RADAR =================

            val bitmap =
                Bitmap.createBitmap(
                    radar.width,
                    radar.height,
                    Bitmap.Config.ARGB_8888
                )

            val radarCanvas = Canvas(bitmap)

            radar.draw(radarCanvas)

            val scaled =
                Bitmap.createScaledBitmap(
                    bitmap,
                    1000,
                    700,
                    true
                )

            canvas.drawBitmap(
                scaled,
                100f,
                y.toFloat(),
                null
            )

            y += 760

            // ================= TABLA COMPLETA =================

            paint.textSize = 18f

            for (line in allLines) {

                // =================================================
                // NUEVO:
                // SI LA PAGINA SE LLENA,
                // CREAR OTRA PAGINA AUTOMATICAMENTE
                // =================================================

                if (y > 2100) {

                    pdf.finishPage(page)

                    currentPage++

                    pageInfo =
                        PdfDocument.PageInfo.Builder(
                            pageWidth,
                            pageHeight,
                            currentPage
                        ).create()

                    page = pdf.startPage(pageInfo)

                    canvas = page.canvas

                    paint.color = Color.RED
                    paint.textSize = 18f

                    y = 60

                    canvas.drawText(
                        "CONTINUACION DATOS ANTENAS",
                        40f,
                        y.toFloat(),
                        paint
                    )

                    y += 50
                }

                canvas.drawText(
                    line,
                    40f,
                    y.toFloat(),
                    paint
                )

                y += 24
            }

            // =====================================================
            // FINALIZAR ULTIMA PAGINA
            // =====================================================

            pdf.finishPage(page)

            pdf.writeTo(out)

            pdf.close()

            out.close()

            Toast.makeText(
                this,
                "PDF generado correctamente con TODAS las antenas",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ================= GPS =================

    override fun onLocationChanged(location: Location) {

        latTxt.text = "LAT: ${location.latitude}"

        lonTxt.text = "LON: ${location.longitude}"
    }

    // ================= PERMISOS =================

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

    // ================= GPS =================

    private fun startGPS() {

        val lm =
            getSystemService(
                LOCATION_SERVICE
            ) as LocationManager

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        lm.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2000,
            2f,
            this
        )
    }
}