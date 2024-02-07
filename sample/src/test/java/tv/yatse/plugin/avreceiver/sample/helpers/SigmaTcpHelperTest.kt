package tv.yatse.plugin.avreceiver.sample.helpers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SigmaTcpHelperTest {
  @Test
  fun hellWorld() {
    val sigmaTcpHelper = SigmaTcpHelper()
    val ret = sigmaTcpHelper.helloWorld()
    assertTrue(ret === "Hello^World")
  }

}
