package com.dailybible.lock

/** Arabic text helpers: Arabic-Indic digits and counted nouns. */
object Ar {
    private const val DIGITS = "٠١٢٣٤٥٦٧٨٩"

    fun num(value: Int): String {
        val sb = StringBuilder()
        for (ch in value.toString()) {
            if (ch in '0'..'9') sb.append(DIGITS[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    /** "أصحاح واحد", "أصحاحان", "٣ أصحاحات", "١١ أصحاحًا". */
    fun chapters(n: Int): String = when {
        n == 1 -> "أصحاح واحد"
        n == 2 -> "أصحاحان"
        n in 3..10 -> num(n) + " أصحاحات"
        else -> num(n) + " أصحاحًا"
    }

    /** Unit word only, to sit under a big number. */
    fun chapterUnit(n: Int): String = when {
        n == 1 -> "أصحاح"
        n == 2 -> "أصحاحان"
        n in 3..10 -> "أصحاحات"
        else -> "أصحاحًا"
    }

    fun days(n: Int): String = when {
        n == 0 -> "لا أيام متتالية بعد"
        n == 1 -> "يوم واحد"
        n == 2 -> "يومان متتاليان"
        n in 3..10 -> num(n) + " أيام متتالية"
        else -> num(n) + " يومًا متتاليًا"
    }

    fun verses(n: Int): String = when {
        n == 1 -> "آية واحدة"
        n == 2 -> "آيتان"
        n in 3..10 -> num(n) + " آيات"
        else -> num(n) + " آية"
    }
}
