package com.dailybible.lock

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Guides the user through the two permissions the lock needs, one at a time, with a
 * moving picture of the switch to turn on. Android does not let an app grant these to
 * itself, so the screen opens the right settings page, watches for the switch being
 * turned on, and then brings the app back by itself.
 */
class PermissionActivity : AppCompatActivity() {

    companion object {
        private const val NONE = 0
        private const val OVERLAY = 1
        private const val USAGE = 2
        private const val POLL_MS = 400L
        private const val MAX_POLLS = 450 // about three minutes
    }

    private val handler = Handler(Looper.getMainLooper())
    private var current = NONE // the permission still missing
    private var waitingFor = NONE // the permission the user went to settings for
    private var polls = 0
    private var demoOn = false

    /** While the user is in the phone settings: come back as soon as the switch is on. */
    private val poll = object : Runnable {
        override fun run() {
            val granted = when (waitingFor) {
                OVERLAY -> Permissions.hasOverlay(this@PermissionActivity)
                USAGE -> Permissions.hasUsageAccess(this@PermissionActivity)
                else -> false
            }
            if (waitingFor == NONE) return
            if (granted) {
                waitingFor = NONE
                bringBack()
            } else if (polls++ < MAX_POLLS) {
                handler.postDelayed(this, POLL_MS)
            }
        }
    }

    /** Moves the picture's switch on and off, like a short repeating clip. */
    private val demo = object : Runnable {
        override fun run() {
            demoOn = !demoOn
            val track = findViewById<View>(R.id.demoTrack)
            val thumb = findViewById<View>(R.id.demoThumb)
            val travel = 22f * resources.displayMetrics.density
            val direction = if (track.layoutDirection == View.LAYOUT_DIRECTION_RTL) -1f else 1f
            thumb.animate().translationX(if (demoOn) travel * direction else 0f).setDuration(250L).start()
            track.setBackgroundResource(if (demoOn) R.drawable.bg_pill_brown else R.drawable.bg_pill_off)
            handler.postDelayed(this, if (demoOn) 1700L else 900L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        findViewById<View>(R.id.permButton).setOnClickListener { onButton() }
        // Running the service keeps the app alive while the user is in the phone settings,
        // so it can notice the switch and come back. It locks nothing until setup is done.
        LockService.start(this)
        if (Prefs.isSetupDone(this)) {
            findViewById<View>(R.id.stepText).visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(poll)
        waitingFor = NONE
        render()
        handler.removeCallbacks(demo)
        handler.postDelayed(demo, 700L)
    }

    override fun onPause() {
        handler.removeCallbacks(demo)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun render() {
        val overlay = Permissions.hasOverlay(this)
        val usage = Permissions.hasUsageAccess(this)
        current = if (!overlay) OVERLAY else if (!usage) USAGE else NONE

        renderChip(R.id.chip1, "١ · فوق التطبيقات", overlay, current == OVERLAY)
        renderChip(R.id.chip2, "٢ · استخدام التطبيقات", usage, current == USAGE)

        val done = current == NONE
        findViewById<View>(R.id.stepCard).visibility = if (done) View.GONE else View.VISIBLE
        findViewById<View>(R.id.doneBox).visibility = if (done) View.VISIBLE else View.GONE
        findViewById<View>(R.id.permNote).visibility = if (done) View.INVISIBLE else View.VISIBLE

        val appName = getString(R.string.app_name)
        val title = findViewById<TextView>(R.id.permTitle)
        val why = findViewById<TextView>(R.id.permWhy)
        val steps = findViewById<TextView>(R.id.permSteps)
        if (current == OVERLAY) {
            title.text = "الإذن ١ من ٢: الظهور فوق التطبيقات"
            why.text = "ليعرض التطبيق شاشة القفل فوق التطبيق المقفول."
            steps.text = "١. اضغط الزر الأصفر «افتح الإعدادات» بالأسفل.\n" +
                "٢. ابحث في القائمة عن «" + appName + "» واضغط عليه.\n" +
                "٣. شغّل المفتاح كما في الصورة المتحركة.\n" +
                "اسم الصفحة في الهاتف: Display over other apps أو Appear on top أو الظهور فوق التطبيقات."
        } else if (current == USAGE) {
            title.text = "الإذن ٢ من ٢: الوصول إلى استخدام التطبيقات"
            why.text = "ليعرف التطبيق متى يُفتح تطبيق مقفول."
            steps.text = "١. اضغط الزر الأصفر «افتح الإعدادات» بالأسفل.\n" +
                "٢. ابحث في القائمة عن «" + appName + "» واضغط عليه.\n" +
                "٣. شغّل المفتاح كما في الصورة المتحركة.\n" +
                "اسم الصفحة في الهاتف: Usage access أو Permit usage access أو الوصول إلى بيانات الاستخدام."
        }

        findViewById<TextView>(R.id.permButton).text = when {
            !done -> "افتح الإعدادات"
            Prefs.isSetupDone(this) -> "تم"
            else -> "ابدأ القراءة"
        }
    }

    private fun renderChip(viewId: Int, label: String, granted: Boolean, isCurrent: Boolean) {
        val chip = findViewById<TextView>(viewId)
        chip.text = if (granted) "تم · " + label else label
        chip.setBackgroundResource(
            when {
                granted -> R.drawable.bg_pill_brown
                isCurrent -> R.drawable.bg_preset_on
                else -> R.drawable.bg_preset
            }
        )
        chip.setTextColor(getColor(if (granted) R.color.yellow_bright else R.color.brown))
    }

    private fun onButton() {
        when (current) {
            OVERLAY -> openSettings(Permissions.overlayIntents(this), OVERLAY)
            USAGE -> openSettings(Permissions.usageIntents(this), USAGE)
            else -> finishFlow()
        }
    }

    private fun openSettings(intents: List<Intent>, which: Int) {
        var opened = false
        for (intent in intents) {
            try {
                startActivity(intent)
                opened = true
                break
            } catch (e: Exception) {
                // This phone has no such page; try the next intent.
            }
        }
        if (!opened) {
            Toast.makeText(this, "افتح إعدادات الهاتف وفعّل الإذن يدويًا", Toast.LENGTH_LONG).show()
            return
        }
        waitingFor = which
        polls = 0
        handler.removeCallbacks(poll)
        handler.postDelayed(poll, 800L)
    }

    /** Returns to this screen from the phone settings once the switch has been turned on. */
    private fun bringBack() {
        val intent = Intent(this, PermissionActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // The phone did not allow it; the user comes back with the back button.
        }
    }

    private fun finishFlow() {
        if (Prefs.isSetupDone(this)) {
            finish()
            return
        }
        // First setup is complete: the lock starts now.
        Prefs.setSetupDone(this)
        LockService.start(this)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
