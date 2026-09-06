package com.example

import com.example.data.repository.NodeSampler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the fix for the refresh that filled to 100% and then failed.
 *
 * Subscription feeds are append-ordered, so `take(400)` on a 22,000-line feed
 * tested only the oldest and deadest nodes. Every probe failed, the progress bar
 * completed, and the user saw "no node reachable" — while thousands of live
 * nodes further down the file were never tried.
 */
class NodeSamplerTest {

    @Test
    fun `returns everything when the pool is smaller than the sample`() {
        val items = (1..10).toList()
        assertEquals(items, NodeSampler.sampleEvenly(items, 50))
    }

    @Test
    fun `returns exactly the requested count`() {
        val items = (1..10_000).toList()
        assertEquals(900, NodeSampler.sampleEvenly(items, 900).size)
    }

    @Test
    fun `reaches the far end of the pool, not just the head`() {
        // The whole point of the fix: fresh nodes live at the bottom of a feed.
        val items = (1..22_669).toList()
        val sample = NodeSampler.sampleEvenly(items, 900)

        assertTrue("sample never left the head of the feed", sample.max() > 22_000)
        assertTrue("sample ignored the head of the feed", sample.min() < 100)
    }

    @Test
    fun `spreads across the pool rather than clustering`() {
        val items = (1..10_000).toList()
        val sample = NodeSampler.sampleEvenly(items, 100)

        // Each quarter of the source must be represented.
        listOf(0..2500, 2501..5000, 5001..7500, 7501..10000).forEach { quarter ->
            assertTrue(
                "no node sampled from $quarter",
                sample.any { it in quarter }
            )
        }
    }

    @Test
    fun `never indexes out of bounds at the boundary`() {
        // A stride that lands exactly on size would throw without the clamp.
        (1..40).forEach { n ->
            (1..40).forEach { k ->
                val out = NodeSampler.sampleEvenly((1..n).toList(), k)
                assertTrue(out.isNotEmpty())
                assertTrue(out.all { it in 1..n })
            }
        }
    }

    @Test
    fun `handles degenerate input`() {
        assertEquals(emptyList<Int>(), NodeSampler.sampleEvenly(emptyList<Int>(), 10))
        assertEquals(listOf(1, 2, 3), NodeSampler.sampleEvenly(listOf(1, 2, 3), 0))
    }
}
