/*
 * Copyright (C) 2023 The Nameless-AOSP Project
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.expressiveos.settings.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import org.expressiveos.settings.preferences.ComposeDialogHandle;
import org.expressiveos.settings.preferences.ListPreferencePicker;

/**
 * The ListPreference for the pages need to show apps icon, backed by the Material 3 Expressive
 * segmented picker dialog.
 */
public class AppListPreference extends ListPreference {

    private ComposeDialogHandle mPickerDialog;

    public AppListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(com.android.settingslib.widget.preference.app.R.layout.preference_app);
    }

    public AppListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(com.android.settingslib.widget.preference.app.R.layout.preference_app);
    }

    public AppListPreference(Context context) {
        super(context);
        setLayoutResource(com.android.settingslib.widget.preference.app.R.layout.preference_app);
    }

    public AppListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(com.android.settingslib.widget.preference.app.R.layout.preference_app);
    }

    @Override
    protected void onClick() {
        mPickerDialog = ListPreferencePicker.openListPicker(this, mPickerDialog);
    }

    @Override
    public void onDetached() {
        if (mPickerDialog != null) {
            mPickerDialog.dismiss();
            mPickerDialog = null;
        }
        super.onDetached();
    }
}
