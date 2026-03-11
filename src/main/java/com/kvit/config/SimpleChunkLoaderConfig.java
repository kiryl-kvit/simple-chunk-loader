package com.kvit.config;

import com.kvit.SimpleChunkLoader;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public record SimpleChunkLoaderConfig(int maxLoaders, int previewRefreshTicks, int previewParticleStep, int maxExpansionLevel) {
    private static final String KEY_MAX_LOADERS = "max_loaders";
    private static final String KEY_PREVIEW_REFRESH_TICKS = "preview_refresh_ticks";
    private static final String KEY_PREVIEW_PARTICLE_STEP = "preview_particle_step";
    private static final String KEY_MAX_EXPANSION_LEVEL = "max_expansion_level";

    private static final int DEFAULT_MAX_LOADERS = 16;
    private static final int DEFAULT_PREVIEW_REFRESH_TICKS = 10;
    private static final int DEFAULT_PREVIEW_PARTICLE_STEP = 1;
    private static final int DEFAULT_MAX_EXPANSION_LEVEL = 3;

    public static SimpleChunkLoaderConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDir.resolve(SimpleChunkLoader.MOD_ID + ".toml");

        try {
            Files.createDirectories(configDir);
            if (Files.notExists(configPath)) {
                Files.writeString(configPath, defaultConfigContents(), StandardCharsets.UTF_8);
            }

            int[] maxLoaders = {DEFAULT_MAX_LOADERS};
			int[] previewRefreshTicks = {DEFAULT_PREVIEW_REFRESH_TICKS};
			int[] previewParticleStep = {DEFAULT_PREVIEW_PARTICLE_STEP};
			int[] maxExpansionLevel = {DEFAULT_MAX_EXPANSION_LEVEL};

            try (Stream<String> lines = Files.lines(configPath, StandardCharsets.UTF_8)) {
                lines.forEach(rawLine -> {
                    String line = stripComment(rawLine).strip();
                    if (line.isEmpty()) {
                        return;
                    }

                    int separator = line.indexOf('=');
                    if (separator < 0) {
                        return;
                    }

                    String key = line.substring(0, separator).strip();
                    String value = line.substring(separator + 1).strip();
                    try {
                        switch (key) {
                            case KEY_MAX_LOADERS -> maxLoaders[0] = Math.max(0, Integer.parseInt(value));
                            case KEY_PREVIEW_REFRESH_TICKS -> previewRefreshTicks[0] = Math.max(2, Integer.parseInt(value));
                            case KEY_PREVIEW_PARTICLE_STEP -> previewParticleStep[0] = Math.max(1, Integer.parseInt(value));
                            case KEY_MAX_EXPANSION_LEVEL -> maxExpansionLevel[0] = Math.max(0, Integer.parseInt(value));
                            default -> { }
                        }
                    } catch (NumberFormatException exception) {
                        SimpleChunkLoader.LOGGER.warn("Ignoring invalid config value '{}' for key '{}'", value, key);
                    }
                });
            }

            return new SimpleChunkLoaderConfig(maxLoaders[0], previewRefreshTicks[0], previewParticleStep[0], maxExpansionLevel[0]);
        } catch (IOException exception) {
            SimpleChunkLoader.LOGGER.error("Failed to load config, using defaults", exception);
            return new SimpleChunkLoaderConfig(
                    DEFAULT_MAX_LOADERS,
                    DEFAULT_PREVIEW_REFRESH_TICKS,
                    DEFAULT_PREVIEW_PARTICLE_STEP,
                    DEFAULT_MAX_EXPANSION_LEVEL
            );
        }
    }

    /**
     * Strips a TOML-style comment from a line.
     * <p>
     * Note: this is a simplified parser that does not handle '#' characters
     * inside quoted string values. This is acceptable because all config
     * values in this file are numeric.
     */
    private static String stripComment(String line) {
        int commentIndex = line.indexOf('#');
        return commentIndex >= 0 ? line.substring(0, commentIndex) : line;
    }

    private static String defaultConfigContents() {
        return """
                # Maximum number of chunk loader blocks that may exist across all dimensions.
                %s = %d
                
                # Preview particle refresh interval in ticks.
                %s = %d
                
                # Spacing between preview particles in blocks.
                %s = %d
                
                # Maximum expansion level for chunk loaders (0 = 1x1, 1 = 3x3, 2 = 5x5, etc.).
                %s = %d
                """.formatted(
                KEY_MAX_LOADERS, DEFAULT_MAX_LOADERS,
                KEY_PREVIEW_REFRESH_TICKS, DEFAULT_PREVIEW_REFRESH_TICKS,
                KEY_PREVIEW_PARTICLE_STEP, DEFAULT_PREVIEW_PARTICLE_STEP,
                KEY_MAX_EXPANSION_LEVEL, DEFAULT_MAX_EXPANSION_LEVEL
        );
    }
}
