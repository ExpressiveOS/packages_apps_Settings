/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import vendor.expressiveos.touch.IGloveMode;

public class HighTouchSensitivityPreferenceController extends TogglePreferenceController {

    private static final String TAG = "HighTouchSensitivityCtrl";
    private static final String SERVICE_NAME = IGloveMode.DESCRIPTOR + "/default";

    private IGloveMode mGloveMode;

    public HighTouchSensitivityPreferenceController(Context context, String key) {
        super(context, key);
    }

    private IGloveMode getGloveMode() {
        if (mGloveMode != null) {
            IBinder binder = mGloveMode.asBinder();
            if (binder != null && binder.isBinderAlive()) {
                return mGloveMode;
            }
            mGloveMode = null;
        }
        if (!ServiceManager.isDeclared(SERVICE_NAME)) {
            return null;
        }
        try {
            IBinder binder = ServiceManager.waitForDeclaredService(SERVICE_NAME);
            mGloveMode = IGloveMode.Stub.asInterface(binder);
        } catch (SecurityException e) {
            Log.w(TAG, "Unable to bind " + SERVICE_NAME, e);
        }
        return mGloveMode;
    }

    @Override
    public int getAvailabilityStatus() {
        return ServiceManager.isDeclared(SERVICE_NAME)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        IGloveMode gloveMode = getGloveMode();
        if (gloveMode == null) {
            return false;
        }
        try {
            return gloveMode.getEnabled();
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to read glove mode state", e);
            return false;
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        IGloveMode gloveMode = getGloveMode();
        if (gloveMode == null) {
            return false;
        }
        try {
            gloveMode.setEnabled(isChecked);
            return true;
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to update glove mode state", e);
            return false;
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
