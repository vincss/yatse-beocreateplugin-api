package tv.yatse.plugin.avreceiver.beocreate.helpers

import android.util.Log
import kotlin.math.roundToInt

class MockRemoteController(private val mMediaCenterIp: String) : IRemoteController {

    private var mVolume = 0.0
    private var mMuted = false

    companion object {
        private const val TAG = "MockRemoteController"
    }

    override fun connect() {
        Log.i(TAG, "-- Connected $mMediaCenterIp")
    }

    override fun close() {
        Log.i(TAG, "-- close")
    }

    override fun getVolume(): Double {
        Log.i(TAG, "-- getVolume $mVolume")
        return mVolume
    }

    override fun setVolume(value: Double) {
        mVolume = value
        Log.i(TAG, "-- setVolume $mVolume")
    }

    override fun mute() {
        Log.i(TAG, "-- mute")
        mMuted = true
    }

    override fun unmute() {
        Log.i(TAG, "-- unmute")
        mMuted = false
    }

    override var muted: Boolean
        get() = mMuted
        set(value) {
            mMuted = value
        }
    override var volume: Int
        get() = mVolume.roundToInt()
        set(value) {
            Log.i(TAG, "-- volume $value")
            mVolume = value.toDouble()
        }
    override val isConnected: Boolean
        get() = true
}