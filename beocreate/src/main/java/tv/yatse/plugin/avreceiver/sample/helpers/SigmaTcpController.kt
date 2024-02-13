package tv.yatse.plugin.avreceiver.sample.helpers

import java.math.BigInteger
import java.net.Socket
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}

class SigmaTcpController(
    private val address: String,
    private val log: (tag: String, message: String) -> Unit,
    private val port: Int = DefaultPort
) : IRemoteController {
    companion object {
        private const val TAG = "SigmaTcpController"

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

    private var tcpClient: Socket? = null

    init {
        connect()
    }

    override fun connect() {
        tcpClient = Socket(address, port)
        tcpClient?.tcpNoDelay = true
        log(TAG, "--- connected ${address}:$port")
    }

    override fun close() {
        tcpClient?.close()
    }

    override fun getVolume(): Double {
        val volume = decimalVal(readMemory(getVolumeAddress()))
        log(TAG, "--- getVolume $volume")
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
        log(TAG, "--- setVolume $value")
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
        val readNbr = tcpClient?.getInputStream()?.read(rcvData, 0, rcvData.size)

        if (rcvData[0] != CommandMetaDataResponse.toByte()) {
            println("WrongHeader")
        }

        val txt = rcvData.copyOfRange(HeaderSize, readNbr!!).toString(Charsets.UTF_8)
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

        tcpClient?.getOutputStream()?.write(packet, 0, packet.size)
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

        tcpClient?.getOutputStream()?.write(data, 0, HeaderSize)

        val rcvData = ByteArray(HeaderSize + decimalLength)
        tcpClient?.getInputStream()?.read(rcvData, 0, rcvData.size)
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

        tcpClient?.getOutputStream()?.write(packet)
    }

    override var volume: Int
        get() {
            return (getVolume() * 100).roundToInt()
        }
        set(value) {
            setVolume((value.toDouble() / 100))
        }

    override val isConnected: Boolean
        get() {
            return tcpClient?.isConnected ?: false
        }
}
