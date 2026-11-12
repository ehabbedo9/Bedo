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
 * Step 2 of first setup (apps to lock + permissions). Opened again later as the
 * settings screen with EXTRA_SETTINGS = true.
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

        findViewById<View>(R.id.overlayButton).setOnClickListener { open(Permissions.overlayIntent(this)) }
        findViewById<View>(R.id.usageButton).setOnClickListener { open(Permissions.usageIntent()) }
        findViewById<View>(R.id.countChange).setOnClickListener {
            startActivity(Intent(this, WelcomeActivity::class.java).putExtra(WelcomeActivity.EXTRA_EDIT, true))
        }

        val start = findViewById<TextView>(R.id.startButton)
        if (settingsMode) {
            findViewById<View>(R.id.stepText).visibility = View.GONE
            findViewById<View>(R.id.countRow).visibility = View.VISIBLE
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
        renderPermission(R.id.overlayButton, Permissions.hasOverlay(this))
        renderPermission(R.id.usageButton, Permissions.hasUsageAccess(this))

        if (settingsMode) {
            val today = Prefs.target(this)
            val next = Prefs.nextTarget(this)
            findViewById<TextView>(R.id.countValue).text =
                if (next == today) Ar.chapters(today) + " كل يوم"
                else Ar.chapters(next) + " · يبدأ من الغد"

            // While today's reading is not finished an app can be added to the lock but not
            // removed from it, otherwise the lock could be switched off instead of reading.
            findViewById<TextView>(R.id.appsHint).text =
                if (Prefs.isComplete(this)) "تُقفل هذه التطبيقات كل يوم إلى أن تكمل قراءتك."
                else "يمكنك إضافة تطبيق للقفل الآن. إلغاء القفل متاح بعد إكمال قراءة اليوم."
        }
    }

    /** One app row: a filled check means the app will be locked. Tap to change. */
    private fun bindRow(rowId: Int, checkId: Int, stateId: Int, appId: String) {
        val check = findViewById<ImageView>(checkId)
        val state = findViewById<TextView>(stateId)

        fun render() {
            val locked = Prefs.isAppLocked(this, appId)
            check.setBackgroundResource(if (locked) R.drawable.bg_circle_brown else R.drawable.bg_circle_outline)
            if (locked) check.setImageResource(R.drawable.ic_check) else check.setImageDrawable(null)
            state.text = if (locked) "سيُقفل" else "غير مقفول"
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

    private fun renderPermission(viewId: Int, granted: Boolean) {
        val button = findViewById<TextView>(viewId)
        button.text = if (granted) "مفعّل" else "تفعيل"
        button.isEnabled = !granted
        button.setBackgroundResource(if (granted) R.drawable.bg_soft else R.drawable.bg_btn_yellow)
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "افتح إعدادات الهاتف وفعّل الإذن يدويًا", Toast.LENGTH_LONG).show()
        }
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
        if (!Permissions.allGranted(this)) {
            Toast.makeText(this, "فعّل الإذنين أولًا ليعمل القفل", Toast.LENGTH_LONG).show()
            return
        }
        Prefs.setSetupDone(this)
        LockService.start(this)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
