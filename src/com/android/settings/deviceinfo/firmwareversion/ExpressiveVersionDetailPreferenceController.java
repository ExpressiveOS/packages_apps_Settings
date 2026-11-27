/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

public class ExpressiveVersionDetailPreferenceController extends BasePreferenceController {

    private static final String TAG = "ExpressiveVersionCtrl";
    private static final int DELAY_TIMER_MILLIS = 500;
    private static final int ACTIVITY_TRIGGER_COUNT = 3;
    private static final String EOS_VERSION_PROPERTY = "ro.expressiveos.version";
    private static final String PLATLOGO_PACKAGE_NAME = "org.expressiveos.easteregg";
    private static final String PLATLOGO_ACTIVITY_CLASS = PLATLOGO_PACKAGE_NAME + ".EasterEgg";

    private final UserManager mUserManager;
    private final long[] mHits = new long[ACTIVITY_TRIGGER_COUNT];
    private RestrictedLockUtils.EnforcedAdmin mFunDisallowedAdmin;
    private boolean mFunDisallowedBySystem;

    public ExpressiveVersionDetailPreferenceController(Context context, String key) {
        super(context, key);
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        initializeAdminPermissions();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        return RomBuildProp.get(EOS_VERSION_PROPERTY, mContext.getString(R.string.unknown));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        final View hostView = findDecorView(preference.getContext());
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - DELAY_TIMER_MILLIS)) {
            if (mUserManager != null && mUserManager.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                if (mFunDisallowedAdmin != null && !mFunDisallowedBySystem) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                            mContext, mFunDisallowedAdmin);
                }
                return true;
            }
            if (hostView != null) {
                hostView.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            }
            final Intent intent = new Intent(Intent.ACTION_MAIN)
                    .setClassName(PLATLOGO_PACKAGE_NAME, PLATLOGO_ACTIVITY_CLASS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mContext.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Unable to start easter egg activity", e);
            }
        } else if (hostView != null) {
            hostView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
        return true;
    }

    private static View findDecorView(Context context) {
        Context cur = context;
        while (cur instanceof ContextWrapper) {
            if (cur instanceof Activity) {
                return ((Activity) cur).getWindow().getDecorView();
            }
            cur = ((ContextWrapper) cur).getBaseContext();
        }
        return null;
    }

    private void initializeAdminPermissions() {
        mFunDisallowedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_FUN, UserHandle.myUserId());
        mFunDisallowedBySystem = RestrictedLockUtilsInternal.hasBaseUserRestriction(
                mContext, UserManager.DISALLOW_FUN, UserHandle.myUserId());
    }
}
