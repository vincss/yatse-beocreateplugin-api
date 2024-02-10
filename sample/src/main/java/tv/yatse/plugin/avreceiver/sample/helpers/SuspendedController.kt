package tv.yatse.plugin.avreceiver.sample.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


class SuspendedController(
        private val address: String,
        private val log: (tag: String, message: String) -> Unit)
    : IRemoteController {

    private var mController: SigmaTcpController? = null

    companion object {
        private const val TAG = "SuspendedController"
    }

    private fun setSuspended(function: () -> Unit) = runBlocking {
//        log(TAG, "-- setSuspended")
        withContext(Dispatchers.IO) {
            try {
                if (mController == null) connect()
                function()
            } catch (e: Exception) {
                try {
                    log(TAG, "ERROR setSuspended - retry ${e.message}")
                    connect()
                    function()
                } catch (e: Exception) {
                    log(TAG, "ERROR setSuspended - failed ${e.message}")
                }
            }
        }
    }

    private fun <T> getSuspended(function: () -> T, defaultValue: T): T = runBlocking {
        var value: T? = null
        withContext(Dispatchers.IO) {
            try {
                if (mController == null) connect()
//                log(TAG, "-- getSuspended $mController")
                value = function()
            } catch (e: Exception) {
                try {
                    log(TAG, "ERROR getSuspended - retry ${e.message}")
                    connect()
                    value = function()
                } catch (e: Exception) {
                    log(TAG, "ERROR getSuspended - failed ${e.message}")
                }
            }
        }
//        log(TAG, "-- getSuspended $value $mController")
        value ?: defaultValue
    }

    override fun connect() = runBlocking {
        withContext(Dispatchers.IO) {
            try {
                mController = SigmaTcpController(address, log)
                log(TAG, "-- Connected to $address $mController")
            } catch (e: Exception) {
                log(TAG, "-- Failed to connect to $address : ${e.message}")
                mController = null
            }
        }
    }


    override fun close() {
        mController?.close()
        mController = null
    }

    override fun getVolume(): Double {
        return getSuspended({ mController?.getVolume() ?: 0.0 }, 0.0)
    }

    override fun setVolume(value: Double) {
        setSuspended { mController?.setVolume(value) }
    }

    override fun mute() {
        setSuspended { mController?.mute() }
    }

    override fun unmute() {
        setSuspended { mController?.unmute() }
    }

    override val muted: Boolean
        get() = getSuspended({ mController?.muted ?: false }, false)
    override var volume: Int
        get() = getSuspended({ mController?.volume ?: 0 }, 0)
        set(value) {
            setSuspended { mController?.volume = value }
        }
    override val isConnected: Boolean
        get() = getSuspended({ mController?.isConnected ?: false }, false)

}