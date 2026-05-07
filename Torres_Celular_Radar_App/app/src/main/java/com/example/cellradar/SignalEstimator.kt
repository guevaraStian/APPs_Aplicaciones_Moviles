package com.example.cellradar

import kotlin.math.pow

object SignalEstimator {

    fun estimateFromRsrp(rsrp: Int): Float {

        val normalized = (rsrp + 140).coerceIn(0, 90)

        // simulación física simplificada
        val distance = normalized.toDouble().pow(1.7) * 60.0

        return distance.toFloat().coerceIn(20f, 12000f)
    }
}