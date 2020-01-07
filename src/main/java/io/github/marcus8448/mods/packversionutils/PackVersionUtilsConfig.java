/*
 * Pack Version Utils
 * Copyright (C) 2019-2020 marcus8448
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

package io.github.marcus8448.mods.packversionutils;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class PackVersionUtilsConfig {
    public static final PackVersionUtilsConfig.Common COMMON;
    static final ForgeConfigSpec commonSpec;
    private static final Marker CONFIG = MarkerManager.getMarker("CONFIG");
    private static byte flag = 0;

    static {
        Pair<PackVersionUtilsConfig.Common, ForgeConfigSpec> specPair = (new ForgeConfigSpec.Builder()).configure(PackVersionUtilsConfig.Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    @SubscribeEvent
    public static void onLoad(ModConfig.Loading configEvent) {

        PackVersionUtils.LOGGER.debug(CONFIG, "Successfully loaded PackVersionUtils' config file!");
    }

    @SubscribeEvent
    public static void onFileChange(ModConfig.ConfigReloading configEvent) {
        if (flag > 1) {
            PackVersionUtils.LOGGER.fatal(CONFIG, "PackVersionUtils' config just got changed on the file system! This shouldn't happen!");
        } else {
            flag++;
        }
    }

    public enum NameCheckLocation {
        FILE_NAME,
        DISPLAY_NAME;
    }

    public static class Common {
        final ForgeConfigSpec.ConfigValue<String> packName;
        final ForgeConfigSpec.ConfigValue<String> packVersion;
        final ForgeConfigSpec.ConfigValue<Integer> packID;

        final ForgeConfigSpec.ConfigValue<NameCheckLocation> versionCheckLocation;
        final ForgeConfigSpec.ConfigValue<String> versionSeparator;

        final ForgeConfigSpec.BooleanValue compareModpack;

        final ForgeConfigSpec.BooleanValue updateReminder;
        final ForgeConfigSpec.BooleanValue updateBeta;
        final ForgeConfigSpec.BooleanValue updateAlpha;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Pack info config").push("info");
            this.packName = builder.comment("Set this to the name of your modpack").translation("packversionutils.config.info.name").worldRestart().define("packName", "[NOT SET]");
            this.packVersion = builder.comment("Set this to your modpacks current version").translation("packversionutils.config.info.version").worldRestart().define("packVersion", "1.0.0");
            this.packID = builder.comment("Set this to your modpacks ID (the number)").translation("packversionutils.config.info.id").worldRestart().define("packID", Integer.MIN_VALUE);
            builder.pop();
            builder.comment("Optional features").push("optional");
            this.compareModpack = builder.comment("If you turn this on, this mod will download the pack manifest (based off of your pack ID and version).\n" +
                    "The file is downloaded once at startup, and not downloaded ever again (unless it's deleted).\n" +
                    "This can be useful for determining the verifiability of the pack in bug reports").translation("packversionutils.config.optional.compare_pack").worldRestart().define("comparePack", true);
            this.updateReminder = builder.comment("Set this to true if you want to be reminded when the modpack has a new update").translation("packversionutils.config.optional.update.enable").worldRestart().define("updateReminder", true);
            this.updateBeta = builder.comment("Set this to true if you want to be reminded for Beta modpack updates too (requires updateReminder to be true)").translation("packversionutils.config.optional.update.beta").worldRestart().define("updateBeta", false);
            this.updateAlpha = builder.comment("Set this to true if you want to be reminded for Alpha modpack updates too (requires updateReminder to be true)").translation("packversionutils.config.optional.update.alpha").worldRestart().define("updateAlpha", false);

            this.versionCheckLocation = builder.comment("Where to check for the version on curseforge. 'FILE_NAME' will check the downloaded file's name ex. PackName-1.0.0.zip, " +
                    "\nwhile `DISPLAY_NAME' will check the set name ex 'Pack Name v1.0.0'").translation("packversionutils.config.optional.checkLoc").worldRestart().define("versionCheckLocation", NameCheckLocation.FILE_NAME);
            this.versionSeparator = builder.comment("What separates the pack name from the pack version. ex. 'PackName-1.1.2.3.4' or 'P]a[ck_+N4me-1.0.0' (the '-' is the separator) or 'PackName 1.0.0' if ' ' is the separator." +
                    "\nThis will automatically assume that there is only ONE '-' and the version comes AFTER the dash. '(So, Pack-Name-1.0.0' and '1.0.0-PackName' would be invalid)").translation("packversionutils.config.optional.version_separator").worldRestart().define("versionSeparator", "-");

            builder.pop();
        }
    }
}
