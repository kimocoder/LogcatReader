package com.dp.logcatapp.fragments.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v14.preference.MultiSelectListPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import com.dp.logcat.Logcat
import com.dp.logcatapp.BuildConfig
import com.dp.logcatapp.R
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.isDarkThemeOn
import com.dp.logcatapp.util.restartApp
import com.dp.logcatapp.util.showToast
import java.text.NumberFormat

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        val TAG = SettingsFragment::class.qualifiedName
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        setupAppearanceCategory()
        setupLogcatCategory()
        setupAboutCategory()
    }

    private fun setupAppearanceCategory() {
        val sharedPrefs = preferenceScreen.sharedPreferences
        val themePref = findPreference(PreferenceKeys.Appearance.KEY_THEME) as ListPreference
        val useBlackThemePref = findPreference(PreferenceKeys.Appearance.KEY_USE_BLACK_THEME)

        val currTheme = sharedPrefs.getString(PreferenceKeys.Appearance.KEY_THEME,
                PreferenceKeys.Appearance.Default.THEME)

        when (currTheme) {
            PreferenceKeys.Appearance.Theme.AUTO,
            PreferenceKeys.Appearance.Theme.DARK -> useBlackThemePref.isEnabled = true
            else -> useBlackThemePref.isEnabled = false
        }

        val themePrefEntries = resources.getStringArray(R.array.pref_appearance_theme_entries)
        themePref.summary = themePrefEntries[currTheme.toInt()]

        themePref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    preference.summary = themePrefEntries[(newValue as String).toInt()]
                    activity!!.restartApp()
                    true
                }

        useBlackThemePref.onPreferenceChangeListener = Preference
                .OnPreferenceChangeListener { _, _ ->
                    if (activity!!.isDarkThemeOn()) {
                        activity!!.restartApp()
                    }
                    true
                }
    }

    private fun setupLogcatCategory() {
        val prefPollInterval = findPreference(PreferenceKeys.Logcat.KEY_POLL_INTERVAL)
        val prefBuffers = findPreference(PreferenceKeys.Logcat.KEY_BUFFERS)
                as MultiSelectListPreference
        val prefMaxLogs = findPreference(PreferenceKeys.Logcat.KEY_MAX_LOGS)

        prefPollInterval.summary = preferenceScreen.sharedPreferences
                .getString(PreferenceKeys.Logcat.KEY_POLL_INTERVAL,
                        PreferenceKeys.Logcat.Default.POLL_INTERVAL) + " ms"

        prefPollInterval.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            try {
                val v = newValue.toString().trim()
                val num = v.toLong()
                if (num <= 0) {
                    activity!!.showToast(getString(R.string.value_must_be_greater_than_0))
                    false
                } else {
                    prefPollInterval.summary = "$v ms"
                    true
                }
            } catch (e: NumberFormatException) {
                activity!!.showToast(getString(R.string.value_must_be_a_positive_integer))
                false
            }
        }

        val availableBuffers = Logcat.AVAILABLE_BUFFERS
        val defaultBuffers = PreferenceKeys.Logcat.Default.BUFFERS
        if (availableBuffers.isNotEmpty() && defaultBuffers.isNotEmpty()) {
            val bufferValues = preferenceScreen.sharedPreferences
                    .getStringSet(PreferenceKeys.Logcat.KEY_BUFFERS, defaultBuffers)

            val toSummary = { values: Set<String> ->
                values.map { e -> availableBuffers[e.toInt()] }
                        .sorted()
                        .joinToString(", ")
            }

            prefBuffers.entries = availableBuffers.copyOf()
            val entryValues = mutableListOf<String>()
            for (i in 0 until availableBuffers.size) {
                entryValues += i.toString()
            }
            prefBuffers.entryValues = entryValues.toTypedArray()
            prefBuffers.summary = toSummary(bufferValues)

            @Suppress("unchecked_cast")
            prefBuffers.onPreferenceChangeListener = Preference
                    .OnPreferenceChangeListener { preference, newValue ->
                        val mp = preference as MultiSelectListPreference
                        val values = newValue as Set<String>

                        if (values.isEmpty()) {
                            false
                        } else {
                            mp.summary = toSummary(values)
                            true
                        }
                    }
        } else {
            prefBuffers.isVisible = false
        }

        val maxLogs = preferenceScreen.sharedPreferences.getString(
                PreferenceKeys.Logcat.KEY_MAX_LOGS,
                PreferenceKeys.Logcat.Default.MAX_LOGS
        ).trim().toInt()

        prefMaxLogs.summary = NumberFormat.getInstance().format(maxLogs)
        prefMaxLogs.onPreferenceChangeListener = Preference
                .OnPreferenceChangeListener callback@ { _, newValue ->
                    try {
                        val newMaxLogs = (newValue as String).trim().toInt()
                        if (newMaxLogs == maxLogs) {
                            return@callback false
                        }

                        if (newMaxLogs < 1000) {
                            activity!!.showToast(getString(R.string.cannot_be_less_than_1000))
                            return@callback false
                        }

                        true
                    } catch (e: NumberFormatException) {
                        activity!!.showToast(getString(R.string.not_a_valid_number))
                        false
                    }
                }
    }

    private fun setupAboutCategory() {
        val prefAbout = findPreference(PreferenceKeys.About.KEY_VERSION_NAME)
        prefAbout.summary = "Version ${BuildConfig.VERSION_NAME}"

        val prefGitHubPage = findPreference(PreferenceKeys.About.KEY_GITHUB_PAGE)
        prefGitHubPage.setOnPreferenceClickListener { _ ->
            try {
                val url = "https://github.com/darshanparajuli/LogcatReader"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
