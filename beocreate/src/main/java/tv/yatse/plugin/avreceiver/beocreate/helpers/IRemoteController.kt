package tv.yatse.plugin.avreceiver.beocreate.helpers

interface IRemoteController {

    fun connect()
    fun close()

    fun getVolume(): Double
    fun setVolume(value: Double)
    fun mute()
    fun unmute()

    var muted: Boolean

    var volume: Int
    val isConnected: Boolean
}