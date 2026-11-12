package com.dailybible.lock

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** All saved state: daily target, reading position, today's count, streak, locked apps. */
object Prefs {
    private const val FILE = "daily_bible"
    private const val K_SETUP = "setup_done"
    private const val K_TARGET = "target"
    private const val K_NEXT_TARGET = "next_target"
    private const val K_DAY = "day"
    private const val K_DONE_TODAY = "done_today"
    private const val K_POINTER = "pointer"
    private const val K_STREAK = "streak"
    private const val K_LAST_COMPLETE = "last_complete_day"
    private const val K_FONT = "font_step"
    private const val K_APP_PREFIX = "lock_"

    const val DEFAULT_TARGET = 3
    const val MAX_TARGET = 20

    private fun sp(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun dayKey(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun today(): String = dayKey(0)
    private fun yesterday(): String = dayKey(-1)

    /** Starts a new day at local midnight: resets today's count and applies a pending target. */
    @Synchronized
    private fun rollover(c: Context) {
        val p = sp(c)
        val today = today()
        if (p.getString(K_DAY, null) == today) return
        p.edit()
            .putString(K_DAY, today)
            .putInt(K_DONE_TODAY, 0)
            .putInt(K_TARGET, p.getInt(K_NEXT_TARGET, p.getInt(K_TARGET, DEFAULT_TARGET)))
            .apply()
    }

    fun isSetupDone(c: Context): Boolean = sp(c).getBoolean(K_SETUP, false)

    fun setSetupDone(c: Context) {
        sp(c).edit().putBoolean(K_SETUP, true).apply()
    }

    /** Today's required number of chapters. */
    fun target(c: Context): Int {
        rollover(c)
        return sp(c).getInt(K_TARGET, DEFAULT_TARGET)
    }

    /** The number that applies from tomorrow (same as today's unless it was changed). */
    fun nextTarget(c: Context): Int {
        val p = sp(c)
        return p.getInt(K_NEXT_TARGET, p.getInt(K_TARGET, DEFAULT_TARGET))
    }

    /** immediate = true during first setup; later changes apply from the next day. */
    fun setTarget(c: Context, value: Int, immediate: Boolean) {
        val v = value.coerceIn(1, MAX_TARGET)
        rollover(c)
        val e = sp(c).edit().putInt(K_NEXT_TARGET, v)
        if (immediate) e.putInt(K_TARGET, v)
        e.apply()
    }

    fun doneToday(c: Context): Int {
        rollover(c)
        return sp(c).getInt(K_DONE_TODAY, 0)
    }

    fun isComplete(c: Context): Boolean = doneToday(c) >= target(c)

    /** Global index (0..1188) of the next chapter to read. */
    fun pointer(c: Context): Int = sp(c).getInt(K_POINTER, 0)

    /** Records the current chapter as read. Returns true if this completed today's reading. */
    @Synchronized
    fun markChapterRead(c: Context): Boolean {
        rollover(c)
        val p = sp(c)
        val wasComplete = p.getInt(K_DONE_TODAY, 0) >= p.getInt(K_TARGET, DEFAULT_TARGET)
        val done = p.getInt(K_DONE_TODAY, 0) + 1
        val pointer = (p.getInt(K_POINTER, 0) + 1) % Bible.TOTAL_CHAPTERS
        val e = p.edit().putInt(K_DONE_TODAY, done).putInt(K_POINTER, pointer)
        val nowComplete = done >= p.getInt(K_TARGET, DEFAULT_TARGET)
        val justCompleted = nowComplete && !wasComplete
        if (justCompleted) {
            val streak = if (p.getString(K_LAST_COMPLETE, null) == yesterday()) p.getInt(K_STREAK, 0) + 1 else 1
            e.putInt(K_STREAK, streak).putString(K_LAST_COMPLETE, today())
        }
        e.apply()
        return justCompleted
    }

    /** Consecutive completed days; 0 if the chain is broken. */
    fun streak(c: Context): Int {
        val p = sp(c)
        val last = p.getString(K_LAST_COMPLETE, null)
        return if (last == today() || last == yesterday()) p.getInt(K_STREAK, 0) else 0
    }

    fun isAppLocked(c: Context, id: String): Boolean = sp(c).getBoolean(K_APP_PREFIX + id, true)

    fun setAppLocked(c: Context, id: String, locked: Boolean) {
        sp(c).edit().putBoolean(K_APP_PREFIX + id, locked).apply()
    }

    /** 0, 1 or 2: reader font size step. */
    fun fontStep(c: Context): Int = sp(c).getInt(K_FONT, 1)

    fun setFontStep(c: Context, step: Int) {
        sp(c).edit().putInt(K_FONT, step).apply()
    }
}
