/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.SystemClock
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

class ExpressiveVersionDetailPreference :
    PreferenceMetadata, PreferenceSummaryProvider, PreferenceBinding,
    Preference.OnPreferenceClickListener {

    private val hits = LongArray(ACTIVITY_TRIGGER_COUNT)

    override val key: String
        get() = "eos_version"

    override val purpose: Int
        get() = R.string.os_firmware_version_purpose

    override val title: Int
        get() = R.string.eos_version_title

    override val indexable
        get() = false

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_PREFERENCE)

    override fun intent(context: Context): Intent? =
        Intent(Intent.ACTION_MAIN)
            .setClassName(PLATLOGO_PACKAGE_NAME, PLATLOGO_ACTIVITY_CLASS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .takeIf { context.packageManager.resolveActivity(it, 0) != null }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
        preference.onPreferenceClickListener = this
    }

    override fun getSummary(context: Context): CharSequence =
        RomBuildProp.get(EOS_VERSION_PROPERTY, context.getString(R.string.unknown))

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (Utils.isMonkeyRunning()) return true

        val context = preference.context
        val decorView = context.findHostActivity()?.window?.decorView

        for (index in 1..<ACTIVITY_TRIGGER_COUNT) hits[index - 1] = hits[index]
        hits[ACTIVITY_TRIGGER_COUNT - 1] = SystemClock.uptimeMillis()
        if (hits[ACTIVITY_TRIGGER_COUNT - 1] - hits[0] > DELAY_TIMER_MILLIS) {
            decorView?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            return true
        }

        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        if (userManager?.hasUserRestriction(UserManager.DISALLOW_FUN) != true) {
            decorView?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            val launch = intent(context)
            if (launch == null) {
                Log.w(TAG, "Easter egg activity not available")
                return true
            }
            return false
        }

        val myUserId = UserHandle.myUserId()
        val enforcedAdmin =
            RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                context,
                UserManager.DISALLOW_FUN,
                myUserId,
            ) ?: return true
        val disallowedBySystem =
            RestrictedLockUtilsInternal.hasBaseUserRestriction(
                context,
                UserManager.DISALLOW_FUN,
                myUserId,
            )
        if (!disallowedBySystem) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, enforcedAdmin)
        }
        return true
    }

    private fun Context.findHostActivity(): Activity? {
        var cur: Context = this
        while (cur is ContextWrapper) {
            if (cur is Activity) return cur
            cur = cur.baseContext
        }
        return null
    }

    companion object {
        private const val TAG = "ExpressiveVersionPref"
        const val ACTIVITY_TRIGGER_COUNT = 3
        const val DELAY_TIMER_MILLIS = 500L

        const val EOS_VERSION_PROPERTY: String = "ro.expressiveos.version"

        const val PLATLOGO_PACKAGE_NAME: String = "org.expressiveos.easteregg"
        const val PLATLOGO_ACTIVITY_CLASS: String = PLATLOGO_PACKAGE_NAME + ".EasterEgg"
    }
}
