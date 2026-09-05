package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

  @Test
  fun useAppContext() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
  }

  @Test
  fun vpnServiceIsDeclared() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val services = context.packageManager
      .getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SERVICES)
      .services
      .orEmpty()
    assertTrue(services.any { it.name.endsWith("MeelanoVpnService") })
  }
}
