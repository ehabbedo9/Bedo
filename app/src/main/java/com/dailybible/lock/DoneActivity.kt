package com.dailybible.lock

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Shown when today's reading is complete and the apps are unlocked. */
class DoneActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_done)

        findViewById<TextView>(R.id.doneSummary).text = "قرأت اليوم " + Ar.chapters(Prefs.doneToday(this))
        val labels = LockedApps.labels(this)
        findViewById<TextView>(R.id.doneApps).text = if (labels.isEmpty()) "لا توجد تطبيقات مختارة" else labels
        findViewById<TextView>(R.id.doneStreak).text = Ar.days(Prefs.streak(this))
        findViewById<TextView>(R.id.doneNext).text = "القراءة القادمة تبدأ من " + Bible.title(Prefs.pointer(this))

        findViewById<View>(R.id.doneButton).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
        findViewById<View>(R.id.extraButton).setOnClickListener {
            startActivity(Intent(this, ReaderActivity::class.java))
            finish()
        }
    }
}
