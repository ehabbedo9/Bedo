package com.dailybible.lock

import android.content.Context

/** The social apps that can be locked, with the Android package names of each. */
object LockedApps {

    class App(val id: String, val label: String, val packages: List<String>)

    val ALL = listOf(
        App("facebook", "فيسبوك", listOf("com.facebook.katana", "com.facebook.lite")),
        App("instagram", "إنستجرام", listOf("com.instagram.android", "com.instagram.lite")),
        App("twitter", "تويتر", listOf("com.twitter.android", "com.twitter.android.lite"))
    )

    fun enabled(c: Context): List<App> = ALL.filter { Prefs.isAppLocked(c, it.id) }

    /** The locked app that owns this package, or null if the package is not locked. */
    fun find(c: Context, packageName: String): App? =
        enabled(c).firstOrNull { it.packages.contains(packageName) }

    /** "فيسبوك · إنستجرام · تويتر" */
    fun labels(c: Context): String = enabled(c).joinToString(" · ") { it.label }
}
