/*
 * SPDX-FileCopyrightText: 2026 The ExpressiveOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display.darkmode

import android.Manifest
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.PreferenceChangeReason

@Suppress("UNCHECKED_CAST")
class DarkModeBlackThemeStorage(private val context: Context) :
    AbstractKeyedDataObservable<String>(), KeyValueStore {

    private val settingsObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                notifyChange(Settings.Secure.EOS_BLACK_THEME, PreferenceChangeReason.VALUE)
            }
        }

    override fun contains(key: String) = key == Settings.Secure.EOS_BLACK_THEME

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val value = Settings.Secure.getInt(context.contentResolver, key, 0) != 0
        return value as T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        val boolValue = value as? Boolean ?: false
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.EOS_BLACK_THEME,
            if (boolValue) 1 else 0,
        )
        notifyChange(key, PreferenceChangeReason.VALUE)
    }

    override fun onFirstObserverAdded() {
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.EOS_BLACK_THEME),
            false,
            settingsObserver,
        )
    }

    override fun onLastObserverRemoved() {
        context.contentResolver.unregisterContentObserver(settingsObserver)
    }

    companion object {
        fun getReadPermissions() = Permissions.EMPTY

        fun getWritePermissions() = Permissions.allOf(Manifest.permission.WRITE_SECURE_SETTINGS)
    }
}
