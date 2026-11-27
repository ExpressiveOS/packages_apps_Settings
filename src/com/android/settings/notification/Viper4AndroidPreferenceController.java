/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

public class Viper4AndroidPreferenceController extends BasePreferenceController {

    public static final String KEY = "viper4android_settings";
    public static final String PACKAGE_NAME = "com.llsl.viper4android";

    public Viper4AndroidPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Nullable
    private Intent getLaunchIntent() {
        return mContext.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);
    }

    @Override
    public int getAvailabilityStatus() {
        return getLaunchIntent() != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        final Intent intent = getLaunchIntent();
        if (intent == null) {
            return false;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        return true;
    }
}
