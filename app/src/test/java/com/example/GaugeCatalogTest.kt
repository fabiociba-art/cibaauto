package com.example

import com.example.model.GaugeCatalog
import com.example.model.SensorRegistry
import com.example.model.TelemetryData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GaugeCatalogTest {

    @Test
    fun idsSaoUnicosETodosOsSensoresDoRegistroEstaoNoCatalogo() {
        val ids = GaugeCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        SensorRegistry.all.forEach { assertTrue(it.id, it.id in GaugeCatalog.byId) }
    }

    @Test
    fun manometrosOriginaisExistemNoCatalogo() {
        assertEquals(GaugeCatalog.SLOT_COUNT, GaugeCatalog.DEFAULT_SLOTS.size)
        GaugeCatalog.DEFAULT_SLOTS.forEach { assertTrue(it in GaugeCatalog.byId) }
    }

    @Test
    fun sanitizeSubstituiIdsInvalidosPeloOriginal() {
        val r = GaugeCatalog.sanitize(listOf("nao_existe", "iat"))
        assertEquals(GaugeCatalog.SLOT_COUNT, r.size)
        assertEquals(GaugeCatalog.DEFAULT_SLOTS[0], r[0])
        assertEquals("iat", r[1])
        assertEquals(GaugeCatalog.DEFAULT_SLOTS[2], r[2])
        assertEquals(GaugeCatalog.DEFAULT_SLOTS, GaugeCatalog.sanitize(null))
    }

    @Test
    fun semLeituraDevolveNull() {
        val t = TelemetryData(coolantTempC = Float.NaN, batteryVoltage = 0f, sensors = emptyMap())
        assertNull(GaugeCatalog.byId.getValue("coolant_temp").valueOf(t))
        assertNull(GaugeCatalog.byId.getValue("battery12").valueOf(t))
        assertNull(GaugeCatalog.byId.getValue("iat").valueOf(t))
    }

    @Test
    fun comLeituraDevolveOValorEFormata() {
        val t = TelemetryData(coolantTempC = 90f, batteryVoltage = 13.8f, sensors = mapOf("iat" to 31f))
        assertEquals(90f, GaugeCatalog.byId.getValue("coolant_temp").valueOf(t)!!, 0.001f)
        assertEquals(31f, GaugeCatalog.byId.getValue("iat").valueOf(t)!!, 0.001f)
        assertEquals("90°C", GaugeCatalog.byId.getValue("coolant_temp").format(90f))
        assertEquals("100 kPa", GaugeCatalog.byId.getValue("map").format(100f))
    }
}
