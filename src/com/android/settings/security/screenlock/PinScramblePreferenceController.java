/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.security.screenlock;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class PinScramblePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    static final String KEY_LOCKSCREEN_SCRAMBLE_PIN_LAYOUT = "lockscreen_scramble_pin_layout";

    private final int mUserId;
    private final LockPatternUtils mLockPatternUtils;

    public PinScramblePreferenceController(Context context, int userId,
            LockPatternUtils lockPatternUtils) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
    }

    @Override
    public boolean isAvailable() {
        return isPinLock();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_LOCKSCREEN_SCRAMBLE_PIN_LAYOUT;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.LOCKSCREEN_PIN_SCRAMBLE_LAYOUT,
                0) == 1);
    }

    private boolean isPinLock() {
        int quality = mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId);
        boolean hasPin = quality == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC ||
                quality == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
        return mLockPatternUtils.isSecure(mUserId) && hasPin;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.System.putInt(
                mContext.getContentResolver(),
                Settings.System.LOCKSCREEN_PIN_SCRAMBLE_LAYOUT,
                (Boolean) newValue ? 1 : 0);
        return true;
    }
}
