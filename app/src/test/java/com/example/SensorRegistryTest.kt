package com.example

import com.example.model.SensorRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorRegistryTest {

    private fun decode(id: String, vararg bytes: Int): Float? =
        SensorRegistry.byId.getValue(id).decode(bytes)

    @Test
    fun idsEPidsSaoUnicos() {
        val ids = SensorRegistry.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        val pids = SensorRegistry.standard.map { it.pid }
        assertEquals(pids.size, pids.toSet().size)
    }

    @Test
    fun conversoesDeUmByte() {
        assertEquals(25f, decode("iat", 65)!!, 0.001f)          // 65 - 40
        assertEquals(0f, decode("stft_b1", 128)!!, 0.001f)
        assertEquals(50f, decode("stft_b1", 192)!!, 0.001f)     // (192-128)*100/128
        assertEquals(0f, decode("timing", 128)!!, 0.001f)       // 128/2 - 64
        assertEquals(98f, decode("baro", 98)!!, 0.001f)
        assertEquals(300f, decode("fuel_pressure", 100)!!, 0.001f)
    }

    @Test
    fun conversoesDeDoisBytes() {
        assertEquals(1f, decode("o2_wr_b1s1_lambda", 0x80, 0x00)!!, 0.001f)
        assertEquals(13.7f, decode("ctrl_voltage", 0x35, 0x84)!!, 0.001f)  // 13700 mV
        assertEquals(12.5f, decode("maf", 0x04, 0xE2)!!, 0.001f)           // 1250 / 100
        assertEquals(-1f, decode("evap_pressure", 0xFF, 0xFC)!!, 0.001f)   // -4 / 4 (com sinal)
        assertEquals(60f, decode("cat_temp_b1s1", 0x03, 0xE8)!!, 0.001f)   // 1000/10 - 40
    }

    @Test
    fun bytesInsuficientesDevolvemNull() {
        assertNull(decode("maf", 0x04))
        assertNull(decode("iat"))
    }

    @Test
    fun afrEDerivadoECalculado() {
        val afr = SensorRegistry.byId.getValue(SensorRegistry.AFR_ID)
        assertEquals(-1, afr.pid)
        assertTrue(afr.origin.marker.isNotBlank())
    }
}
