package com.example

import com.example.data.DtcLibrary
import com.example.data.InmetroConsumption
import com.example.model.PreloadedVehicles
import com.example.obd.DtcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtcParserTest {

    @Test
    fun canSingleFrameWithCount() {
        val raw = "43 02 04 20 01 71 00 00\r"
        assertEquals(listOf("P0420", "P0171"), DtcParser.decodeDtcs(raw, "43", canWithCount = true))
    }

    @Test
    fun canNoCodes() {
        assertTrue(DtcParser.decodeDtcs("4300000000000000\r", "43", true).isEmpty())
        assertTrue(DtcParser.hasValidResponse("4300000000000000\r", "43"))
    }

    @Test
    fun canMultiFrameWithByteCountLine() {
        val raw = "SEARCHING...\r00A\r0: 43 04 01 33 01\r1: 34 02 01 03 01\r"
        assertEquals(
            listOf("P0133", "P0134", "P0201", "P0301"),
            DtcParser.decodeDtcs(raw, "43", true)
        )
    }

    @Test
    fun legacyProtocolWithoutCount() {
        assertEquals(listOf("P0133"), DtcParser.decodeDtcs("43 01 33 00 00 00 00\r", "43", false))
    }

    @Test
    fun noDataAndNegativeResponsesAreIgnored() {
        assertTrue(DtcParser.decodeDtcs("NO DATA\r", "43", true).isEmpty())
        assertFalse(DtcParser.hasValidResponse("7F0A11\r", "4A"))
    }

    @Test
    fun dtcTypeLetters() {
        assertEquals("U0100", DtcParser.decodePair(0xC1, 0x00))
        assertEquals("C0035", DtcParser.decodePair(0x40, 0x35))
        assertEquals("P0420", DtcParser.decodePair(0x04, 0x20))
    }

    @Test
    fun monitorStatusMilAndCount() {
        assertEquals(Pair(true, 2), DtcParser.parseMonitorStatus("41 01 82 07 E5 00\r"))
        assertEquals(Pair(false, 0), DtcParser.parseMonitorStatus("4101000765 00\r"))
        assertNull(DtcParser.parseMonitorStatus("NO DATA\r"))
    }

    @Test
    fun protocolDetection() {
        assertTrue(DtcParser.isCanProtocol("A6"))
        assertTrue(DtcParser.isCanProtocol("6"))
        assertFalse(DtcParser.isCanProtocol("A5"))
        assertFalse(DtcParser.isCanProtocol("3"))
    }

    @Test
    fun libraryHasKnownAndFallbackCodes() {
        assertTrue(DtcLibrary.isKnown("P0420"))
        assertFalse(DtcLibrary.isKnown("P1234"))
        assertNotNull(DtcLibrary.lookup("P1234").description)
    }

    @Test
    fun inmetroPicksMostSpecificMatch() {
        val cross = PreloadedVehicles.corollaCrossXre2025
        assertEquals(11.6f, InmetroConsumption.forProfile(cross).cityGas, 0.01f)
        val hybrid = PreloadedVehicles.corollaHybrid2025
        assertEquals(18.5f, InmetroConsumption.forProfile(hybrid).cityGas, 0.01f)
        val crossHybrid = PreloadedVehicles.corollaCrossHybrid2025
        assertEquals(17.7f, InmetroConsumption.forProfile(crossHybrid).cityGas, 0.01f)
    }

    @Test
    fun allProfilesHaveUniqueIds() {
        val ids = PreloadedVehicles.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
