/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display.darkmode

import android.content.Context
import android.provider.Settings
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

class DarkModeBlackThemePreference(context: Context, private val darkModeStorage: DarkModeStorage) :
    PreferenceMetadata, BooleanValuePreference, PreferenceBinding {

    private val blackThemeStorage = DarkModeBlackThemeStorage(context)

    override val key: String = Settings.Secure.EOS_BLACK_THEME

    override val purpose: Int = R.string.eos_black_theme_purpose

    override val title: Int = R.string.eos_black_theme_title

    override val summary: Int = R.string.eos_black_theme_summary

    override val supportsWrite = true

    override fun storage(context: Context) = blackThemeStorage

    override fun isEnabled(context: Context): Boolean {
        return darkModeStorage.getBoolean(DarkModeMainSwitchPreference.KEY) ?: false
    }

    override fun createWidget(context: Context) =
        SwitchPreferenceCompat(context).apply { isPersistent = false }

    fun isAvailable(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(
                "org.expressiveos.overlay.blacktheme",
                0,
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
