package com.example

import com.example.data.repository.GeoLabeler
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoLabelerTest {

    @Test
    fun `detects country from flag emoji in remark`() {
        assertEquals("🇩🇪", GeoLabeler.of("node1.example.net", "🇩🇪 Frankfurt 01").flag)
    }

    @Test
    fun `detects country from city keyword`() {
        assertEquals("فنلاند", GeoLabeler.of("edge.provider.net", "Helsinki-Fast").country)
    }

    @Test
    fun `detects country from cctld`() {
        assertEquals("ترکیه", GeoLabeler.of("node.example.tr", "").country)
    }

    @Test
    fun `falls back to unknown`() {
        assertEquals("🌐", GeoLabeler.of("198.51.100.7", "").flag)
    }
}
