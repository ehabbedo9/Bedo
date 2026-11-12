package com.dailybible.lock

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Lets the user choose where reading starts: first a book (Old or New Testament),
 * then a chapter. Reading then continues in order from that chapter.
 */
class PickerActivity : AppCompatActivity() {

    companion object {
        private const val FIRST_NEW_TESTAMENT_BOOK = 39
        private const val STATE_BOOK = "book"
        private const val STATE_NEW = "new_testament"
    }

    private lateinit var grid: GridView
    private lateinit var heading: TextView
    private lateinit var tabs: View
    private lateinit var tabOld: TextView
    private lateinit var tabNew: TextView
    private var newTestament = false
    private var book = -1 // -1 while a book is being chosen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)

        grid = findViewById(R.id.pickGrid)
        heading = findViewById(R.id.pickTitle)
        tabs = findViewById(R.id.pickTabs)
        tabOld = findViewById(R.id.tabOld)
        tabNew = findViewById(R.id.tabNew)

        findViewById<View>(R.id.pickBack).setOnClickListener { goBack() }
        tabOld.setOnClickListener {
            newTestament = false
            showBooks()
        }
        tabNew.setOnClickListener {
            newTestament = true
            showBooks()
        }
        grid.setOnItemClickListener { _, _, position, _ -> onItem(position) }

        if (savedInstanceState != null) {
            newTestament = savedInstanceState.getBoolean(STATE_NEW, false)
            book = savedInstanceState.getInt(STATE_BOOK, -1)
        } else {
            newTestament = Bible.locate(Prefs.pointer(this)).first >= FIRST_NEW_TESTAMENT_BOOK
        }
        if (book >= 0) showChapters(book) else showBooks()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_NEW, newTestament)
        outState.putInt(STATE_BOOK, book)
    }

    private fun firstBook(): Int = if (newTestament) FIRST_NEW_TESTAMENT_BOOK else 0

    private fun showBooks() {
        book = -1
        heading.text = "اختر السفر"
        tabs.visibility = View.VISIBLE
        tabOld.setBackgroundResource(if (newTestament) R.drawable.bg_preset else R.drawable.bg_preset_on)
        tabNew.setBackgroundResource(if (newTestament) R.drawable.bg_preset_on else R.drawable.bg_preset)

        val last = if (newTestament) Bible.BOOK_NAMES.size else FIRST_NEW_TESTAMENT_BOOK
        val names = ArrayList<String>()
        for (i in firstBook() until last) names.add(Bible.BOOK_NAMES[i])
        grid.numColumns = 2
        grid.adapter = ArrayAdapter<String>(this, R.layout.item_pick, names)
    }

    private fun showChapters(bookIndex: Int) {
        book = bookIndex
        heading.text = Bible.BOOK_NAMES[bookIndex] + " · اختر الأصحاح"
        tabs.visibility = View.GONE

        val numbers = ArrayList<String>()
        for (i in 1..Bible.CHAPTER_COUNTS[bookIndex]) numbers.add(Ar.num(i))
        grid.numColumns = 5
        grid.adapter = ArrayAdapter<String>(this, R.layout.item_pick, numbers)
    }

    private fun onItem(position: Int) {
        if (book < 0) {
            showChapters(firstBook() + position)
        } else {
            val index = Bible.globalIndex(book, position)
            Prefs.setPointer(this, index)
            Toast.makeText(this, "تبدأ القراءة من " + Bible.title(index), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun goBack() {
        if (book >= 0) showBooks() else finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (book >= 0) showBooks() else super.onBackPressed()
    }
}
