/*
 * Modpack Utils
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

package io.github.marcus8448.mods.modpackutils;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class ModpackUtilsConfig {
    public static final ModpackUtilsConfig.Common COMMON;
    static final ForgeConfigSpec commonSpec;
    private static final Marker CONFIG = MarkerManager.getMarker("CONFIG");
    private static byte flag = 0;

    static {
        Pair<ModpackUtilsConfig.Common, ForgeConfigSpec> specPair = (new ForgeConfigSpec.Builder()).configure(ModpackUtilsConfig.Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onLoad(ModConfig.Loading configEvent) {
        ModpackUtils.LOGGER.debug(CONFIG, "Successfully loaded PackVersionUtils' config file!");
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onFileChange(ModConfig.ConfigReloading configEvent) {
        if (flag > 1) {
            ModpackUtils.LOGGER.fatal(CONFIG, "PackVersionUtils' config just got changed on the file system! This shouldn't happen!");
        } else {
            flag++;
        }
    }

    public static class Common {
        final ForgeConfigSpec.ConfigValue<String> packName;
        final ForgeConfigSpec.ConfigValue<String> packVersion;
        final ForgeConfigSpec.ConfigValue<Integer> packID;

        final ForgeConfigSpec.BooleanValue compareModpack;

        final ForgeConfigSpec.BooleanValue updateReminder;
        final ForgeConfigSpec.BooleanValue updateBeta;
        final ForgeConfigSpec.BooleanValue updateAlpha;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Pack info config").push("info");
            this.packName = builder.comment("Set this to the name of your modpack").translation("modpackutils.config.info.name").worldRestart().define("packName", "[NOT SET]");
            this.packVersion = builder.comment("Set this to your modpacks current version. This will be used to verify/compare the\n" +
                    "the loaded mods to the ones specified online. This will not work if you have the version appear in the pack name or have versions 1.0 and 1.0b (for example).\n" +
                    "Good examples: 'PackName-1.0.0', '1.0.0-PackName' Pac10+2_x0N0.aM>e1.0.0 (1.0.0 is the version).\n" +
                    "Bad examples: 'PackName-1.0' when 'PackName-1.0.1' exists in the same project. (if someone is playing '1.0' the mod will match it with '1.0.1' causing conflicts - just name it 1.0.0 and 1.0.1").translation("modpackutils.config.info.version").worldRestart().define("packVersion", "1.0.0");
            this.packID = builder.comment("Set this to your modpacks ID (the number) (must be greater than 10)").translation("modpackutils.config.info.id").worldRestart().define("packID", Integer.MIN_VALUE);
            builder.pop();
            builder.comment("Optional features").push("optional");
            this.compareModpack = builder.comment("If you turn this on, this mod will download the pack manifest (based off of your pack ID and version).\n" +
                    "The file is downloaded once at startup, and not downloaded ever again (unless it's deleted).\n" +
                    "This can be useful for determining the verifiability of the pack in bug reports").translation("modpackutils.config.optional.compare_pack").worldRestart().define("comparePack", true);
            this.updateReminder = builder.comment("Set this to true if you want to be reminded when the modpack has a new update").translation("modpackutils.config.optional.update.enable").worldRestart().define("updateReminder", true);
            this.updateBeta = builder.comment("Set this to true if you want to be reminded for Beta modpack updates too (requires updateReminder to be true)").translation("modpackutils.config.optional.update.beta").worldRestart().define("updateBeta", false);
            this.updateAlpha = builder.comment("Set this to true if you want to be reminded for Alpha modpack updates too (requires updateReminder to be true)").translation("modpackutils.config.optional.update.alpha").worldRestart().define("updateAlpha", false);

            builder.pop();
            ModpackUtils.LOGGER.info(CONFIG, "Successfully initialized the common config!");
        }
    }
}
