package com.example.desktop

import com.example.desktop.ui.DesktopConnectionState
import com.example.desktop.ui.MeelanoColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the desktop palette to the phone's.
 *
 * The two builds resolve `Color` from different Compose artifacts, so the theme
 * cannot literally be shared; the hex values are the contract instead. If
 * someone retunes the Android palette without touching this file, these fail
 * and say so, rather than the Windows build quietly drifting to a different
 * brand.
 */
class DesktopThemeTest {

    @Test
    fun `canvas colours match the android theme`() {
        assertEquals(0xFF040A22, MeelanoColors.BgDark.value.toLong() ushr 32)
        assertEquals(0xFF090418, MeelanoColors.BgDarkSecondary.value.toLong() ushr 32)
        assertEquals(0xFF261149, MeelanoColors.BgMid.value.toLong() ushr 32)
        assertEquals(0xFF181331, MeelanoColors.SurfaceCard.value.toLong() ushr 32)
        assertEquals(0xFF40526F, MeelanoColors.SurfaceCardBorder.value.toLong() ushr 32)
    }

    @Test
    fun `the icon's two signature lights match`() {
        // These are sampled from the launcher artwork; they are the brand.
        assertEquals(0xFF1FEAF7, MeelanoColors.IconCyan.value.toLong() ushr 32)
        assertEquals(0xFFB44BFF, MeelanoColors.IconViolet.value.toLong() ushr 32)
        assertEquals(0xFFC9D2E6, MeelanoColors.Chrome.value.toLong() ushr 32)
    }

    @Test
    fun `status colours match`() {
        assertEquals(0xFF00E676, MeelanoColors.GreenSuccess.value.toLong() ushr 32)
        assertEquals(0xFFFF3B5C, MeelanoColors.RedKillSwitch.value.toLong() ushr 32)
        assertEquals(0xFFFFC53D, MeelanoColors.GoldVip.value.toLong() ushr 32)
    }

    @Test
    fun `ping colour thresholds match the phone build`() {
        assertEquals(MeelanoColors.TextMuted, MeelanoColors.forPing(0))
        assertEquals(MeelanoColors.PingGreen, MeelanoColors.forPing(80))
        assertEquals(MeelanoColors.PingYellow, MeelanoColors.forPing(200))
        assertEquals(MeelanoColors.PingOrange, MeelanoColors.forPing(400))
        assertEquals(MeelanoColors.PingRed, MeelanoColors.forPing(900))
    }

    @Test
    fun `only connecting and testing count as busy`() {
        assertTrue(DesktopConnectionState.CONNECTING.isBusy)
        assertTrue(DesktopConnectionState.TESTING.isBusy)
        assertFalse(DesktopConnectionState.CONNECTED.isBusy)
        assertFalse(DesktopConnectionState.DISCONNECTED.isBusy)
        assertFalse(DesktopConnectionState.FAILED.isBusy)
    }

    @Test
    fun `every state has a persian label`() {
        DesktopConnectionState.entries.forEach {
            assertTrue("missing label for $it", it.persian.isNotBlank())
        }
    }
}
