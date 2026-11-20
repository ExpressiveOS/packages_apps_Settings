/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.os.SystemProperties
import android.text.format.DateFormat
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.preference.PreferenceBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class ExpressiveVendorSecurityPatchLevelPreference :
    PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceBinding {

    override val key: String
        get() = "vendor_security_patch"

    override val purpose: Int
        get() = R.string.security_key_purpose

    override val title: Int
        get() = R.string.vendor_security_patch

    override val indexable
        get() = false

    override val availabilityDescription = "The device must have a vendor security patch level."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_PREFERENCE)

    override fun isAvailable(context: Context): Boolean =
        context.resources.getBoolean(R.bool.config_show_vendor_security_patch) &&
            SystemProperties.get(VENDOR_SECURITY_PATCH_PROPERTY, "").isNotEmpty()

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
    }

    override fun getSummary(context: Context): CharSequence {
        val raw = SystemProperties.get(VENDOR_SECURITY_PATCH_PROPERTY, "")
        if (raw.isEmpty()) return context.getString(R.string.unknown)
        return try {
            val template = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = template.parse(raw)
            if (date != null) {
                val format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy")
                DateFormat.format(format, date)
            } else {
                raw
            }
        } catch (e: ParseException) {
            raw
        }
    }

    companion object {
        const val VENDOR_SECURITY_PATCH_PROPERTY: String = "ro.vendor.build.security_patch"
    }
}
