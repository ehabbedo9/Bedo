package com.dailybible.lock

import android.content.Context
import org.json.JSONArray

/**
 * Arabic Smith & Van Dyck Bible (public domain, fully vowelled text from eBible.org
 * "arb-vd" via github.com/midvash/bible-data), stored as one JSON file per book
 * in assets/bible/bNN.json. Each file is an array of chapters; each chapter is an
 * array of verse strings.
 */
object Bible {

    val BOOK_NAMES = arrayOf(
        "التكوين", "الخروج", "اللاويين", "العدد", "التثنية", "يشوع", "القضاة", "راعوث",
        "صموئيل الأول", "صموئيل الثاني", "الملوك الأول", "الملوك الثاني",
        "أخبار الأيام الأول", "أخبار الأيام الثاني", "عزرا", "نحميا", "أستير", "أيوب",
        "المزامير", "الأمثال", "الجامعة", "نشيد الأنشاد", "إشعياء", "إرميا", "مراثي إرميا",
        "حزقيال", "دانيال", "هوشع", "يوئيل", "عاموس", "عوبديا", "يونان", "ميخا", "ناحوم",
        "حبقوق", "صفنيا", "حجي", "زكريا", "ملاخي",
        "متى", "مرقس", "لوقا", "يوحنا", "أعمال الرسل", "رومية", "كورنثوس الأولى",
        "كورنثوس الثانية", "غلاطية", "أفسس", "فيلبي", "كولوسي", "تسالونيكي الأولى",
        "تسالونيكي الثانية", "تيموثاوس الأولى", "تيموثاوس الثانية", "تيطس", "فليمون",
        "العبرانيين", "يعقوب", "بطرس الأولى", "بطرس الثانية", "يوحنا الأولى",
        "يوحنا الثانية", "يوحنا الثالثة", "يهوذا", "رؤيا يوحنا"
    )

    val CHAPTER_COUNTS = intArrayOf(
        50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150, 31,
        12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
        28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5,
        1, 1, 1, 22
    )

    const val TOTAL_CHAPTERS = 1189

    private var cachedBook = -1
    private var cachedChapters: List<List<String>> = emptyList()

    /** Converts a global chapter index (0..1188) to (bookIndex, chapterIndex). */
    fun locate(globalIndex: Int): Pair<Int, Int> {
        var rest = ((globalIndex % TOTAL_CHAPTERS) + TOTAL_CHAPTERS) % TOTAL_CHAPTERS
        for (book in CHAPTER_COUNTS.indices) {
            if (rest < CHAPTER_COUNTS[book]) return Pair(book, rest)
            rest -= CHAPTER_COUNTS[book]
        }
        return Pair(0, 0)
    }

    /** Title such as "التكوين ٢". */
    fun title(globalIndex: Int): String {
        val (book, chapter) = locate(globalIndex)
        return BOOK_NAMES[book] + " " + Ar.num(chapter + 1)
    }

    @Synchronized
    private fun loadBook(context: Context, book: Int): List<List<String>> {
        if (book == cachedBook) return cachedChapters
        val name = String.format(java.util.Locale.US, "bible/b%02d.json", book)
        val text = context.applicationContext.assets.open(name).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val json = JSONArray(text)
        val chapters = ArrayList<List<String>>(json.length())
        for (i in 0 until json.length()) {
            val verses = json.getJSONArray(i)
            val list = ArrayList<String>(verses.length())
            for (j in 0 until verses.length()) list.add(verses.getString(j))
            chapters.add(list)
        }
        cachedBook = book
        cachedChapters = chapters
        return chapters
    }

    /** Verses of the chapter at the given global index. */
    fun verses(context: Context, globalIndex: Int): List<String> {
        val (book, chapter) = locate(globalIndex)
        return loadBook(context, book)[chapter]
    }
}
