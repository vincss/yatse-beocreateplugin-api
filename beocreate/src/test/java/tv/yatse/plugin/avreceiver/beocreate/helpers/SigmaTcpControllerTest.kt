package tv.yatse.plugin.avreceiver.beocreate.helpers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class SigmaTcpControllerTest {


    @Ignore("Manual testing")
    @Test
    fun manualTesting() {
//        val sigmaTcpHelper = SigmaTcpController("192.168.0.4")
        val sigmaTcpHelper = SuspendedController("192.168.0.4") { _, _ -> run {} }

        sigmaTcpHelper.mute()
        var muted = sigmaTcpHelper.muted
        assertTrue(muted)

        sigmaTcpHelper.unmute()
        muted = sigmaTcpHelper.muted
        assertFalse(muted)


        sigmaTcpHelper.volume = 33
        val volume = sigmaTcpHelper.volume
        assertTrue(volume == 33)
        val vol = sigmaTcpHelper.getVolume()
        assertTrue(vol == 0.33)

        sigmaTcpHelper.setVolume(0.1)
        assertTrue(sigmaTcpHelper.getVolume() == 0.1)

        sigmaTcpHelper.setVolume(0.05)
        assertTrue(sigmaTcpHelper.getVolume() == 0.05)


    }

    @Test
    fun sigmaConverter_intData() {
        val dictionary = mapOf(
            1 to byteArrayOf(0, 0, 0, 1),
            0 to byteArrayOf(0, 0, 0, 0),
            16777216 to byteArrayOf(1, 0, 0, 0),
            167772 to byteArrayOf(0, 2, -113, 92),
            5536481 to byteArrayOf(0, 84, 122, -31)
        )

        for ((value, expected) in dictionary) {
            val dataValue = SigmaTcpController.Companion.intData(value)
            assertTrue(dataValue.contentEquals(expected))
        }
    }

    @Test
    fun sigmaConverter_decimalRepr() {
        val dictionary = mapOf(
            0 to 0,
            1 to 16777216,
            0.01 to 167772.16,
            0.33 to 5536481.28,
            0.05 to 838860.8
        )

        for ((value, expected) in dictionary) {
            val dataValue = SigmaTcpController.Companion.decimalRepr(value.toDouble())
            val expectedDouble = expected.toDouble()
            assertTrue(dataValue == expectedDouble)
        }
    }

    @Test
    fun sigmaConverter_decimalVal() {
        val dictionary = mapOf(
            0.05 to byteArrayOf(0, 12, -52, -52),
            0.1 to byteArrayOf(0, 25, -103, -103),
            0.33 to byteArrayOf(0, 84, 122, -31)
        )

        for ((expected, value) in dictionary) {
            val dataValue = SigmaTcpController.Companion.decimalVal(value).roundTo(2)
            assertTrue(dataValue == expected)
        }
    }

    @Test
    fun sigmaConverter() {
        val dictionary = mapOf(
            0x80000000 to -128,
            0xFF000000 to -1,
            0 to 0,
            0x40000000 to 64,
            0x1000000 to 1,
            0x10000 to 0.00390625,
        )

        for ((key, value) in dictionary) {
            val keyValue = SigmaTcpController.Companion.decimalRepr(value.toDouble())
            val dataValue = SigmaTcpController.Companion.decimalVal(key.toInt())
            assertTrue(keyValue == key.toDouble())
            assertTrue(dataValue == value.toDouble())
        }
    }


}
