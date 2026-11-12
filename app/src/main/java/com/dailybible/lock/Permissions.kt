package com.dailybible.lock

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

object Permissions {

    private fun opAllowed(c: Context, op: String): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(op, Process.myUid(), c.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(op, Process.myUid(), c.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** "Display over other apps". Some phones report it late, so both sources are checked. */
    fun hasOverlay(c: Context): Boolean =
        Settings.canDrawOverlays(c) || opAllowed(c, AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW)

    /** "Usage access". */
    fun hasUsageAccess(c: Context): Boolean = opAllowed(c, AppOpsManager.OPSTR_GET_USAGE_STATS)

    fun allGranted(c: Context): Boolean = hasOverlay(c) && hasUsageAccess(c)

    /** Intents to try in order: this app's own page first, then the general list. */
    fun overlayIntents(c: Context): List<Intent> = listOf(
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + c.packageName)),
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
    )

    fun usageIntents(c: Context): List<Intent> = listOf(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + c.packageName)),
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    )
}
