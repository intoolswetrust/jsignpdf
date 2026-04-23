/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 *
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 *
 * Contributor(s): Josef Cacek.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
package net.sf.jsignpdf.utils;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;

/**
 * Hands out {@link PropertyProvider} instances backed by the right file under the resolved config directory.
 */
public final class PropertyStoreFactory {

    private static final String PRESET_FILENAME_PREFIX = "preset-";
    private static final String PRESET_FILENAME_SUFFIX = ".properties";

    private static volatile PropertyStoreFactory instance;

    private final ConfigLocationResolver resolver;
    private volatile PropertyProvider mainConfig;

    public PropertyStoreFactory(ConfigLocationResolver resolver) {
        this.resolver = resolver;
    }

    public static PropertyStoreFactory getInstance() {
        PropertyStoreFactory ref = instance;
        if (ref == null) {
            synchronized (PropertyStoreFactory.class) {
                ref = instance;
                if (ref == null) {
                    ref = new PropertyStoreFactory(ConfigLocationResolver.getInstance());
                    instance = ref;
                }
            }
        }
        return ref;
    }

    /**
     * Returns the cached {@link PropertyProvider} backed by the main config file. On first call it loads from disk.
     * Returns an in-memory-only provider if the config directory cannot be resolved. The same instance is handed out to
     * every caller so their views of the main config stay consistent (e.g. signing options and recent-file list share
     * one backing Properties).
     */
    public PropertyProvider mainConfig() {
        PropertyProvider ref = mainConfig;
        if (ref == null) {
            synchronized (this) {
                ref = mainConfig;
                if (ref == null) {
                    Path file = resolver.getMainConfigFile();
                    ref = new PropertyProvider(file);
                    ref.load();
                    mainConfig = ref;
                }
            }
        }
        return ref;
    }

    /**
     * Returns a {@link PropertyProvider} backed by the given preset file (by filename, without directory).
     */
    public PropertyProvider preset(String filename) {
        Path dir = resolver.getPresetsDir();
        if (dir == null) {
            return new PropertyProvider(null);
        }
        PropertyProvider provider = new PropertyProvider(dir.resolve(filename));
        provider.load();
        return provider;
    }

    /**
     * Creates an empty {@link PropertyProvider} backed by a fresh {@code preset-<epochMillis>.properties} file in the presets
     * directory. Resolves filename collisions by appending a counter suffix. The file is not written until
     * {@link PropertyProvider#save()} is called.
     */
    public PropertyProvider newPreset() {
        Path dir = resolver.getPresetsDir();
        if (dir == null) {
            return new PropertyProvider(null);
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create presets directory " + dir, e);
            return new PropertyProvider(null);
        }
        long ts = System.currentTimeMillis();
        Path target = dir.resolve(PRESET_FILENAME_PREFIX + ts + PRESET_FILENAME_SUFFIX);
        int counter = 2;
        while (Files.exists(target)) {
            target = dir.resolve(PRESET_FILENAME_PREFIX + ts + "-" + counter + PRESET_FILENAME_SUFFIX);
            counter++;
        }
        return new PropertyProvider(target);
    }

    public ConfigLocationResolver getResolver() {
        return resolver;
    }

    /**
     * Wipes every file under the resolved config directory (main config file, presets, anything else) and clears the in-memory
     * state of the cached main-config provider so every existing holder sees the reset. The config directory itself is kept
     * so subsequent writes don't race with directory creation.
     */
    public synchronized void resetAll() {
        Path dir = resolver.getConfigDir();
        if (dir != null) {
            deleteDirectoryContents(dir);
        }
        if (mainConfig != null) {
            mainConfig.clear();
        }
    }

    private static void deleteDirectoryContents(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    if (!d.equals(dir)) {
                        Files.deleteIfExists(d);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to wipe config directory " + dir, e);
        }
    }
}
