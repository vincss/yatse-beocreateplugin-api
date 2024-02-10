package tv.yatse.plugin.avreceiver.sample.helpers

interface IRemoteController {

    fun connect()
    fun close()

    fun getVolume(): Double
    fun setVolume(value: Double)
    fun mute()
    fun unmute()

    val muted: Boolean

    var volume: Int
    val isConnected: Boolean
}