package com.dailybible.lock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the lock service after the phone reboots or the app is updated. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.isSetupDone(context)) {
            LockService.start(context)
        }
    }
}
