package tv.yatse.plugin.avreceiver.sample.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SuspendedController(address: String, val log: (tag: String, message: String) -> Unit) :
    IRemoteController {

    private var mController: IRemoteController? = null

    companion object {
        private const val TAG = "SuspendedController"
    }

    init {
        initController(address)
    }

    private fun initController(address: String) = runBlocking {
        withContext(Dispatchers.IO) {
            mController = SigmaTcpController(address)
            log(TAG, "---- Connected to $address")
        }
    }

    private fun setSuspended(function: () -> Unit) = runBlocking {
        withContext(Dispatchers.IO) {
            function()
        }
    }

    private fun <T> getSuspended(function: () -> T): T = runBlocking {
        var value: T
        withContext(Dispatchers.IO) {
            value = function()
        }
        value
    }

    override fun getVolume(): Double {
        return getSuspended { mController!!.getVolume() }
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
        get() = getSuspended { mController!!.muted }
    override var volume: Int
        get() = getSuspended { mController!!.volume }
        set(value) {
            setSuspended { mController!!.volume = value }
        }
    override val isConnected: Boolean
        get() = getSuspended { mController!!.isConnected }

}