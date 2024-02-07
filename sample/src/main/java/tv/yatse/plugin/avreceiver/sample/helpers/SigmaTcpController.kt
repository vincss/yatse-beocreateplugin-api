package tv.yatse.plugin.avreceiver.sample.helpers

import java.math.BigInteger
import java.net.Socket
import kotlin.math.pow
import kotlin.math.roundToInt

fun Double.roundTo(n: Int): Double {
    return "%.${n}f".format(this).toDouble()
}

interface IRemoteController {
    fun getVolume(): Double
    fun setVolume(value: Double)
    fun mute()
    fun unmute()

    val muted: Boolean

    var volume: Int
    val isConnected: Boolean
}

class SigmaTcpController(address: String, port: Int = DefaultPort) : IRemoteController {
    companion object {
        const val DefaultPort = 8086
        const val HeaderSize = 14
        const val DecimalLength = 4
        const val AttributeVolumeControl = "volumeControlRegister"
        const val AttributeMuteRegister = "muteRegister"
        const val CommandReadMemory = 0x0a
        const val CommandWriteMemory = 0x09
        const val CommandGetMetaData = 0xf8
        const val CommandMetaDataResponse = 0xf9
        private val LSB_SIGMA = 1.0f / 2.0.pow(23)

        fun intData(intval: Int, length: Int = DecimalLength): ByteArray {
            val octets = ByteArray(length)
            for (i in length downTo 1) {
                octets[length - i] = ((intval shr (i - 1) * 8) and 0xff).toByte()
            }
            return octets
        }

        fun decimalVal(p: Any): Double {
            if (p is ByteArray) {
                var value = 0
                p.forEach { octet ->
                    value = value shl 8
                    value += octet.toInt() and 0xff
                }
                return value.toDouble() / 2.0.pow(24)
//                return (f * 1000.0) / 1000.0
            }

            var f = (p as Int).toDouble() / 2.0.pow(24)

            if (f >= 128) {
                f += -256
            }

            return f
        }

        fun decimalRepr(f: Double): Double {
            if (f > 256 - LSB_SIGMA || f < -256) {
                throw Exception("Value $f not in range [-16,16]")
            }

            var value = f
            if (value < 0) {
                value += 256
            }

            value *= 2.0.pow(24)
            return value
        }
    }

    private var volumeAddress: Int? = null
    private var muteAddress: Int? = null
    private var internalVolume: Int? = null

    private val tcpClient = Socket(address, port)

    init {
        tcpClient.tcpNoDelay = true
    }

    override fun getVolume(): Double {
        val volume = decimalVal(readMemory(getVolumeAddress()))
        return (volume).roundTo(2)
    }

    private fun getVolumeAddress(): Int {
        if (volumeAddress == null) {
            sendCommandGetMetaData(AttributeVolumeControl)
            volumeAddress = receiveMetaDataAddress()
        }
        return volumeAddress!!
    }

    override fun setVolume(value: Double) {
        writeMemory(getVolumeAddress(), value)
    }

    override fun mute() {
        setMute(true)
    }

    override fun unmute() {
        setMute(false)
    }

    override val muted: Boolean
        get() {
            if (muteAddress == null) {
                sendCommandGetMetaData(AttributeMuteRegister)
                muteAddress = receiveMetaDataAddress()
            }
            val muteVal = BigInteger(readMemory(muteAddress!!)).toInt()
            return muteVal == 1
        }

    private fun setMute(mute: Boolean) {
        if (muteAddress == null) {
            sendCommandGetMetaData(AttributeMuteRegister)
            muteAddress = receiveMetaDataAddress()
        }
        writeMemory(muteAddress!!, if (mute) 1 else 0)
    }

    private fun receiveMetaDataAddress(): Int {
        val rcvData = ByteArray(256)
        val readNbr = tcpClient.getInputStream().read(rcvData, 0, rcvData.size)

        if (rcvData[0] != CommandMetaDataResponse.toByte()) {
            println("WrongHeader")
        }

        val txt = rcvData.copyOfRange(HeaderSize, readNbr).toString(Charsets.UTF_8)
        return txt.toInt()
    }

    private fun sendCommandGetMetaData(attribute: String) {
        val length = (HeaderSize + attribute.length).toByte()

        val header = ByteArray(HeaderSize)
        header[0] = CommandGetMetaData.toByte()
        header[3] = ((length.toInt() shr 8) and 0xff).toByte()
        header[4] = (length.toInt() and 0xff).toByte()
        val attributeByte = attribute.toByteArray(Charsets.UTF_8)
        val packet = header + attributeByte

        tcpClient.getOutputStream().write(packet, 0, packet.size)
    }

    private fun readMemory(addr: Int, decimalLength: Int = DecimalLength): ByteArray {
        val data = ByteArray(HeaderSize)
        val length = decimalLength.toByte()
        data[0] = CommandReadMemory.toByte()
        data[4] = HeaderSize.toByte()
        data[9] = length
        data[8] = ((length.toInt() shr 8) and 0xff).toByte()
        data[11] = (addr and 0xff).toByte()
        data[10] = ((addr shr 8) and 0xff).toByte()

        tcpClient.getOutputStream().write(data, 0, HeaderSize)

        val rcvData = ByteArray(HeaderSize + decimalLength)
        tcpClient.getInputStream().read(rcvData, 0, rcvData.size)
        return rcvData.copyOfRange(HeaderSize, rcvData.size)
    }

    private fun writeMemory(addressToWrite: Int, value: Double) {
        val valueToSend = decimalRepr(value)
        writeMemory(addressToWrite, valueToSend.toInt())
    }

    private fun writeMemory(addr: Int, value: Int) {
        val data = intData(value)
        val length = data.size
        val header = ByteArray(HeaderSize)
        header[0] = CommandWriteMemory.toByte()
        header[11] = (length and 0xff).toByte()
        header[10] = ((length shr 8) and 0xff).toByte()
        header[13] = (addr and 0xff).toByte()
        header[12] = ((addr shr 8) and 0xff).toByte()

        val packet = header + data

        val packetLength = packet.size
        packet[6] = (packetLength and 0xff).toByte()
        packet[5] = ((packetLength shr 8) and 0xff).toByte()

        tcpClient.getOutputStream().write(packet)
    }

    override var volume: Int
        get() {
            if (internalVolume == null) {
                val vol = getVolume()
                internalVolume = (vol * 100).roundToInt()
            }
            return internalVolume!!
        }
        set(value) {
            internalVolume = value
            setVolume((value.toDouble() / 100))
        }

    override val isConnected: Boolean
        get() {
            return tcpClient.isConnected
        }
}
