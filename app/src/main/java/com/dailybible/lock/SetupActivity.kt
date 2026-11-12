package com.dailybible.lock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Step 2 of first setup: choose the apps to lock. Opened again later as the settings
 * screen with EXTRA_SETTINGS = true, where it also shows the daily number and permissions.
 */
class SetupActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SETTINGS = "settings"
    }

    private var settingsMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        settingsMode = intent.getBooleanExtra(EXTRA_SETTINGS, false)

        bindRow(R.id.rowFacebook, R.id.checkFacebook, R.id.stateFacebook, "facebook")
        bindRow(R.id.rowInstagram, R.id.checkInstagram, R.id.stateInstagram, "instagram")
        bindRow(R.id.rowTwitter, R.id.checkTwitter, R.id.stateTwitter, "twitter")

        findViewById<View>(R.id.countChange).setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java).putExtra(WelcomeActivity.EXTRA_EDIT, true))
        }
        findViewById<View>(R.id.permOpen).setOnClickListener {
            startActivity(Intent(this, PermissionActivity::class.java))
        }

        val start = findViewById<TextView>(R.id.startButton)
        if (settingsMode) {
            findViewById<View>(R.id.stepText).visibility = View.GONE
            findViewById<View>(R.id.countRow).visibility = View.VISIBLE
            findViewById<View>(R.id.permRow).visibility = View.VISIBLE
            start.text = "تم"
        }
        start.setOnClickListener { onStartClicked() }

        // Android 13+ needs permission to show the service notification.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!settingsMode) return

        val today = Prefs.target(this)
        val next = Prefs.nextTarget(this)
        findViewById<TextView>(R.id.countValue).text =
            if (next == today) Ar.chapters(today) + " كل يوم"
            else Ar.chapters(next) + " · يبدأ من الغد"

        findViewById<TextView>(R.id.permStatus).text =
            if (Permissions.allGranted(this)) "مفعّلة" else "غير مفعّلة، والقفل لا يعمل"

        // While today's reading is not finished an app can be added to the lock but not
        // removed from it, otherwise the lock could be switched off instead of reading.
        findViewById<TextView>(R.id.appsHint).text =
            if (Prefs.isComplete(this)) "اضغط على التطبيق لتحديده أو لإلغاء تحديده."
            else "يمكنك إضافة تطبيق للقفل الآن. إلغاء القفل متاح بعد إكمال قراءة اليوم."
    }

    /** One app row: a filled check means the app will be locked. Tap to change. */
    private fun bindRow(rowId: Int, checkId: Int, stateId: Int, appId: String) {
        val check = findViewById<ImageView>(checkId)
        val state = findViewById<TextView>(stateId)

        fun render() {
            val locked = Prefs.isAppLocked(this, appId)
            check.setBackgroundResource(if (locked) R.drawable.bg_circle_brown else R.drawable.bg_circle_outline)
            if (locked) check.setImageResource(R.drawable.ic_check) else check.setImageDrawable(null)
            state.text = if (locked) "سيُقفل" else "اضغط للتحديد"
        }

        findViewById<View>(rowId).setOnClickListener {
            val locked = Prefs.isAppLocked(this, appId)
            if (settingsMode && locked && !Prefs.isComplete(this)) {
                // Not allowed to unlock an app before today's reading is done.
                Toast.makeText(this, "أكمل قراءة اليوم أولًا لإلغاء قفل التطبيق", Toast.LENGTH_LONG).show()
            } else {
                Prefs.setAppLocked(this, appId, !locked)
                render()
            }
        }
        render()
    }

    private fun onStartClicked() {
        if (settingsMode) {
            finish()
            return
        }
        if (LockedApps.enabled(this).isEmpty()) {
            Toast.makeText(this, "اختر تطبيقًا واحدًا على الأقل للقفل", Toast.LENGTH_LONG).show()
            return
        }
        // Step 3: the guided permission screen finishes the setup and starts the lock.
        startActivity(Intent(this, PermissionActivity::class.java))
    }
}
