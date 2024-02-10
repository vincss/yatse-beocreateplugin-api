package tv.yatse.plugin.avreceiver.sample.helpers

import kotlin.math.roundToInt

class MockRemoteController : IRemoteController {

    private var mVolume = 0.0
    private var mMuted = false
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun getVolume(): Double {
        return mVolume
    }

    override fun setVolume(value: Double) {
        mVolume = value
    }

    override fun mute() {
        mMuted = true
    }

    override fun unmute() {
        mMuted = false
    }

    override val muted: Boolean
        get() = mMuted
    override var volume: Int
        get() = mVolume.roundToInt()
        set(value) {
            mVolume = value.toDouble()
        }
    override val isConnected: Boolean
        get() = true
}