/*
 * SPDX-FileCopyrightText: Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.graphics.PathParser;

import com.android.settings.R;

/**
 * Draws the ExpressiveOS wordmark in the Bloom mark's colours: its four petals
 * (violet, blue, green, coral) as a gradient sweeping across the outline, a soft
 * glow with a blue and violet fringe, a periodic shine streak and a few
 * twinkling sparkles. Dark surfaces get the petals' mid tones (light tones for
 * the fringe), light ones their deep tones.
 *
 * While a holiday theme's banner is on, the sweep and the fringe are in that
 * holiday's colours instead, read when the view is made and again on
 * restartAnimation() (see KEY_HOLIDAY_PALETTE).
 *
 * The animation loops for as long as the view shows, unless setAnimationLimit()
 * stops it on a still frame.
 *
 * All geometry is expressed in the 1080x240 viewport of
 * ic_evolution_logo_outline and scaled to fit the view (centerInside).
 */
public class GlowView extends View {
    private static final float VIEWPORT_W = 1080f;
    private static final float VIEWPORT_H = 240f;

    // The petals in the boot animation wordmark's order, the first repeated so
    // the sweep loops: mid tones for dark surfaces, deep tones for light ones.
    private static final int[] BLOOM_ON_DARK = {
        0xFFAE69FC, 0xFF417BFC, 0xFF4AD29D, 0xFFFD7771, 0xFFAE69FC };
    private static final int[] BLOOM_ON_LIGHT = {
        0xFF6B2BBB, 0xFF1842D1, 0xFF18A07C, 0xFFD33952, 0xFF6B2BBB };
    private static final float[] BLOOM_STOPS = { 0f, 0.25f, 0.5f, 0.75f, 1f };

    // Fringe: the blue petal left, the violet petal right (light tones on dark
    // surfaces, deep tones on light ones)
    private static final int FRINGE_LEFT_ON_DARK   = 0xFF76BCFB;
    private static final int FRINGE_RIGHT_ON_DARK  = 0xFFF3B1FC;
    private static final int FRINGE_LEFT_ON_LIGHT  = 0xFF1842D1;
    private static final int FRINGE_RIGHT_ON_LIGHT = 0xFF6B2BBB;

    // Settings.Secure key with the banner colours of the holiday theme that is
    // on, written by the holiday themes and also read by the Updater's wordmark:
    // "<id>;<sweep on dark>;<sweep on light>;<fringe on dark>;<fringe on light>",
    // a sweep being 4 RRGGBB colours and a fringe 2 (left, right), split by ','.
    // Unset, empty or anything else means the Bloom colours above.
    private static final String KEY_HOLIDAY_PALETTE = "expressiveos_holiday_palette";

    // Gradient vector (viewport units). One sweep moves it by its own length,
    // so the repeating gradient loops seamlessly.
    private static final float GRAD_X1 = 190f;
    private static final float GRAD_Y1 = 60f;
    private static final float GRAD_X2 = 890f;
    private static final float GRAD_Y2 = 180f;
    // One sweep, as setAnimationLimit() takes it
    public static final long   SWEEP_MS = 5000L;

    private static final float MAIN_STROKE   = 2.4f;
    private static final int   FILL_ALPHA    = 38;   // 15%
    private static final float GLOW_STROKE   = 9f;
    private static final float GLOW_SIGMA    = 7f;
    private static final int   GLOW_ALPHA    = 140;  // 55%
    private static final float FRINGE_STROKE = 3f;
    private static final float FRINGE_SIGMA  = 2.5f;
    private static final float FRINGE_SHIFT  = 2f;
    private static final int   FRINGE_ALPHA  = 128;  // 50%

    // Shine: a skewed band crossing the logo, then a pause.
    private static final long  SHINE_MS     = 5000L;
    private static final float SHINE_ACTIVE = 0.3f;  // fraction of the period spent moving
    private static final float SHINE_FROM   = 120f;
    private static final float SHINE_TO     = 900f;
    private static final float SHINE_WIDTH  = 90f;
    private static final float SHINE_SKEW   = -0.36397f; // tan(-20 deg)
    private static final float SHINE_REACH  = 5000f;
    private static final int   SHINE_FILL_ALPHA = 64;    // 25%

    // Sparkles: x, y, size (viewport units), then twinkle start/peak/end as a
    // fraction of SPARKLE_MS.
    private static final long SPARKLE_MS = 3200L;
    private static final float[][] SPARKLES = {
        { 880f,  66f, 9f, 0.00f, 0.20f, 0.40f },
        { 792f, 176f, 7f, 0.35f, 0.50f, 0.65f },
        { 548f,  80f, 6f, 0.60f, 0.75f, 0.90f },
    };

    private final Path mLogoUnits = new Path();
    private final Path mLogo = new Path();
    private final Path mStar = new Path();
    private final Matrix mMatrix = new Matrix();
    private final Matrix mShaderMatrix = new Matrix();

    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLeftFringePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRightFringePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShineFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShineStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final LinearGradient mBand = new LinearGradient(
            0f, 0f, SHINE_WIDTH, 0f,
            new int[]{ 0x00FFFFFF, 0xFFFFFFFF, 0x00FFFFFF }, null,
            Shader.TileMode.CLAMP);
    private LinearGradient mHolo;
    private int[] mHoloColors;

    private float mScale;
    private float mOx;
    private float mOy;
    private boolean mOnDark;
    private boolean mAnimate = true;
    private long mStartTime;
    private long mAnimationLimit;

    public GlowView(Context ctx) {
        super(ctx);
        init(ctx);
    }

    public GlowView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init(ctx);
    }

    public GlowView(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        init(ctx);
    }

    private void init(Context ctx) {
        final int ink = resolveInk(ctx);
        // Light ink means a dark surface
        mOnDark = Color.luminance(ink) > 0.5f;

        for (String data : ctx.getResources().getStringArray(R.array.evolution_logo_paths)) {
            mLogoUnits.addPath(PathParser.createPathFromPathData(data));
        }

        mStar.moveTo(0f, -1f);
        mStar.quadTo(0f, 0f, 1f, 0f);
        mStar.quadTo(0f, 0f, 0f, 1f);
        mStar.quadTo(0f, 0f, -1f, 0f);
        mStar.quadTo(0f, 0f, 0f, -1f);
        mStar.close();

        mGlowPaint.setStyle(Paint.Style.STROKE);
        mGlowPaint.setStrokeJoin(Paint.Join.ROUND);
        mGlowPaint.setAlpha(GLOW_ALPHA);

        mLeftFringePaint.setStyle(Paint.Style.STROKE);
        mLeftFringePaint.setStrokeJoin(Paint.Join.ROUND);

        mRightFringePaint.setStyle(Paint.Style.STROKE);
        mRightFringePaint.setStrokeJoin(Paint.Join.ROUND);

        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setAlpha(FILL_ALPHA);

        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        mStrokePaint.setStrokeCap(Paint.Cap.ROUND);

        mShineFill.setStyle(Paint.Style.FILL);
        mShineFill.setColor(ink);
        mShineFill.setAlpha(SHINE_FILL_ALPHA);

        mShineStroke.setStyle(Paint.Style.STROKE);
        mShineStroke.setStrokeJoin(Paint.Join.ROUND);
        mShineStroke.setColor(ink);

        mBandPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mSparklePaint.setColor(ink);

        applyPalette(ctx);
    }

    // A holiday theme's colours while its banner is on, else Bloom's: the sweep
    // (the gradient is made from them when the view is sized) and the fringe
    private void applyPalette(Context ctx) {
        final HolidayPalette holiday = readHolidayPalette(ctx);
        final int fringeLeft;
        final int fringeRight;
        if (holiday != null) {
            mHoloColors = mOnDark ? holiday.onDark : holiday.onLight;
            final int[] fringe = mOnDark ? holiday.fringeOnDark : holiday.fringeOnLight;
            fringeLeft = fringe[0];
            fringeRight = fringe[1];
        } else {
            mHoloColors = mOnDark ? BLOOM_ON_DARK : BLOOM_ON_LIGHT;
            fringeLeft = mOnDark ? FRINGE_LEFT_ON_DARK : FRINGE_LEFT_ON_LIGHT;
            fringeRight = mOnDark ? FRINGE_RIGHT_ON_DARK : FRINGE_RIGHT_ON_LIGHT;
        }

        // setColor sets the alpha too
        mLeftFringePaint.setColor(fringeLeft);
        mLeftFringePaint.setAlpha(FRINGE_ALPHA);
        mRightFringePaint.setColor(fringeRight);
        mRightFringePaint.setAlpha(FRINGE_ALPHA);
    }

    // Shine and sparkles follow the theme's primary text color: light on dark
    // surfaces, dark on light ones. Forced opaque, the paints set their own alpha.
    private static int resolveInk(Context ctx) {
        final TypedArray a = ctx.obtainStyledAttributes(
                new int[] { android.R.attr.textColorPrimary });
        try {
            return a.getColor(0, 0xFFFFFFFF) | 0xFF000000;
        } finally {
            a.recycle();
        }
    }

    // The KEY_HOLIDAY_PALETTE colours, or null (Bloom) when the key is unset or
    // isn't exactly the five groups it should be.
    private static HolidayPalette readHolidayPalette(Context ctx) {
        final String value;
        try {
            value = Settings.Secure.getString(ctx.getContentResolver(), KEY_HOLIDAY_PALETTE);
        } catch (RuntimeException e) {
            return null;
        }
        if (value == null) return null;

        // -1 keeps empty trailing groups, so "a;b;c;d;e;" counts as six
        final String[] groups = value.trim().split(";", -1);
        if (groups.length != 5 || groups[0].isEmpty() || groups[0].indexOf(',') >= 0) {
            return null;
        }
        final int[] onDark = parseColors(groups[1], 4);
        final int[] onLight = parseColors(groups[2], 4);
        final int[] fringeOnDark = parseColors(groups[3], 2);
        final int[] fringeOnLight = parseColors(groups[4], 2);
        if (onDark == null || onLight == null || fringeOnDark == null || fringeOnLight == null) {
            return null;
        }
        return new HolidayPalette(loop(onDark), loop(onLight), fringeOnDark, fringeOnLight);
    }

    // Exactly count opaque colours from RRGGBB hex split by ',', else null
    private static int[] parseColors(String group, int count) {
        final String[] parts = group.split(",", -1);
        if (parts.length != count) return null;
        final int[] colors = new int[count];
        for (int i = 0; i < count; i++) {
            final String part = parts[i];
            if (part.length() != 6) return null;
            int rgb = 0;
            for (int j = 0; j < 6; j++) {
                final int digit = hexDigit(part.charAt(j));
                if (digit < 0) return null;
                rgb = (rgb << 4) | digit;
            }
            colors[i] = 0xFF000000 | rgb;
        }
        return colors;
    }

    // Only ASCII hex digits (Character.digit also takes other scripts' digits)
    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        return -1;
    }

    // The four sweep colours with the first repeated, for BLOOM_STOPS
    private static int[] loop(int[] colors) {
        return new int[] { colors[0], colors[1], colors[2], colors[3], colors[0] };
    }

    /**
     * Stops the animation ms after it starts (SWEEP_MS for one sweep) and holds
     * that frame, without a shine or sparkle: one that wouldn't be over by then
     * isn't started. It is also the still frame shown with animations off.
     * 0, the default, loops for as long as the view shows.
     */
    public void setAnimationLimit(long ms) {
        mAnimationLimit = Math.max(0L, ms);
        invalidate();
    }

    /**
     * Plays the animation again from the start, in the holiday colours as they
     * are now.
     */
    public void restartAnimation() {
        applyPalette(getContext());
        if (mScale > 0f) rebuildHolo();
        mAnimate = ValueAnimator.areAnimatorsEnabled();
        mStartTime = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Respect "remove animations": draw one still frame instead of looping.
        mAnimate = ValueAnimator.areAnimatorsEnabled();
        mStartTime = SystemClock.uptimeMillis();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mScale = Math.min(w / VIEWPORT_W, h / VIEWPORT_H);
        mOx = (w - VIEWPORT_W * mScale) / 2f;
        mOy = (h - VIEWPORT_H * mScale) / 2f;

        mMatrix.setScale(mScale, mScale);
        mMatrix.postTranslate(mOx, mOy);
        mLogoUnits.transform(mMatrix, mLogo);

        final BlurMaskFilter glowBlur = new BlurMaskFilter(
                blurRadius(GLOW_SIGMA * mScale), BlurMaskFilter.Blur.NORMAL);
        final BlurMaskFilter fringeBlur = new BlurMaskFilter(
                blurRadius(FRINGE_SIGMA * mScale), BlurMaskFilter.Blur.NORMAL);

        mGlowPaint.setStrokeWidth(GLOW_STROKE * mScale);
        mGlowPaint.setMaskFilter(glowBlur);
        mLeftFringePaint.setStrokeWidth(FRINGE_STROKE * mScale);
        mLeftFringePaint.setMaskFilter(fringeBlur);
        mRightFringePaint.setStrokeWidth(FRINGE_STROKE * mScale);
        mRightFringePaint.setMaskFilter(fringeBlur);
        mStrokePaint.setStrokeWidth(MAIN_STROKE * mScale);
        mShineStroke.setStrokeWidth(MAIN_STROKE * mScale);

        rebuildHolo();
    }

    private void rebuildHolo() {
        mHolo = new LinearGradient(
                mOx + GRAD_X1 * mScale, mOy + GRAD_Y1 * mScale,
                mOx + GRAD_X2 * mScale, mOy + GRAD_Y2 * mScale,
                mHoloColors, BLOOM_STOPS,
                Shader.TileMode.REPEAT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mHolo == null || mScale <= 0f) return;

        long t = mAnimate ? SystemClock.uptimeMillis() - mStartTime : 0L;
        // A limited animation holds its last frame, with animations off too
        final boolean limited = mAnimationLimit > 0;
        if (limited && (!mAnimate || t > mAnimationLimit)) t = mAnimationLimit;

        // Sweep the gradient along its own vector, which loops seamlessly.
        final float sweep = (t % SWEEP_MS) / (float) SWEEP_MS;
        mShaderMatrix.setTranslate(
                sweep * (GRAD_X2 - GRAD_X1) * mScale,
                sweep * (GRAD_Y2 - GRAD_Y1) * mScale);
        mHolo.setLocalMatrix(mShaderMatrix);
        mGlowPaint.setShader(mHolo);
        mFillPaint.setShader(mHolo);
        mStrokePaint.setShader(mHolo);

        // Soft glow, then the fringe (blue and violet in Bloom)
        canvas.drawPath(mLogo, mGlowPaint);

        canvas.save();
        canvas.translate(-FRINGE_SHIFT * mScale, 0f);
        canvas.drawPath(mLogo, mLeftFringePaint);
        canvas.restore();

        canvas.save();
        canvas.translate(FRINGE_SHIFT * mScale, 0f);
        canvas.drawPath(mLogo, mRightFringePaint);
        canvas.restore();

        // Glassy fill and gradient outline
        canvas.drawPath(mLogo, mFillPaint);
        canvas.drawPath(mLogo, mStrokePaint);

        drawShine(canvas, t);
        drawSparkles(canvas, t);

        if (mAnimate && (!limited || t < mAnimationLimit)) postInvalidateOnAnimation();
    }

    private void drawShine(Canvas canvas, long t) {
        final float p = (t % SHINE_MS) / (float) SHINE_MS;
        if (p >= SHINE_ACTIVE || !endsInTime(t, SHINE_MS, SHINE_ACTIVE)) return;

        final float bx = SHINE_FROM + (SHINE_TO - SHINE_FROM) * (p / SHINE_ACTIVE);

        final int layer = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        canvas.drawPath(mLogo, mShineFill);
        canvas.drawPath(mLogo, mShineStroke);

        // Keep only the part of the white logo that sits under the band
        canvas.save();
        canvas.translate(mOx, mOy);
        canvas.scale(mScale, mScale);
        canvas.skew(SHINE_SKEW, 0f);
        mShaderMatrix.setTranslate(bx, 0f);
        mBand.setLocalMatrix(mShaderMatrix);
        mBandPaint.setShader(mBand);
        canvas.drawRect(-SHINE_REACH, -SHINE_REACH, SHINE_REACH, SHINE_REACH, mBandPaint);
        canvas.restore();
        canvas.restoreToCount(layer);
    }

    private void drawSparkles(Canvas canvas, long t) {
        final float p = (t % SPARKLE_MS) / (float) SPARKLE_MS;
        for (float[] s : SPARKLES) {
            final float k = pulse(p, s[3], s[4], s[5]) * s[2] * mScale;
            if (k <= 0f || !endsInTime(t, SPARKLE_MS, s[5])) continue;
            canvas.save();
            canvas.translate(mOx + s[0] * mScale, mOy + s[1] * mScale);
            canvas.scale(k, k);
            canvas.drawPath(mStar, mSparklePaint);
            canvas.restore();
        }
    }

    // Whether what ends at end (a fraction of period) in t's period is over by the
    // animation limit, so the animation never stops on a half-lit shine or sparkle
    private boolean endsInTime(long t, long period, float end) {
        return mAnimationLimit <= 0 || t - t % period + (long) (end * period) <= mAnimationLimit;
    }

    // 0 at start and end, 1 at peak, linear in between
    private static float pulse(float p, float start, float peak, float end) {
        if (p <= start || p >= end) return 0f;
        return p < peak ? (p - start) / (peak - start) : (end - p) / (end - peak);
    }

    // BlurMaskFilter takes a radius, Skia converts it with sigma = 0.57735 * r + 0.5
    private static float blurRadius(float sigma) {
        return Math.max(1f, (sigma - 0.5f) / 0.57735f);
    }

    // A holiday theme's banner colours: 5-stop sweeps and { left, right } fringes
    private static final class HolidayPalette {
        final int[] onDark;
        final int[] onLight;
        final int[] fringeOnDark;
        final int[] fringeOnLight;

        HolidayPalette(int[] onDark, int[] onLight, int[] fringeOnDark, int[] fringeOnLight) {
            this.onDark = onDark;
            this.onLight = onLight;
            this.fringeOnDark = fringeOnDark;
            this.fringeOnLight = fringeOnLight;
        }
    }
}
