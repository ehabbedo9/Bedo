package com.dailybible.lock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
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

        bindSwitch(R.id.switchFacebook, "facebook")
        bindSwitch(R.id.switchInstagram, "instagram")
        bindSwitch(R.id.switchTwitter, "twitter")

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

            // While today's reading is not finished the list of locked apps cannot be changed,
            // otherwise the lock could be switched off instead of reading.
            val editable = Prefs.isComplete(this)
            findViewById<View>(R.id.switchFacebook).isEnabled = editable
            findViewById<View>(R.id.switchInstagram).isEnabled = editable
            findViewById<View>(R.id.switchTwitter).isEnabled = editable
            findViewById<TextView>(R.id.appsHint).text =
                if (editable) "تُقفل هذه التطبيقات كل يوم إلى أن تكمل قراءتك."
                else "يمكن تعديل التطبيقات بعد إكمال قراءة اليوم."
        }
    }

    private fun bindSwitch(viewId: Int, appId: String) {
        val switch = findViewById<SwitchCompat>(viewId)
        switch.isChecked = Prefs.isAppLocked(this, appId)
        switch.setOnCheckedChangeListener { _, checked -> Prefs.setAppLocked(this, appId, checked) }
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
