package com.smz70.mmhue.watch

import android.content.Context

/**
 * Runtime configuration, so the app is not wired to one house.
 *
 * The mmhue address used to be a compile-time constant; now it is a setting the
 * user can change on the watch, persisted in SharedPreferences and mirrored here
 * in memory for the networking layer to read on every request. It defaults to
 * the build's address so an untouched install still works.
 */
object AppConfig {
    @Volatile
    var baseUrl: String = BuildConfig.MMHUE_BASE_URL
        private set

    private const val PREFS = "mmhue-settings"
    private const val KEY_BASE_URL = "base_url"

    /** Load persisted settings into memory. Call once at app start. */
    fun load(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_BASE_URL, null)?.let { baseUrl = it }
    }

    /** Persist and apply a new address. Returns the normalised value stored. */
    fun setBaseUrl(context: Context, raw: String): String {
        val normalised = normalizeUrl(raw)
        baseUrl = normalised
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_BASE_URL, normalised).apply()
        return normalised
    }

    /**
     * Tidy a hand-typed address into a base URL: add http:// if no scheme is
     * given (the panel is plain HTTP on the LAN), and drop any trailing slash so
     * paths concatenate cleanly.
     */
    fun normalizeUrl(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return BuildConfig.MMHUE_BASE_URL
        if (!s.startsWith("http://") && !s.startsWith("https://")) s = "http://$s"
        return s.trimEnd('/')
    }
}
