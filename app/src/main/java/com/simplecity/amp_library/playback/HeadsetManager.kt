package com.simplecity.amp_library.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager

class HeadsetManager(
    private val playbackManager: PlaybackManager,
    private val playbackSettingsManager: PlaybackSettingsManager
) {

    private var headsetReceiver: BroadcastReceiver? = null

    fun registerHeadsetPlugReceiver(context: Context) {

        val filter = IntentFilter()
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG)

        headsetReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                if (isInitialStickyBroadcast) {
                    return
                }

                handleHeadsetState(intent)
            }

            private fun handleHeadsetState(intent: Intent) {
                if (intent.hasExtra("state")) {
                    val state = intent.getIntExtra("state", 0)
                    when (state) {
                        0 -> handleHeadsetDisconnected()
                        1 -> handleHeadsetConnected()
                    }
                }
            }

            private fun handleHeadsetDisconnected() {
                if (playbackSettingsManager.pauseOnHeadsetDisconnect) {
                    playbackManager.pause(false)
                }
            }

            private fun handleHeadsetConnected() {
                if (playbackSettingsManager.playOnHeadsetConnect) {
                    playbackManager.play()
                }
            }
        }

        context.registerReceiver(headsetReceiver, filter)
    }


    fun unregisterHeadsetPlugReceiver(context: Context) {
        context.unregisterReceiver(headsetReceiver)
    }
}
