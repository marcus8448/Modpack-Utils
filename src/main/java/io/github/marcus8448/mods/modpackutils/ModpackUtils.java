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

import com.google.common.collect.Lists;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.file.BasicCurseFile;
import com.therandomlabs.curseapi.file.CurseFile;
import com.therandomlabs.curseapi.file.CurseReleaseType;
import com.therandomlabs.curseapi.minecraft.CurseAPIMinecraft;
import com.therandomlabs.curseapi.minecraft.modpack.CurseModpack;
import com.therandomlabs.curseapi.project.CurseProject;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Mod("modpackutils")
public class ModpackUtils {
    static final Logger LOGGER = LogManager.getLogger("Modpack Utils");
    private static final Marker INFODUMP = MarkerManager.getMarker("INFODUMP");

    public ModpackUtils() {
        CurseAPIMinecraft.initialize();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.HIGHEST, this::setup);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModpackUtilsConfig.commonSpec);
        FMLJavaModLoadingContext.get().getModEventBus().register(ModpackUtilsConfig.class);
    }

    private void setup(final FMLCommonSetupEvent event) {
        ModpackVersionVerifier.init();

        LOGGER.info(INFODUMP, "|--------------------------------------------------------------------------------------|");
        LOGGER.info(INFODUMP, "Modpack Utils Info Dump:\n");
        LOGGER.info(INFODUMP, "Modpack Name: {}", ModpackUtilsConfig.COMMON.packName.get());
        LOGGER.info(INFODUMP, "Modpack Version: {}", ModpackUtilsConfig.COMMON.packVersion.get());
        LOGGER.info(INFODUMP, "Mod Count: {}", ModList.get().getMods().size()); //FORGE + MINECRAFT count as 2
        LOGGER.info(INFODUMP, "|--------------------------------------------------------------------------------------|");

        try {
            if (ModpackUtilsConfig.COMMON.compareModpack.get()) {
                long millitime = System.currentTimeMillis();
                try {
                    Optional<CurseProject> project = CurseAPI.project(ModpackUtilsConfig.COMMON.packID.get());

                    if (project.isPresent()) {
                        for (CurseFile curseFile : project.get().files()) {
                            if ((curseFile.nameOnDisk().replace(".zip", "").replace(ModpackUtilsConfig.COMMON.packName.get(), "").toLowerCase().contains(ModpackUtilsConfig.COMMON.packVersion.get().toLowerCase()) ||
                                    (curseFile.displayName().replace(ModpackUtilsConfig.COMMON.packName.get(), "").toLowerCase().contains(ModpackUtilsConfig.COMMON.packVersion.get().toLowerCase())))) {

                                if (!new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "modpackutils-files" + File.separator + curseFile.nameOnDisk().replace(".zip", "")).exists() &&
                                        !new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "modpackutils-files" + File.separator + curseFile.nameOnDisk()).exists()) {
                                    LOGGER.info("Downloading pack...");
                                    CurseAPI.downloadFileToDirectory(curseFile.projectID(), curseFile.id(), new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "modpackutils-files" + File.separator).toPath());
                                }

                                File zipFilePath;

                                if (new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "modpackutils-files" + File.separator + curseFile.nameOnDisk()).exists()) {
                                    zipFilePath = new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "modpackutils-files" + File.separator + curseFile.nameOnDisk());
                                } else {
                                    zipFilePath = new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "modpackutils-files" + File.separator + curseFile.nameOnDisk().replace(".zip", ""));
                                }

                                File dir = new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "modpackutils-files" + File.separator + "tmp" + File.separator);
                                if (dir.exists()) {
                                    dir.delete();
                                    dir.mkdirs();
                                }
                                File newFile = null;
                                String fileName;
                                byte[] buffer = new byte[4096];
                                try {
                                    ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath));
                                    ZipEntry ze = zis.getNextEntry();

                                    while (ze != null) {
                                        fileName = ze.getName();
                                        if (fileName.equals("manifest") || fileName.equals("manifest.json")) {
                                            newFile = new File(dir + File.separator + fileName);
                                            if (newFile.exists()) {
                                                newFile.delete();
                                            }
                                            newFile.getParentFile().mkdirs();
                                            newFile.createNewFile();

                                            FileOutputStream fos = new FileOutputStream(newFile);
                                            int len;
                                            while ((len = zis.read(buffer)) > 0) {
                                                fos.write(buffer, 0, len);
                                            }
                                            fos.close();
                                            break;
                                        }
                                        zis.closeEntry();
                                        ze = zis.getNextEntry();
                                    }
                                    zis.closeEntry();
                                    zis.close();
                                } catch (IOException e) {
                                    LOGGER.fatal("Failed to extract pack manifest!");
                                    e.printStackTrace();
                                    break;
                                }

                                assert newFile != null;
                                File manifest = newFile;

                                try {
                                    CurseModpack modpack = CurseModpack.fromJSON(new BufferedReader(new FileReader(manifest)).readLine());

                                    final List<File> mods = Lists.newArrayList(Objects.requireNonNull(FMLPaths.MODSDIR.get().toFile().listFiles()));
                                    List<File> mods1 = Lists.newArrayList(Objects.requireNonNull(FMLPaths.MODSDIR.get().toFile().listFiles()));
                                    final List<CurseFile> files = new ArrayList<>();
                                    final List<CurseFile> missingMods = new ArrayList<>();
                                    int iter = 0;
                                    long avg = 0;
                                    LOGGER.info("Matching mod ids...");
                                    long mtt = System.currentTimeMillis();
                                    for (BasicCurseFile file : modpack.files()) {
                                        iter++;
                                        long mt = System.currentTimeMillis();
                                        CurseFile cf = file.toCurseFile();
                                        avg += System.currentTimeMillis() - mt;
                                        LOGGER.debug("Time taken: {}ms. Average: {}ms", System.currentTimeMillis() - mt, avg / iter);
                                        files.add(cf);
                                        missingMods.add(cf);
                                    }

                                    LOGGER.info("Done matching! Time taken: {}ms", System.currentTimeMillis() - mtt);


                                    for (CurseFile file : files) {
                                        for (File mod : mods1) {
                                            if (file.nameOnDisk().replace(".jar", "").equals(mod.getName()) || (file.nameOnDisk()).equals(mod.getName()) || file.nameOnDisk().equals(mod.getName() + ".jar") || file.nameOnDisk().replace(".jar", "").equals(mod.getName() + "jar")) {
                                                missingMods.remove(file);
                                                mods.remove(mod);
                                                break;
                                            }
                                        }
                                        mods1 = new ArrayList<>(mods);
                                    }

                                    ArrayList<String> s = new ArrayList<>();

                                    for (File f : (File[]) mods.toArray()) {
                                        s.add(f.getName());
                                    }

                                    if (!mods.isEmpty()) {
                                        LOGGER.error("Added mods! {}", Arrays.toString(s.toArray()));
                                    }

                                    List<String> mm = new ArrayList<>();

                                    for (CurseFile file : missingMods) {
                                        mm.add(file.nameOnDisk().replace(".jar", ""));
                                    }

                                    if (!mm.isEmpty()) {
                                        LOGGER.warn("Missing mods! {}", Arrays.toString(mm.toArray()));
                                    }

                                    if (mods.isEmpty() && mm.isEmpty()) {
                                        LOGGER.info("Mod list seems legit.");
                                    }

                                } catch (CurseException | NullPointerException e) {
                                    LOGGER.fatal("Failed to compare modlists!");
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    } else {
                        throw new CurseException("Optional was empty... Invalid pack id?");
                    }
                } catch (CurseException | IllegalArgumentException e) {
                    LOGGER.fatal("Failed to download CF pack!");
                    e.printStackTrace();
                } catch (IOException e) {
                    LOGGER.fatal("Failed to read manifest!");
                    e.printStackTrace();
                }
                LOGGER.info("Took {}ms to match CF pack/mods.", System.currentTimeMillis() - millitime);
                LOGGER.info(INFODUMP, "|--------------------------------------------------------------------------------------|");
            }
        } catch (Throwable t) {
            LOGGER.fatal("Uncaught exception!");
            t.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void sendUpdateMessage(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity && ModpackUtilsConfig.COMMON.updateReminder.get()) {
            try {
                if (ModpackUtilsConfig.COMMON.packID.get() > 10) {
                    Optional<CurseProject> project = CurseAPI.project(ModpackUtilsConfig.COMMON.packID.get());
                    if (project.isPresent()) {
                        CurseFile mostRecent = null;
                        CurseFile thisOne = null;
                        for (CurseFile curseFile : project.get().files()) {
                            if (((curseFile.releaseType() == CurseReleaseType.ALPHA && ModpackUtilsConfig.COMMON.updateAlpha.get()) || (curseFile.releaseType() == CurseReleaseType.BETA && ModpackUtilsConfig.COMMON.updateBeta.get()) || (curseFile.releaseType() == CurseReleaseType.RELEASE))) {
                                if (mostRecent == null) {
                                    mostRecent = curseFile;
                                } else {
                                    if (mostRecent.uploadTime().isBefore(curseFile.uploadTime())) {
                                        mostRecent = curseFile;
                                    }
                                }

                                if (curseFile.nameOnDisk().replace(".zip", "").contains(ModpackUtilsConfig.COMMON.packVersion.get())) {
                                    thisOne = curseFile;
                                }
                            }
                        }

                        if (mostRecent == null) {
                            throw new IllegalArgumentException("Couldn't find any valid files on CurseForge!");
                        } else {
                            if (thisOne == null) {
                                LOGGER.error("Failed to find this modpack on CurseForge!");
                            } else {
                                int i = thisOne.nameOnDisk().indexOf(ModpackUtilsConfig.COMMON.packVersion.get());

                                String name = mostRecent.nameOnDisk().replace(".zip", "");

                                String version = name.substring(i - 1);

                                LOGGER.info(version);

                                if (!version.equals(ModpackUtilsConfig.COMMON.packVersion.get())) {
                                    LOGGER.warn("Currently running {}-{}. Latest is {}!", ModpackUtilsConfig.COMMON.packName, ModpackUtilsConfig.COMMON.packVersion, name);
                                    event.getEntity().sendMessage(new TranslationTextComponent("modpackutils.message.new_update", ModpackUtilsConfig.COMMON.packName, ModpackUtilsConfig.COMMON.packID, ModpackUtilsConfig.COMMON.packVersion, version));
                                }
                            }
                        }


                    }
                } else {
                    throw new IllegalArgumentException("Invalid curseforge pack id! " + ModpackUtilsConfig.COMMON.packID);
                }
            } catch (Exception e) {
                LOGGER.error("FAILED TO GET PROJECT INFO");
                e.printStackTrace();
            }
        }
    }
}
