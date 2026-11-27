/*
 * Copyright (C) 2026 AxionOS
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.display

import android.app.WallpaperManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemProperties
import android.provider.Settings
import android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.widget.scrollbar
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.search.SearchIndexableRaw
import com.android.settingslib.spa.framework.theme.settingsBackground
import com.android.settingslib.spa.framework.theme.settingsCardBackground
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.expressiveos.settings.fragments.miscellaneous.ExpressiveCorners
import org.expressiveos.settings.fragments.miscellaneous.ExpressiveGroup
import org.expressiveos.settings.fragments.miscellaneous.ExpressiveSwitchRow
import org.expressiveos.settings.fragments.miscellaneous.ExpressiveTheme

@SearchIndexable
class BlurSettings : SettingsPreferenceFragment() {

    override fun getMetricsCategory(): Int = SettingsEnums.DISPLAY

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Compose fragment, no preference XML resources to load.
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ExpressiveTheme {
                    BlurSettingsScreen()
                }
            }
        }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(com.android.settingslib.R.string.window_blurs)
    }

    @Composable
    fun BlurSettingsScreen() {
        val context = LocalContext.current
        val cr = context.contentResolver

        val blurEnabledByDefault = SystemProperties.getBoolean("ro.custom.blur.enable", false)

        var blursEnabled by remember {
            mutableStateOf(
                Settings.Global.getInt(
                    cr,
                    Settings.Global.DISABLE_WINDOW_BLURS,
                    if (blurEnabledByDefault) 0 else 1,
                ) == 0
            )
        }

        var blurRadiusPct by remember {
            mutableFloatStateOf(
                Settings.Secure.getFloat(
                        cr,
                        KEY_SYSTEM_BLUR_RADIUS_PCT,
                        MAX_BLUR_RADIUS_PCT,
                    )
                    .takeIf { it.isFinite() }
                    ?.coerceIn(MIN_BLUR_RADIUS_PCT, MAX_BLUR_RADIUS_PCT)
                    ?: MAX_BLUR_RADIUS_PCT
            )
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.settingsBackground,
        ) { paddingValues ->
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .scrollbar(listState.scrollIndicatorState, Orientation.Vertical),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            ) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        BlurIllustration(blursEnabled, blurRadiusPct)
                    }
                }

                item {
                    ExpressiveGroup(title = "") {
                        item {
                            ExpressiveSwitchRow(
                                titleText = stringResource(com.android.settingslib.R.string.window_blurs),
                                summaryText = stringResource(R.string.blur_settings_summary),
                                isChecked = blursEnabled,
                                onChange = { newValue ->
                                    blursEnabled = newValue
                                    Settings.Global.putInt(
                                        cr,
                                        Settings.Global.DISABLE_WINDOW_BLURS,
                                        if (newValue) 0 else 1,
                                    )
                                },
                            )
                        }
                        item {
                            val pctLabel = "${blurRadiusPct.roundToInt()}%"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (blursEnabled) 1f else 0.60f),
                                shape = ExpressiveCorners.last,
                                color = MaterialTheme.colorScheme.settingsCardBackground,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.blur_strength_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = pctLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Slider(
                                        value = blurRadiusPct,
                                        onValueChange = { blurRadiusPct = it },
                                        onValueChangeFinished = {
                                            Settings.Secure.putFloat(
                                                cr,
                                                KEY_SYSTEM_BLUR_RADIUS_PCT,
                                                blurRadiusPct,
                                            )
                                        },
                                        valueRange = MIN_BLUR_RADIUS_PCT..MAX_BLUR_RADIUS_PCT,
                                        enabled = blursEnabled,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .semantics {
                                                stateDescription = pctLabel
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BlurIllustration(enabled: Boolean, radiusPct: Float) {
        val context = LocalContext.current

        val layerFg = Color(context.getColor(com.android.internal.R.color.shade_panel_fg))
        val layerBg = Color(context.getColor(com.android.internal.R.color.shade_panel_bg))
        val compositeColor = ColorUtils.compositeColors(layerFg.toArgb(), layerBg.toArgb())
        val shadeColor = Color(compositeColor)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    RoundedCornerShape(28.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(280.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(4.dp, Color.Black.copy(alpha = 0.9f), RoundedCornerShape(32.dp))
                    .background(Color.Black)
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(26.dp)),
                    ) {
                        WallpaperImage(
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(
                                    (MAX_PREVIEW_BLUR_RADIUS_DP * radiusPct /
                                        MAX_BLUR_RADIUS_PCT).dp
                                ),
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (enabled) shadeColor
                                    else MaterialTheme.colorScheme.surfaceContainer
                                ),
                        )

                        QSContent()
                    }

                    StatusBar()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                )
            }
        }
    }

    @Composable
    fun WallpaperImage(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val wallpaperManager = remember { WallpaperManager.getInstance(context) }
        val drawable = remember {
            try {
                wallpaperManager.drawable
            } catch (e: Exception) {
                null
            }
        }

        if (drawable != null) {
            val painter = rememberDrawablePainter(drawable)
            Image(
                painter = painter,
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = modifier.background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        )
                    )
                )
            )
        }
    }

    @Composable
    fun rememberDrawablePainter(drawable: Drawable?): Painter {
        return remember(drawable) {
            object : Painter() {
                override val intrinsicSize: Size
                    get() = drawable?.let {
                        Size(it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat())
                    } ?: Size.Unspecified

                override fun DrawScope.onDraw() {
                    drawable?.let {
                        it.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                        it.draw(drawContext.canvas.nativeCanvas)
                    }
                }
            }
        }
    }

    @Composable
    fun StatusBar() {
        val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
        var time by remember { mutableStateOf(LocalTime.now().format(formatter)) }

        LaunchedEffect(Unit) {
            while (true) {
                time = LocalTime.now().format(formatter)
                delay(1000 * 30)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                time,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp, 6.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                )
            }
        }
    }

    @Composable
    fun QSContent() {
        val icons = listOf(
            Icons.Filled.Wifi,
            Icons.Filled.Bluetooth,
            Icons.Filled.FlashlightOn,
            Icons.Filled.Settings,
            Icons.Filled.AirplanemodeActive,
            Icons.Filled.Nfc,
            Icons.Filled.Adb,
            Icons.Filled.NotificationsActive,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(4) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(2) { colIndex ->
                        val iconIndex = (rowIndex * 2 + colIndex) % icons.size
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.settingsCardBackground)
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                    RoundedCornerShape(16.dp),
                                ),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = icons[iconIndex],
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .height(2.5.dp)
                                        .weight(1f)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.settingsCardBackground),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Icon(
                    imageVector = Icons.Filled.Brightness4,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp).size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.settingsCardBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    companion object {
        private const val KEY_SYSTEM_BLUR_RADIUS_PCT = "system_blur_radius_pct"
        private const val MIN_BLUR_RADIUS_PCT = 0f
        private const val MAX_BLUR_RADIUS_PCT = 100f
        private const val MAX_PREVIEW_BLUR_RADIUS_DP = 34f

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider() {
                override fun isPageSearchEnabled(context: Context): Boolean =
                    CROSS_WINDOW_BLUR_SUPPORTED

                override fun getRawDataToIndex(
                    context: Context,
                    enabled: Boolean,
                ): List<SearchIndexableRaw> {
                    if (!CROSS_WINDOW_BLUR_SUPPORTED) return emptyList()
                    val screenTitle = context.getString(com.android.settingslib.R.string.window_blurs)
                    return listOf(
                        SearchIndexableRaw(context).apply {
                            key = "blur_settings_toggle"
                            title = screenTitle
                            summaryOn = context.getString(R.string.blur_settings_summary)
                            this.screenTitle = screenTitle
                        },
                        SearchIndexableRaw(context).apply {
                            key = "blur_settings_strength"
                            title = context.getString(R.string.blur_strength_title)
                            summaryOn = context.getString(R.string.blur_settings_summary)
                            this.screenTitle = screenTitle
                        },
                    )
                }
            }
    }
}
