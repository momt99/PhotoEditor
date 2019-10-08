package com.felan.photoeditor.widgets.filters

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CurvesValue {
    companion object {
        private val curveGranularity = 100
        private val curveDataStep = 2
    }

    var blacksLevel = 0.0f
    var shadowsLevel = 25.0f
    var midtonesLevel = 50.0f
    var highlightsLevel = 75.0f
    var whitesLevel = 100.0f

    var previousBlacksLevel = 0.0f
    var previousShadowsLevel = 25.0f
    var previousMidtonesLevel = 50.0f
    var previousHighlightsLevel = 75.0f
    var previousWhitesLevel = 100.0f

    lateinit var cachedDataPoints: FloatArray

    val dataPoints: FloatArray
        get() {
            if (!this::cachedDataPoints.isInitialized) {
                interpolateCurve()
            }
            return cachedDataPoints
        }

    val isDefault: Boolean
        get() =
            abs(blacksLevel - 0) < 0.00001 && abs(shadowsLevel - 25) < 0.00001 && abs(midtonesLevel - 50) < 0.00001 && abs(
                highlightsLevel - 75
            ) < 0.00001 && abs(whitesLevel - 100) < 0.00001

    fun saveValues() {
        previousBlacksLevel = blacksLevel
        previousShadowsLevel = shadowsLevel
        previousMidtonesLevel = midtonesLevel
        previousHighlightsLevel = highlightsLevel
        previousWhitesLevel = whitesLevel
    }

    fun restoreValues() {
        blacksLevel = previousBlacksLevel
        shadowsLevel = previousShadowsLevel
        midtonesLevel = previousMidtonesLevel
        highlightsLevel = previousHighlightsLevel
        whitesLevel = previousWhitesLevel
        interpolateCurve()
    }

    fun interpolateCurve(): FloatArray {
        val points = floatArrayOf(
            -0.001f,
            blacksLevel / 100.0f,
            0.0f,
            blacksLevel / 100.0f,
            0.25f,
            shadowsLevel / 100.0f,
            0.5f,
            midtonesLevel / 100.0f,
            0.75f,
            highlightsLevel / 100.0f,
            1f,
            whitesLevel / 100.0f,
            1.001f,
            whitesLevel / 100.0f
        )

        val dataPoints = ArrayList<Float>(100)
        val interpolatedPoints = ArrayList<Float>(100)

        interpolatedPoints.add(points[0])
        interpolatedPoints.add(points[1])

        for (index in 1 until points.size / 2 - 2) {
            val point0x = points[(index - 1) * 2]
            val point0y = points[(index - 1) * 2 + 1]
            val point1x = points[index * 2]
            val point1y = points[index * 2 + 1]
            val point2x = points[(index + 1) * 2]
            val point2y = points[(index + 1) * 2 + 1]
            val point3x = points[(index + 2) * 2]
            val point3y = points[(index + 2) * 2 + 1]


            for (i in 1 until curveGranularity) {
                val t = i.toFloat() * (1.0f / curveGranularity.toFloat())
                val tt = t * t
                val ttt = tt * t

                val pix =
                    0.5f * (2 * point1x + (point2x - point0x) * t + (2 * point0x - 5 * point1x + 4 * point2x - point3x) * tt + (3 * point1x - point0x - 3 * point2x + point3x) * ttt)
                var piy =
                    0.5f * (2 * point1y + (point2y - point0y) * t + (2 * point0y - 5 * point1y + 4 * point2y - point3y) * tt + (3 * point1y - point0y - 3 * point2y + point3y) * ttt)

                piy = max(0f, min(1f, piy))

                if (pix > point0x) {
                    interpolatedPoints.add(pix)
                    interpolatedPoints.add(piy)
                }

                if ((i - 1) % curveDataStep == 0) {
                    dataPoints.add(piy)
                }
            }
            interpolatedPoints.add(point2x)
            interpolatedPoints.add(point2y)
        }
        interpolatedPoints.add(points[12])
        interpolatedPoints.add(points[13])

        cachedDataPoints = FloatArray(dataPoints.size)
        for (a in cachedDataPoints.indices) {
            cachedDataPoints[a] = dataPoints[a]
        }
        val retValue = FloatArray(interpolatedPoints.size)
        for (a in retValue.indices) {
            retValue[a] = interpolatedPoints[a]
        }
        return retValue
    }
}

class CurvesToolValue {

    var luminanceCurve = CurvesValue()
    var redCurve = CurvesValue()
    var greenCurve = CurvesValue()
    var blueCurve = CurvesValue()
    var curveBuffer: ByteBuffer

    var activeType: CurveType =
        CurveType.LUMINANCE

    init {
        curveBuffer = ByteBuffer.allocateDirect(200 * 4)
        curveBuffer.order(ByteOrder.LITTLE_ENDIAN)
    }

    fun fillBuffer() {
        curveBuffer.position(0)
        val luminanceCurveData = luminanceCurve.dataPoints
        val redCurveData = redCurve.dataPoints
        val greenCurveData = greenCurve.dataPoints
        val blueCurveData = blueCurve.dataPoints
        for (a in 0..199) {
            curveBuffer.put((redCurveData[a] * 255).toByte())
            curveBuffer.put((greenCurveData[a] * 255).toByte())
            curveBuffer.put((blueCurveData[a] * 255).toByte())
            curveBuffer.put((luminanceCurveData[a] * 255).toByte())
        }
        curveBuffer.position(0)
    }

    fun shouldBeSkipped(): Boolean {
        return luminanceCurve.isDefault && redCurve.isDefault && greenCurve.isDefault && blueCurve.isDefault
    }

    enum class CurveType(val value: Int) {
        LUMINANCE(0),
        RED(1),
        GREEN(2),
        BLUE(3)
    }
}