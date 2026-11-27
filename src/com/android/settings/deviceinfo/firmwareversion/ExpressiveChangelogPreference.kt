/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.content.Intent
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

/** Explicit Changelog row in Android version / Firmware version launching ChangelogActivity. */
class ExpressiveChangelogPreference : PreferenceMetadata, PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.eos_changelog_purpose

    override val title: Int
        get() = R.string.changelog_title

    override val summary: Int
        get() = R.string.changelog_summary

    override val indexable: Boolean
        get() = true

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_PREFERENCE)

    override val availabilityDescription: String =
        "Requires the ExpressiveOS changelog activity to be available."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean = intent(context) != null

    override fun intent(context: Context): Intent? =
        Intent()
            .setClassName(
                "com.android.settings",
                "org.expressiveos.settings.fragments.about.ChangelogActivity",
            )
            .takeIf { context.packageManager.resolveActivity(it, 0) != null }

    companion object {
        const val KEY = "eos_changelog"
    }
}
