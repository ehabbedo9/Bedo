package com.dailybible.lock

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.View
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Shows the next chapter. The chapter only counts after the reader has scrolled to
 * its end AND stayed on it for a minimum time based on its length.
 */
class ReaderActivity : AppCompatActivity() {

    companion object {
        private const val STATE_INDEX = "index"
        private const val STATE_ELAPSED = "elapsed"
        private const val STATE_REACHED = "reached"
        private const val SECONDS_PER_WORD = 0.25
        private const val MIN_SECONDS = 30
        private const val MAX_SECONDS = 300
        private val FONT_SIZES = floatArrayOf(20f, 23f, 27f)
    }

    private lateinit var scroll: ScrollView
    private lateinit var chapterText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var verseStatus: TextView
    private lateinit var timeStatus: TextView
    private lateinit var finishButton: TextView
    private lateinit var finishHint: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var index = 0
    private var verseStarts = IntArray(0)
    private var requiredSeconds = MIN_SECONDS
    private var elapsedSeconds = 0
    private var reachedEnd = false
    private var currentVerse = 1

    private val timer = object : Runnable {
        override fun run() {
            elapsedSeconds++
            updateStatus()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        scroll = findViewById(R.id.scroll)
        chapterText = findViewById(R.id.chapterText)
        progress = findViewById(R.id.scrollProgress)
        verseStatus = findViewById(R.id.verseStatus)
        timeStatus = findViewById(R.id.timeStatus)
        finishButton = findViewById(R.id.finishButton)
        finishHint = findViewById(R.id.finishHint)

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
        findViewById<View>(R.id.fontButton).setOnClickListener {
            Prefs.setFontStep(this, (Prefs.fontStep(this) + 1) % FONT_SIZES.size)
            applyFontSize()
        }
        finishButton.setOnClickListener { onFinishClicked() }

        scroll.setOnScrollChangeListener { _, _, _, _, _ -> onScrolled() }
        chapterText.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> onScrolled() }

        applyFontSize()
        if (savedInstanceState != null && savedInstanceState.getInt(STATE_INDEX, -1) == Prefs.pointer(this)) {
            elapsedSeconds = savedInstanceState.getInt(STATE_ELAPSED, 0)
            reachedEnd = savedInstanceState.getBoolean(STATE_REACHED, false)
        }
        loadChapter()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(timer)
        handler.postDelayed(timer, 1000L)
    }

    override fun onPause() {
        handler.removeCallbacks(timer)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_INDEX, index)
        outState.putInt(STATE_ELAPSED, elapsedSeconds)
        outState.putBoolean(STATE_REACHED, reachedEnd)
    }

    private fun applyFontSize() {
        val step = Prefs.fontStep(this).coerceIn(0, FONT_SIZES.size - 1)
        chapterText.setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZES[step])
    }

    private fun loadChapter() {
        index = Prefs.pointer(this)
        val verses = Bible.verses(this, index)
        val numberColor = getColor(R.color.verse_num)

        val text = SpannableStringBuilder()
        verseStarts = IntArray(verses.size)
        var words = 0
        for (i in verses.indices) {
            verseStarts[i] = text.length
            val number = Ar.num(i + 1)
            text.append(number)
            text.setSpan(ForegroundColorSpan(numberColor), verseStarts[i], text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(RelativeSizeSpan(0.62f), verseStarts[i], text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.append(" ")
            text.append(verses[i])
            if (i < verses.size - 1) text.append("\n")
            words += verses[i].split(' ').size
        }
        chapterText.text = text
        requiredSeconds = (words * SECONDS_PER_WORD).toInt().coerceIn(MIN_SECONDS, MAX_SECONDS)
        currentVerse = 1

        val target = Prefs.target(this)
        val done = Prefs.doneToday(this)
        findViewById<TextView>(R.id.chapterTitle).text = Bible.title(index)
        findViewById<TextView>(R.id.chapterSub).text =
            if (done < target) "الأصحاح " + Ar.num(done + 1) + " من " + Ar.num(target) + " اليوم · ترجمة فان دايك"
            else "قراءة إضافية · ترجمة فان دايك"

        scroll.scrollTo(0, 0)
        progress.progress = 0
        updateStatus()
    }

    /** Called on every scroll and after every layout of the text. */
    private fun onScrolled() {
        val textHeight = chapterText.height
        val viewport = scroll.height
        if (textHeight == 0 || viewport == 0) return

        val maxScroll = (textHeight - viewport).coerceAtLeast(0)
        val y = scroll.scrollY
        progress.progress = if (maxScroll == 0) 1000 else (y.toLong() * 1000L / maxScroll).toInt().coerceIn(0, 1000)

        val slack = (24 * resources.displayMetrics.density).toInt()
        if (y >= maxScroll - slack) reachedEnd = true

        // Last verse that has appeared on screen.
        val layout = chapterText.layout
        if (layout != null && verseStarts.isNotEmpty()) {
            val bottom = (y + viewport - chapterText.paddingTop).coerceAtLeast(0)
            val line = layout.getLineForVertical(bottom)
            val offset = layout.getLineStart(line)
            var verse = 1
            for (i in verseStarts.indices) {
                if (verseStarts[i] <= offset) verse = i + 1 else break
            }
            if (reachedEnd) verse = verseStarts.size
            if (verse > currentVerse) currentVerse = verse
        }
        updateStatus()
    }

    private fun updateStatus() {
        val total = verseStarts.size
        verseStatus.text = "الآية " + Ar.num(currentVerse.coerceAtMost(total.coerceAtLeast(1))) + " من " + Ar.num(total)

        val remaining = requiredSeconds - elapsedSeconds
        timeStatus.text = if (remaining <= 0) {
            "اكتمل الوقت"
        } else {
            val seconds = remaining % 60
            "باقي " + Ar.num(remaining / 60) + ":" + (if (seconds < 10) Ar.num(0) else "") + Ar.num(seconds)
        }

        val ready = reachedEnd && remaining <= 0
        finishButton.isEnabled = ready
        finishButton.setTextColor(getColor(if (ready) R.color.cream else R.color.disabled_text))
        finishHint.visibility = if (ready) View.INVISIBLE else View.VISIBLE
    }

    private fun onFinishClicked() {
        if (!(reachedEnd && elapsedSeconds >= requiredSeconds)) return
        val completedToday = Prefs.markChapterRead(this)
        if (completedToday) {
            startActivity(Intent(this, DoneActivity::class.java))
            finish()
        } else {
            elapsedSeconds = 0
            reachedEnd = false
            loadChapter()
        }
    }
}
