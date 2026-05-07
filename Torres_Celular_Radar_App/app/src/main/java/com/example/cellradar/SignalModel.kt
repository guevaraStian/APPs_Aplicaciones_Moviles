package com.example.cellradar

import kotlin.math.pow
import kotlin.math.log10

object SignalModel {

    /**
     * Estimación PASIVA de distancia (no real física exacta)
     * basada en pérdida de señal (Free Space Path Loss simplificado)
     */
    fun estimateDistance(dbm: Int, frequencyMHz: Double = 1800.0): Float {

        val txPower = -30.0
        val pathLoss = txPower - dbm

        val distanceKm = 10.0.pow(
            (pathLoss - 32.44 - 20 * log10(frequencyMHz)) / 20.0
        )

        return (distanceKm * 1000).toFloat().coerceIn(10f, 5000f)
    }
}