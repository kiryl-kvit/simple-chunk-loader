package com.kvit.config;

import com.kvit.SimpleChunkLoader;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record SimpleChunkLoaderConfig(int maxLoaders, int previewRefreshTicks, int previewParticleStep) {
    private static final int DEFAULT_MAX_LOADERS = 16;
    private static final int DEFAULT_PREVIEW_REFRESH_TICKS = 10;
    private static final int DEFAULT_PREVIEW_PARTICLE_STEP = 1;

    public static SimpleChunkLoaderConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve(SimpleChunkLoader.MOD_ID + ".toml");

        try {
            Files.createDirectories(configDir);
            if (Files.notExists(configPath)) {
                Files.writeString(configPath, defaultConfigContents(), StandardCharsets.UTF_8);
            }

            int maxLoaders = DEFAULT_MAX_LOADERS;
            int previewRefreshTicks = DEFAULT_PREVIEW_REFRESH_TICKS;
            int previewParticleStep = DEFAULT_PREVIEW_PARTICLE_STEP;

            List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = stripComment(rawLine).trim();
                if (line.isEmpty()) {
                    continue;
                }

                int separator = line.indexOf('=');
                if (separator < 0) {
                    continue;
                }

                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                try {
                    switch (key) {
                        case "max_loaders" -> maxLoaders = Math.max(0, Integer.parseInt(value));
                        case "preview_refresh_ticks" -> previewRefreshTicks = Math.max(2, Integer.parseInt(value));
                        case "preview_particle_step" -> previewParticleStep = Math.max(1, Integer.parseInt(value));
                        default -> {
                        }
                    }
                } catch (NumberFormatException exception) {
                    SimpleChunkLoader.LOGGER.warn("Ignoring invalid config value '{}' for key '{}'", value, key);
                }
            }

            return new SimpleChunkLoaderConfig(maxLoaders, previewRefreshTicks, previewParticleStep);
        } catch (IOException exception) {
            SimpleChunkLoader.LOGGER.error("Failed to load config, using defaults", exception);
            return new SimpleChunkLoaderConfig(
                    DEFAULT_MAX_LOADERS,
                    DEFAULT_PREVIEW_REFRESH_TICKS,
                    DEFAULT_PREVIEW_PARTICLE_STEP
            );
        }
    }

    private static String stripComment(String line) {
        int commentIndex = line.indexOf('#');
        return commentIndex >= 0 ? line.substring(0, commentIndex) : line;
    }

    private static String defaultConfigContents() {
        return "# Maximum number of chunk loader blocks that may exist across all dimensions.\n"
                + "max_loaders = " + DEFAULT_MAX_LOADERS + "\n\n"
                + "# Preview particle refresh interval in ticks.\n"
                + "preview_refresh_ticks = " + DEFAULT_PREVIEW_REFRESH_TICKS + "\n\n"
                + "# Spacing between preview particles in blocks.\n"
                + "preview_particle_step = " + DEFAULT_PREVIEW_PARTICLE_STEP + "\n";
    }
}
