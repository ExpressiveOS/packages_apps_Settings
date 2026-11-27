/*
 * SPDX-FileCopyrightText: Evolution X
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.hardwareinfo;

/**
 * M3 Expressive spring tokens (ExpressiveMotionTokens) for View-based About Phone surfaces, which
 * cannot read Compose's MotionScheme directly. Each pair is damping ratio / stiffness, as used by
 * androidx.dynamicanimation's SpringForce. Keep these in sync with MotionScheme.expressive().
 */
public final class ModelPageMotion {

    public static final float FAST_SPATIAL_DAMPING = 0.6f;
    public static final float FAST_SPATIAL_STIFFNESS = 800f;
    public static final float DEFAULT_SPATIAL_DAMPING = 0.8f;
    public static final float DEFAULT_SPATIAL_STIFFNESS = 380f;
    public static final float SLOW_SPATIAL_DAMPING = 0.8f;
    public static final float SLOW_SPATIAL_STIFFNESS = 200f;

    public static final float FAST_EFFECTS_DAMPING = 1f;
    public static final float FAST_EFFECTS_STIFFNESS = 3800f;
    public static final float DEFAULT_EFFECTS_DAMPING = 1f;
    public static final float DEFAULT_EFFECTS_STIFFNESS = 1600f;
    public static final float SLOW_EFFECTS_DAMPING = 1f;
    public static final float SLOW_EFFECTS_STIFFNESS = 800f;

    private ModelPageMotion() {}
}
