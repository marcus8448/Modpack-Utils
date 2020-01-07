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

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

class PackVersionVerifier {
    private static final String PROTOCOL_VERSION = PackVersionUtilsConfig.COMMON.packVersion.get() + "-" + PackVersionUtilsConfig.COMMON.packName.get();

    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("packversionutils", "pack_verification"),
            () -> PROTOCOL_VERSION,
            PackVersionVerifier::checkVersion,
            PROTOCOL_VERSION::equals
    );

    static void init() {
    }

    private static boolean checkVersion(String to) {
        if (PROTOCOL_VERSION.equals(to)) {
            return true;
        } else {
            PackVersionUtils.LOGGER.fatal("Attempted to join server with pack version: {}. But, this client is on {}. This will not work!", to, PROTOCOL_VERSION);
            return false;
        }
    }
}
