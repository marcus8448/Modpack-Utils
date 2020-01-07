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

import com.google.common.collect.Lists;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.file.BasicCurseFile;
import com.therandomlabs.curseapi.file.CurseFile;
import com.therandomlabs.curseapi.file.CurseFiles;
import com.therandomlabs.curseapi.file.CurseReleaseType;
import com.therandomlabs.curseapi.minecraft.CurseAPIMinecraft;
import com.therandomlabs.curseapi.minecraft.modpack.CurseModpack;
import com.therandomlabs.curseapi.project.CurseProject;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Mod("packversionutils")
public class PackVersionUtils {
    static final Logger LOGGER = LogManager.getLogger("Pack Version Utils");
    private static final Marker INIT = MarkerManager.getMarker("INIT");

    public PackVersionUtils() {
        CurseAPIMinecraft.initialize();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup));
        DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onServerStarting));

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PackVersionUtilsConfig.commonSpec);
        FMLJavaModLoadingContext.get().getModEventBus().register(PackVersionUtilsConfig.class);
    }

    public static String encrypt(String strToEncrypt) { // just so that people dont just add mods
        try {
            byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec("superSecretKey".toCharArray(), "salt".getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public static String decrypt(String strToDecrypt) {
        try {
            byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec("superSecretKey".toCharArray(), "salt".getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info(INIT, "|--------------------------------------------------------------------------------------|");
        LOGGER.info(INIT, " _____           _     __      __           _               _    _ _   _ _     ");
        LOGGER.info(INIT, "|  __ \\         | |    \\ \\    / /          (_)             | |  | | | (_) |    ");
        LOGGER.info(INIT, "| |__) |_ _  ___| | __  \\ \\  / /__ _ __ ___ _  ___  _ __   | |  | | |_ _| |___ ");
        LOGGER.info(INIT, "|  ___/ _` |/ __| |/ /   \\ \\/ / _ \\ '__/ __| |/ _ \\| '_ \\  | |  | | __| | / __|");
        LOGGER.info(INIT, "| |  | (_| | (__|   <     \\  /  __/ |  \\__ \\ | (_) | | | | | |__| | |_| | \\__ \\");
        LOGGER.info(INIT, "|_|   \\__,_|\\___|_|\\_\\     \\/ \\___|_|  |___/_|\\___/|_| |_|  \\____/ \\__|_|_|___/");
        LOGGER.info(INIT, "|--------------------------------------------------------------------------------------|");
        LOGGER.info(INIT, "Modpack Name: {}", PackVersionUtilsConfig.COMMON.packName.get());
        LOGGER.info(INIT, "Modpack Version: {}", PackVersionUtilsConfig.COMMON.packVersion.get());
        LOGGER.info(INIT, "Mod Count: {}", ModList.get().getMods().size()); //FORGE + MINECRAFT count as 2
        LOGGER.info(INIT, "|--------------------------------------------------------------------------------------|");

        if (PackVersionUtilsConfig.COMMON.compareModpack.get()) {
            try {
                Optional<CurseProject> project = CurseAPI.project(PackVersionUtilsConfig.COMMON.packID.get());

                if (project.isPresent()) {
                    for (CurseFile curseFile : project.get().files()) {

                        String name;
                        if (PackVersionUtilsConfig.COMMON.versionCheckLocation.get() == PackVersionUtilsConfig.NameCheckLocation.FILE_NAME) {
                            name = curseFile.nameOnDisk();
                        } else {
                            name = curseFile.displayName();
                        }
                        String version = "";
                        try {
                            version = name.split(PackVersionUtilsConfig.COMMON.versionSeparator.get())[1];
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            LOGGER.error("Failed to split version! (Try loading the game again). If this persists report it to https://github.com/marcus8448/Pack-Utils/issues");
                        }

                        if (!version.equals(PackVersionUtilsConfig.COMMON.packVersion.get())) {
                            continue;
                        }
                        new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "packutils-files" + File.separator).mkdirs();

                        if (!new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "packutils-files" + File.separator + curseFile.nameOnDisk()).exists() &&
                                !new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "packutils-files" + File.separator + curseFile.nameOnDisk() + ".zip").exists()) {
                            CurseAPI.downloadFileToDirectory(curseFile.projectID(), curseFile.id(), new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "packutils-files" + File.separator).toPath()).get();
                        }
                        File zipFilePath;

                        if (new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "packutils-files" + File.separator + curseFile.nameOnDisk() + ".zip").exists()) {
                            zipFilePath = new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "packutils-files" + File.separator + curseFile.nameOnDisk() + ".zip");
                        } else {
                            zipFilePath = new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "packutils-files" + File.separator + curseFile.nameOnDisk());
                        }

                        File dir = new File(FMLPaths.GAMEDIR.get().toString() + File.separator + "packutils-files" + File.separator + "tmp" + File.separator);
                        if (dir.exists()) {
                            dir.delete();
                            dir.mkdirs();
                        }

                        byte[] buffer = new byte[1024];
                        try {
                            FileInputStream fis = new FileInputStream(zipFilePath);
                            ZipInputStream zis = new ZipInputStream(fis);
                            ZipEntry ze = zis.getNextEntry();
                            while (ze != null) {
                                String fileName = ze.getName();
                                if (fileName.equals("manifest") || fileName.equals("manifest.json")) {
                                    File newFile = new File(dir + File.separator + fileName);
                                    if (new File(dir + File.separator + fileName).exists()) {
                                        new File(dir + File.separator + fileName).delete();
                                    }
                                    new File(dir + File.separator).mkdirs();
                                    new File(dir + File.separator + fileName).createNewFile();
                                    LOGGER.debug("Found pack manifest! Unzipping to: {}", newFile.getAbsolutePath());

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
                            //close last ZipEntry
                            zis.closeEntry();
                            zis.close();
                            fis.close();
                        } catch (IOException e) {
                            LOGGER.fatal("Failed to extract pack manifest!");
                            e.printStackTrace();
                            break;
                        }

                        File manifest;
                        if (new File(dir + File.separator + "manifest.json").exists()) {
                            manifest = new File(dir + File.separator + "manifest.json");
                        } else {
                            manifest = new File(dir + File.separator + "manifest");
                        }

                        try {
                            CurseModpack modpack = CurseModpack.fromJSON(new BufferedReader(new FileReader(manifest)).readLine());

                            List<File> mods = Lists.newArrayList(Objects.requireNonNull(FMLPaths.MODSDIR.get().toFile().listFiles()));
                            File[] mods1 = FMLPaths.MODSDIR.get().toFile().listFiles();
                            CurseFiles<BasicCurseFile> expectedMods = modpack.files();
                            for (BasicCurseFile basicCurseFile : modpack.files()) {
                                for (File mod : Objects.requireNonNull(mods1)) {
                                    if (basicCurseFile.toCurseFile().nameOnDisk().equals(mod.getName()) || (basicCurseFile.toCurseFile().nameOnDisk() + ".jar").equals(mod.getName()) ||
                                            basicCurseFile.toCurseFile().nameOnDisk().equals(mod.getName() + ".jar")) {
                                        expectedMods.remove(basicCurseFile);
                                        mods.remove(mod);
                                    }
                                }
                            }

                            if (!mods.isEmpty()) {
                                LOGGER.error("Missing mods! {}", Arrays.toString(mods.toArray()));
                            }
                            List<String> em = new ArrayList<>();

                            for (BasicCurseFile file : expectedMods) {
                                em.add(file.toCurseFile().nameOnDisk());
                            }

                            if (!expectedMods.isEmpty()) {
                                LOGGER.warn("Added mods! {}", Arrays.toString(em.toArray()));
                            }

                            if (mods.isEmpty() && expectedMods.isEmpty()) {
                                LOGGER.info("Mod list seems legit.");
                            }
                        } catch (CurseException | NullPointerException e) {
                            LOGGER.fatal("Failed to compare modlists!");
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            } catch (CurseException e) {
                LOGGER.fatal("Failed to download CF pack!");
                e.printStackTrace();
            } catch (IOException e) {
                LOGGER.fatal("Failed to read manifest!");
                e.printStackTrace();
            }
        }
//        if (PackVersionUtilsConfig.COMMON.saveModList.get()) {
//            try {
//                File modListFile = new File(FMLPaths.CONFIGDIR.get().toString() + File.separator + "modlist.txt");
//                List<ModInfo> mods = ModList.get().getMods();
//                List<String> extra = new ArrayList<>();
//                List<String> missing = new ArrayList<>();
//                if (modListFile.exists()) {
//                    ArrayList<String> expectedMods = Lists.newArrayList(decrypt(new BufferedReader(new FileReader(modListFile)).readLine()).split(","));
//                    for (ModInfo info : mods) {
//                        String id = info.getModId() + "@" + info.getVersion();
//                        boolean found = false;
//                        for (String em : expectedMods) {
//                            if (em.equals(id)) {
//                                found = true;
//                                break;
//                            }
//                        }
//                        if (!found) {
//                            extra.add(id);
//                        } else {
//                            expectedMods.remove(id);
//                        }
//                    }
//                    for (ModInfo info : mods) {
//                        missing.add(info.getModId() + "@" + info.getVersion());
//                    }
//                    if (!extra.isEmpty()) {
//                        LOGGER.warn("Extra mods have been found! - {}", Arrays.toString(extra.toArray()));
//                    }
//
//                    if (!missing.isEmpty()) {
//                        LOGGER.fatal("Mods are missing! - {}", Arrays.toString(missing.toArray()));
//                    }
//
//                    if (missing.isEmpty() && extra.isEmpty()) {
//                        LOGGER.info("Modlist seems to match. Modlist code: {}", new BufferedReader(new FileReader(modListFile)).readLine());
//                    }
//                } else {
//                    LOGGER.info("Generating new modlist...");
//                    FileWriter fileWriter = new FileWriter(modListFile);
//                    StringBuilder builder = new StringBuilder();
//                    for (int i = 0; i < mods.size(); i++) {
//                        ModInfo info = mods.get(i);
//                        builder.append(info.getModId()).append("@").append(info.getVersion());
//                        if (i + 1 < mods.size()) {
//                            builder.append(',');
//                        }
//                    }
//                    fileWriter.write(encrypt(builder.toString()));
//                    LOGGER.info("New modlist key: {}", encrypt(builder.toString()));
//                    fileWriter.flush();
//                    fileWriter.close();
//                }
//                LOGGER.info(INIT, "|--------------------------------------------------------------------------------------|");
//            } catch (IOException | NullPointerException ex) {
//                LOGGER.fatal("FAILED TO COMPARE/SAVE MODLIST! THIS FEATURE WILL NOT WORK!");
//                ex.printStackTrace();
//            }
//        }

        PackVersionVerifier.init();
    }

    @OnlyIn(Dist.CLIENT)
    public void clientSetup(final FMLClientSetupEvent event) {

    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    public void onServerStarting(FMLServerStartingEvent event) {

    }

    @SubscribeEvent
    public void sendUpdateMessage(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity && PackVersionUtilsConfig.COMMON.updateReminder.get()) {
            try {
                if (PackVersionUtilsConfig.COMMON.packID.get() > 10) {
                    Optional<CurseProject> project = CurseAPI.project(PackVersionUtilsConfig.COMMON.packID.get());
                    if (project.isPresent()) {
                        CurseFile mostRecent = null;
                        for (CurseFile curseFile : project.get().files()) {
                            if (((curseFile.releaseType() == CurseReleaseType.ALPHA && PackVersionUtilsConfig.COMMON.updateAlpha.get()) || (curseFile.releaseType() == CurseReleaseType.BETA && PackVersionUtilsConfig.COMMON.updateBeta.get()) || (curseFile.releaseType() == CurseReleaseType.RELEASE))) {
                                if (mostRecent == null) {
                                    mostRecent = curseFile;
                                } else {
                                    if (mostRecent.uploadTime().isBefore(curseFile.uploadTime())) {
                                        mostRecent = curseFile;
                                    }
                                }
                            }
                        }
                        if (mostRecent == null) {
                            throw new IllegalArgumentException("Couldn't find any valid files on CurseForge!");
                        } else {
                            String name;
                            if (PackVersionUtilsConfig.COMMON.versionCheckLocation.get() == PackVersionUtilsConfig.NameCheckLocation.FILE_NAME) {
                                name = mostRecent.nameOnDisk();

                            } else {
                                name = mostRecent.displayName();
                            }

                            String version = name.split(PackVersionUtilsConfig.COMMON.versionSeparator.get())[1];

                            if (!version.equals(PackVersionUtilsConfig.COMMON.packVersion.get())) {
                                LOGGER.warn("Currently running {} (CF ID: {}), version {}. Latest is {}!", PackVersionUtilsConfig.COMMON.packName, PackVersionUtilsConfig.COMMON.packID, PackVersionUtilsConfig.COMMON.packVersion, version);
                                event.getEntity().sendMessage(new TranslationTextComponent("packversionutils.message.new_update", PackVersionUtilsConfig.COMMON.packName, PackVersionUtilsConfig.COMMON.packID, PackVersionUtilsConfig.COMMON.packVersion, version));
                            }

                        }


                    }
                } else {
                    throw new IllegalArgumentException("Invalid curseforge pack id! " + PackVersionUtilsConfig.COMMON.packID);
                }
            } catch (Exception e) {
                LOGGER.error("FAILED TO GET PROJECT INFO");
                e.printStackTrace();
            }
        }
    }
}
