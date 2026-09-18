package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AdBlockRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AdBlock VPN", appName)
  }

  @Test
  fun `test domain blocking logic`() {
    val (isUnityBlocked, _) = AdBlockRepository.isDomainBlocked("unityads.unity3d.com")
    assertTrue("Unity Ads should be blocked", isUnityBlocked)

    val (isGoogleAdBlocked, _) = AdBlockRepository.isDomainBlocked("doubleclick.net")
    assertTrue("DoubleClick should be blocked", isGoogleAdBlocked)

    val (isLegitBlocked, _) = AdBlockRepository.isDomainBlocked("wikipedia.org")
    assertEquals("Legitimate site should not be blocked", false, isLegitBlocked)
  }
}
