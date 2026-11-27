/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.notification

import android.content.Context
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

class VerticalVolumeSlidersPreference :
    SwitchPreference(
        key = KEY,
        purpose = R.string.volume_panel_vertical_sliders_purpose,
        title = R.string.volume_panel_vertical_sliders_title,
        summary = R.string.volume_panel_vertical_sliders_summary,
    ) {

    override fun storage(context: Context): KeyValueStore =
        SettingsSystemStore.get(context).apply { setDefaultValue(KEY, false) }

    override fun getReadPermissions(context: Context) = SettingsSystemStore.getReadPermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSystemStore.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = VerticalVolumeSlidersPreferenceController.KEY
    }
}
