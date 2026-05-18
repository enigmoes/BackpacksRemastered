/*
 * BackpacksRemastered - remastered version of the popular Backpacks plugin
 * Copyright (C) 2019, Andrew Howard, <divisionind.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.divisionind.bprm.nms;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A list of known versions. This gives us a history of how one version progressed to the next which
 * is useful for quickly resolving NMS functionality at runtime. It's why we can support so many versions.
 *
 * This requires that our list of previous versions be comprehensive back to min_supported_version, but allows for
 * future unknown versions (not in the enum) to fall back to whatever the latest NMS revision is. Hence, why there
 * is no .after().
 */
public enum KnownVersion {

    v1_9_R1,
    v1_9_R2,
    v1_10_R1,
    v1_11_R1,
    v1_12_R1,
    v1_13_R1,
    v1_13_R2,
    v1_14_R1,
    v1_15_R1,
    v1_16_R1,
    v1_16_R2,
    v1_16_R3,
    v1_17_R1,
    v1_18_R1,
    v1_18_R2,
    v1_19_R1,
    v1_20_R1,
    v1_20_R2,
    v1_20_R3,
    v1_21_R1,
    v1_21_R2,
    v1_21_R3,
    v1_21_R4,
    v1_21_R5,
    v1_21_R6,
    v1_21_R7,
    ;

    private static final KnownVersion KVERSION;
    public static final String VERSION;
    private static final boolean HAS_VERSIONED_PACKAGE;
    private static final Pattern CRAFT_VERSION_PATTERN = Pattern.compile("v\\d+_\\d+_R\\d+");

    static {
        String vtmp = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = vtmp.substring(vtmp.lastIndexOf('.') + 1);
        HAS_VERSIONED_PACKAGE = CRAFT_VERSION_PATTERN.matcher(VERSION).matches();

        KnownVersion resolved = resolveByIdentifier(VERSION);
        if (resolved == null) {
            resolved = resolveFromBukkitVersion();
        }
        if (resolved == null) {
            KnownVersion[] versions = values();
            resolved = versions[versions.length - 1];
        }
        KVERSION = resolved;
    }

    private static KnownVersion resolveFromBukkitVersion() {
        String bukkitVersion = Bukkit.getBukkitVersion();
        // Try major.minor.patch first (e.g. 1.21.3 -> v1_21_R2)
        Matcher matcher3 = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)").matcher(bukkitVersion);
        if (matcher3.find()) {
            // Known patch → R-revision mappings for 1.21.x
            int major = Integer.parseInt(matcher3.group(1));
            int minor = Integer.parseInt(matcher3.group(2));
            int patch = Integer.parseInt(matcher3.group(3));
            if (major == 1 && minor == 21) {
                String identifier;
                if (patch <= 1) identifier = "v1_21_R1";
                else if (patch <= 2) identifier = "v1_21_R2";
                else if (patch <= 3) identifier = "v1_21_R3";
                else if (patch <= 4) identifier = "v1_21_R4";
                else if (patch <= 5) identifier = "v1_21_R5";
                else if (patch <= 10) identifier = "v1_21_R6";
                else identifier = "v1_21_R7";
                KnownVersion v = resolveByIdentifier(identifier);
                if (v != null) return v;
            }
        }
        // Fall back to major.minor only
        Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)").matcher(bukkitVersion);
        if (!matcher.find()) {
            return null;
        }

        String identifier = String.format("v%s_%s_R1", matcher.group(1), matcher.group(2));
        return resolveByIdentifier(identifier);
    }

    private static KnownVersion resolveByIdentifier(String identifier) {
        try {
            return valueOf(identifier);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * @return whether the current server version is before the current instance's version (non-inclusive)
     */
    public boolean before() {
        return KVERSION.ordinal() < ordinal();
    }

    public static boolean hasVersionedCraftPackage() {
        return HAS_VERSIONED_PACKAGE;
    }
}
