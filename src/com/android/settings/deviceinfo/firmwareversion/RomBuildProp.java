/*
 * SPDX-FileCopyrightText: ExpressiveOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion;

import android.os.SystemProperties;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reads one of the ROM's own props. Integrity modules (Integrity Box's custom-ROM hiding) delete
 * them at runtime, but the build.prop files they came from are untouched, so fall back to those.
 */
final class RomBuildProp {

    private static final String[] BUILD_PROP_FILES = {
            "/product/etc/build.prop",
            "/system/build.prop",
    };

    private RomBuildProp() {}

    static String get(String name, String def) {
        final String value = SystemProperties.get(name, "");
        if (!value.isEmpty()) {
            return value;
        }
        final String prefix = name + "=";
        for (String path : BUILD_PROP_FILES) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(prefix)) {
                        return line.substring(prefix.length()).trim();
                    }
                }
            } catch (IOException e) {
                // Try the next file.
            }
        }
        return def;
    }
}
