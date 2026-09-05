package com.example

import com.example.data.model.RoutingMode
import com.example.vpn.net.RouteTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTableTest {

    @Test
    fun `global mode installs default route`() {
        val routes = RouteTable.routesFor(RoutingMode.GLOBAL)
        assertEquals(1, routes.size)
        assertEquals("0.0.0.0", routes[0].address)
        assertEquals(0, routes[0].prefix)
    }

    @Test
    fun `direct mode installs nothing`() {
        assertTrue(RouteTable.routesFor(RoutingMode.DIRECT).isEmpty())
    }

    @Test
    fun `smart bypass excludes iranian ranges but keeps foreign ones`() {
        val routes = RouteTable.routesFor(RoutingMode.SMART_BYPASS)
        assertTrue(routes.isNotEmpty())

        // An Iranian IP must NOT be routed into the tunnel.
        assertFalse(RouteTable.contains(routes, "5.112.10.20"))   // 5.112.0.0/12
        assertFalse(RouteTable.contains(routes, "151.240.5.5"))   // 151.240.0.0/12
        assertFalse(RouteTable.contains(routes, "10.0.0.5"))      // private
        assertFalse(RouteTable.contains(routes, "127.0.0.1"))     // loopback

        // Foreign IPs must be tunnelled.
        assertTrue(RouteTable.contains(routes, "8.8.8.8"))
        assertTrue(RouteTable.contains(routes, "1.1.1.1"))
        assertTrue(RouteTable.contains(routes, "142.250.185.78"))
    }

    @Test
    fun `ip conversion round trips`() {
        val value = RouteTable.ipToLong("192.168.1.10")!!
        assertEquals("192.168.1.10", RouteTable.longToIp(value))
    }

    @Test
    fun `complement of full space is empty`() {
        val complement = RouteTable.complementOf(listOf(RouteTable.Cidr("0.0.0.0", 0)))
        assertTrue(complement.isEmpty())
    }
}
