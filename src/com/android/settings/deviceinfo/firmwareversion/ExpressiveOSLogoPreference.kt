/*
 * Copyright (C) 2019-2024 The Evolution X Project
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.deviceinfo.hardwareinfo.ModelPageMotion
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.LayoutPreference

/** ExpressiveOS logo banner on the Android version screen. Tapping replays the sweep and opens the Telegram group. */
class ExpressiveOSLogoPreference :
    PreferenceMetadata, PreferenceBinding, Preference.OnPreferenceClickListener {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.expressiveos_logo_purpose

    override val indexable
        get() = false

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun createWidget(context: Context): Preference =
        LayoutPreference(context, R.layout.expressiveos_logo)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        // GlowView resolves its own ink color from the current theme.
        (preference as? LayoutPreference)?.let { layoutPref ->
            layoutPref.findViewById<View>(R.id.expressiveos_logo_container)?.let { container ->
                container.contentDescription =
                    preference.context.getString(R.string.expressiveos_logo_content_description)
                ExpressiveLogoAnimator.bind(container)
                bindPressSpring(container)
            }
            layoutPref.findViewById<GlowView>(R.id.expressiveos_rgb_glow)
                ?.setAnimationLimit(GlowView.SWEEP_MS)
        }
        preference.onPreferenceClickListener = this
    }

    private fun bindPressSpring(container: View) {
        val scaleX = SpringAnimation(container, DynamicAnimation.SCALE_X, 1f).apply {
            spring = SpringForce(1f)
                .setDampingRatio(ModelPageMotion.FAST_SPATIAL_DAMPING)
                .setStiffness(ModelPageMotion.FAST_SPATIAL_STIFFNESS)
        }
        val scaleY = SpringAnimation(container, DynamicAnimation.SCALE_Y, 1f).apply {
            spring = SpringForce(1f)
                .setDampingRatio(ModelPageMotion.FAST_SPATIAL_DAMPING)
                .setStiffness(ModelPageMotion.FAST_SPATIAL_STIFFNESS)
        }
        val parent = container.parent as? View ?: container
        parent.setOnTouchListener { _, event ->
            val target = when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> 0.97f
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> 1f
                else -> return@setOnTouchListener false
            }
            if (ValueAnimator.areAnimatorsEnabled()) {
                scaleX.animateToFinalPosition(target)
                scaleY.animateToFinalPosition(target)
            } else {
                if (scaleX.isRunning) scaleX.cancel()
                if (scaleY.isRunning) scaleY.cancel()
                container.scaleX = target
                container.scaleY = target
            }
            false
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        (preference as? LayoutPreference)?.let { layoutPref ->
            layoutPref.findViewById<View>(R.id.expressiveos_logo_container)
                ?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            layoutPref.findViewById<GlowView>(R.id.expressiveos_rgb_glow)
                ?.restartAnimation()
        }
        val context = preference.context
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE_URL))
        if (context.packageManager.queryIntentActivities(intent, 0).isEmpty()) {
            // Don't send out the intent to stop crash
            Log.w(TAG, "queryIntentActivities() returns empty")
            return true
        }
        context.startActivity(intent)
        return true
    }

    companion object {
        const val KEY = "expressiveos_logo"
        private const val TAG = "ExpressiveOSLogoPref"
        private const val WEBSITE_URL = "https://t.me/ExpressiveGroup"
    }
}
