package com.dailybible.lock

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * First screen after install: choose how many chapters to read each day.
 * Also opened from settings (EXTRA_EDIT) to change the number from the next day.
 */
class WelcomeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EDIT = "edit"
        private const val STATE_COUNT = "count"
    }

    private var count = Prefs.DEFAULT_TARGET
    private var editMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        editMode = intent.getBooleanExtra(EXTRA_EDIT, false)
        count = savedInstanceState?.getInt(STATE_COUNT)
            ?: if (editMode) Prefs.nextTarget(this) else Prefs.DEFAULT_TARGET

        findViewById<View>(R.id.plusButton).setOnClickListener { setCount(count + 1) }
        findViewById<View>(R.id.minusButton).setOnClickListener { setCount(count - 1) }
        findViewById<View>(R.id.preset1).setOnClickListener { setCount(1) }
        findViewById<View>(R.id.preset3).setOnClickListener { setCount(3) }
        findViewById<View>(R.id.preset5).setOnClickListener { setCount(5) }
        findViewById<View>(R.id.preset7).setOnClickListener { setCount(7) }

        findViewById<View>(R.id.startChange).setOnClickListener {
            startActivity(Intent(this, PickerActivity::class.java))
        }

        val next = findViewById<TextView>(R.id.nextButton)
        if (editMode) {
            findViewById<View>(R.id.stepText).visibility = View.GONE
            findViewById<TextView>(R.id.noteText).text = "يبدأ العدد الجديد من اليوم التالي."
            next.text = "حفظ"
        }
        next.setOnClickListener {
            if (editMode) {
                Prefs.setTarget(this, count, false)
                finish()
            } else {
                Prefs.setTarget(this, count, true)
                startActivity(Intent(this, SetupActivity::class.java))
            }
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        // The picker may have changed where reading starts.
        findViewById<TextView>(R.id.startValue).text = Bible.title(Prefs.pointer(this))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_COUNT, count)
    }

    private fun setCount(value: Int) {
        count = value.coerceIn(1, Prefs.MAX_TARGET)
        render()
    }

    private fun render() {
        findViewById<TextView>(R.id.countText).text = Ar.num(count)
        findViewById<TextView>(R.id.unitText).text = Ar.chapterUnit(count)
        val presets = intArrayOf(R.id.preset1, R.id.preset3, R.id.preset5, R.id.preset7)
        val values = intArrayOf(1, 3, 5, 7)
        for (i in presets.indices) {
            findViewById<View>(presets[i]).setBackgroundResource(
                if (values[i] == count) R.drawable.bg_preset_on else R.drawable.bg_preset
            )
        }
    }
}
