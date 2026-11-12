package com.dailybible.lock

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Home screen: today's progress, today's chapters and the lock status. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.continueButton).setOnClickListener { openReader() }
        findViewById<View>(R.id.settingsButton).setOnClickListener { openSettings() }
        findViewById<View>(R.id.permissionWarning).setOnClickListener { openSettings() }
    }

    override fun onResume() {
        super.onResume()
        if (!Prefs.isSetupDone(this)) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }
        LockService.start(this)
        render()
        // Refresh once more so the status line reflects the service after it has started.
        window.decorView.postDelayed({ if (!isFinishing && Prefs.isSetupDone(this)) render() }, 1500L)
    }

    private fun openReader() {
        startActivity(Intent(this, ReaderActivity::class.java))
    }

    private fun openSettings() {
        startActivity(Intent(this, SetupActivity::class.java).putExtra(SetupActivity.EXTRA_SETTINGS, true))
    }

    private fun render() {
        val target = Prefs.target(this)
        val done = Prefs.doneToday(this)
        val shown = done.coerceAtMost(target)
        val complete = done >= target
        val pointer = Prefs.pointer(this)

        findViewById<TextView>(R.id.streakText).text = Ar.days(Prefs.streak(this))
        findViewById<RingView>(R.id.ring).setProgress(shown, target)
        findViewById<TextView>(R.id.progressText).text = Ar.num(shown) + " / " + Ar.num(target)

        findViewById<TextView>(R.id.statusText).text =
            if (complete) "اكتملت قراءة اليوم"
            else "باقي " + Ar.chapters(target - done) + " لفتح التطبيقات"

        findViewById<TextView>(R.id.continueButton).text = when {
            complete -> "اقرأ أصحاحًا إضافيًا"
            done == 0 -> "ابدأ القراءة"
            else -> "كمّل القراءة"
        }

        // Today's chapters: the ones already read today, then the next ones in order.
        val container = findViewById<LinearLayout>(R.id.chaptersContainer)
        container.removeAllViews()
        for (i in 0 until target) {
            val index = pointer - done + i
            val item = layoutInflater.inflate(R.layout.item_chapter, container, false)
            val badge = item.findViewById<TextView>(R.id.itemBadge)
            val check = item.findViewById<ImageView>(R.id.itemCheck)
            val title = item.findViewById<TextView>(R.id.itemTitle)
            val sub = item.findViewById<TextView>(R.id.itemSub)
            val action = item.findViewById<TextView>(R.id.itemAction)

            title.text = Bible.title(index)
            badge.text = Ar.num(i + 1)
            val verseCount = try {
                Bible.verses(this, index).size
            } catch (e: Exception) {
                0
            }
            when {
                i < done -> {
                    check.visibility = View.VISIBLE
                    badge.visibility = View.INVISIBLE
                    sub.text = "تمّت القراءة"
                }
                i == done -> {
                    item.setBackgroundResource(R.drawable.bg_card_active)
                    badge.setBackgroundResource(R.drawable.bg_circle_yellow)
                    badge.setTextColor(getColor(R.color.brown))
                    sub.text = "التالي · " + Ar.verses(verseCount)
                    action.visibility = View.VISIBLE
                    action.setOnClickListener { openReader() }
                }
                else -> sub.text = Ar.verses(verseCount)
            }
            container.addView(item)
        }

        // Lock status card.
        val apps = LockedApps.enabled(this)
        val lockTitle = findViewById<TextView>(R.id.lockTitle)
        val lockIcon = findViewById<ImageView>(R.id.lockIcon)
        lockIcon.setImageResource(if (complete || apps.isEmpty()) R.drawable.ic_unlock else R.drawable.ic_lock)
        lockTitle.text = when {
            apps.isEmpty() -> "لا توجد تطبيقات مختارة للقفل"
            complete -> "التطبيقات مفتوحة الآن"
            apps.size == 1 -> "تطبيق واحد مقفول الآن"
            apps.size == 2 -> "تطبيقان مقفولان الآن"
            else -> Ar.num(apps.size) + " تطبيقات مقفولة الآن"
        }
        val lockApps = findViewById<TextView>(R.id.lockApps)
        lockApps.text = LockedApps.labels(this)
        lockApps.visibility = if (apps.isEmpty()) View.GONE else View.VISIBLE

        findViewById<View>(R.id.permissionWarning).visibility =
            if (Permissions.allGranted(this)) View.GONE else View.VISIBLE

        // Small status line to help find out why a lock did not appear.
        val alive = System.currentTimeMillis() - LockService.lastTick < 5000L
        var status = "خدمة القفل: " + (if (alive) "تعمل" else "متوقفة") +
            "\nآخر تطبيق تم رصده: " + (LockService.lastOtherPackage ?: "لا شيء")
        val error = LockService.lastError
        if (error != null) status += "\nخطأ: " + error
        findViewById<TextView>(R.id.debugStatus).text = status
    }
}
