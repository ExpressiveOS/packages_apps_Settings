/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.notification

import android.content.Context
import android.content.Intent
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

class Viper4AndroidPreference : PreferenceMetadata, PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.viper4android_purpose

    override val title: Int
        get() = R.string.viper4android_title

    override val summary: Int
        get() = R.string.viper4android_summary

    override val availabilityDescription: String =
        "Requires ViPER4Android RE to be installed on the device."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean = intent(context) != null

    override fun intent(context: Context): Intent? =
        context.packageManager
            .getLaunchIntentForPackage(Viper4AndroidPreferenceController.PACKAGE_NAME)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    companion object {
        const val KEY = Viper4AndroidPreferenceController.KEY
    }
}
