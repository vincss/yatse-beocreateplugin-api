/*
 * Copyright 2015 Tolriq / Genimee.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package tv.yatse.plugin.avreceiver.sample

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import tv.yatse.plugin.avreceiver.api.AVReceiverPluginService
import tv.yatse.plugin.avreceiver.api.PluginCustomCommand
import tv.yatse.plugin.avreceiver.api.YatseLogger
import tv.yatse.plugin.avreceiver.sample.helpers.IRemoteController
import tv.yatse.plugin.avreceiver.sample.helpers.PreferencesHelper
import tv.yatse.plugin.avreceiver.sample.helpers.SuspendedController
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Sample AVReceiverPluginService that implement all functions with dummy code that displays Toast and logs to main Yatse log system.
 *
 *
 * See [AVReceiverPluginService] for documentation on all functions
 */
class AVPluginService : AVReceiverPluginService() {
    private val handler = Handler(Looper.getMainLooper())
    private var mHostUniqueId: String? = null
    private var mHostName: String? = null
    private var mHostIp: String? = null
    private var mController: IRemoteController? = null
    private val mVolumeIncrement = 1

    private var mVolumePercent = 50.0
    private var mIsMuted = false

    override fun onDestroy() {
        Log.d(TAG, "- onDestroy")
        if (mController != null) {
            mController?.close()
            mController = null
        }
    }

    override fun getVolumeUnitType(): Int {
        return UNIT_TYPE_PERCENT
    }

    override fun getVolumeMinimalValue(): Double {
        return 0.0
    }

    override fun getVolumeMaximalValue(): Double {
        return 100.0
    }

    override fun setMuteStatus(status: Boolean): Boolean {
        YatseLogger.logVerbose(applicationContext, TAG, "Setting mute status: $status")
        displayToast("Setting mute status : $status")

        mIsMuted = if (status) {
            mController?.mute()
            true

        } else {
            mController?.unmute()
            false
        }

        return true
    }

    override fun getMuteStatus(): Boolean {
        mIsMuted = mController?.muted == true
        return mIsMuted
    }

    override fun toggleMuteStatus(): Boolean {
        YatseLogger.logVerbose(applicationContext, TAG, "Toggling mute status")
        displayToast("Toggling mute status")

        setMuteStatus(!getMuteStatus())

        return true
    }

    override fun setVolumeLevel(volume: Double): Boolean {
        YatseLogger.logVerbose(applicationContext, TAG, "Setting volume level: $volume")
        displayToast("Setting volume: $volume")

        Log.d(TAG, "- setVolumeLevel newVolume:$volume currentVolume:$mVolumePercent")

        mController?.volume = volume.roundToInt()
        mVolumePercent = volume
        return true
    }

    override fun getVolumeLevel(): Double {
        mVolumePercent = mController?.volume?.toDouble() ?: 0.0
        Log.d(TAG, "- getVolumeLevel volume:$mVolumePercent")
        return mVolumePercent
    }

    override fun volumePlus(): Boolean {
        mVolumePercent = min(100.0, getVolumeLevel() + mVolumeIncrement)
        mController?.volume = mVolumePercent.roundToInt()

        YatseLogger.logVerbose(applicationContext, TAG, "Calling volume plus")
        displayToast("Volume plus: $mVolumePercent")
        return true
    }

    override fun volumeMinus(): Boolean {
        mVolumePercent = max(0.0, getVolumeLevel() - mVolumeIncrement)
        mController?.volume = mVolumePercent.roundToInt()

        YatseLogger.logVerbose(applicationContext, TAG, "Calling volume minus")
        displayToast("Volume minus: $mVolumePercent")
        return true
    }

    override fun refresh(): Boolean {
        getMuteStatus()
        getVolumeLevel()

        Log.d(TAG, "- refresh mVolumePercent:$mVolumePercent mIsMuted:$mIsMuted")

        YatseLogger.logVerbose(applicationContext, TAG, "Refreshing values from receiver")
        return true
    }

    override fun getDefaultCustomCommands(): List<PluginCustomCommand> {
        return ArrayList()
    }

    override fun executeCustomCommand(customCommand: PluginCustomCommand?): Boolean {
        YatseLogger.logVerbose(
                applicationContext,
                TAG,
                "Executing CustomCommand: ${customCommand!!.title}"
        )
        displayToast(customCommand.param1)
        return false
    }

    private fun displayToast(message: String?) {
        handler.post { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
    }

    override fun connectToHost(uniqueId: String?, name: String?, ip: String?) {
        mHostUniqueId = uniqueId
        mHostName = name
        mHostIp = ip
        val receiverIp = PreferencesHelper.getInstance(applicationContext).hostIp(mHostUniqueId!!)
        if (TextUtils.isEmpty(receiverIp)) {
            YatseLogger.logError(applicationContext, TAG, "No configuration for $name")
        }

        val address = receiverIp.ifBlank { mHostIp }
        Log.d(TAG, "- connectingToHost $address")
        mController = SuspendedController(address!!.trim(), Log::d)
        mController?.connect()
        refresh()

        YatseLogger.logVerbose(
                applicationContext, TAG, "Connected to: $name/$mHostUniqueId"
        )
    }

    override fun getSettingsVersion(): Long {
        return PreferencesHelper.getInstance(applicationContext).settingsVersion()
    }

    override fun getSettings(): String {
        return PreferencesHelper.getInstance(applicationContext).settingsAsJSON
    }

    override fun restoreSettings(settings: String?, version: Long): Boolean {
        val result = PreferencesHelper.getInstance(applicationContext)
                .importSettingsFromJSON(settings!!, version)
        if (result) {
            connectToHost(mHostUniqueId, mHostName, mHostIp)
        }
        return result
    }

    companion object {
        private const val TAG = "AVPluginService"
    }
}