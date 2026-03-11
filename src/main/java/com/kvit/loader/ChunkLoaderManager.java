package com.kvit.loader;

import com.kvit.ModContent;
import com.kvit.SimpleChunkLoader;
import com.kvit.data.ChunkLoaderRecord;
import com.kvit.data.ChunkLoaderSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ChunkLoaderManager {
    static final int CHUNK_SIZE = 16;

    private ChunkLoaderManager() {
    }

    public static boolean canPlaceAnother(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return getTotalLoaderCount(server) < SimpleChunkLoader.getConfig().maxLoaders();
    }

    private static int getTotalLoaderCount(MinecraftServer server) {
        int total = 0;
        for (ServerLevel level : server.getAllLevels()) {
            total += getData(level).loaderCount();
        }
        return total;
    }

    public static void handleWorldLoad(ServerLevel level) {
        ChunkLoaderSavedData data = getData(level);

        // Only validate blocks in chunks that are already in memory.
        // Unloaded chunks cannot be checked yet — their loaders will be
        // validated via BLOCK_ENTITY_LOAD when the chunks eventually load.
        List<BlockPos> toRemove = new ArrayList<>();
        for (ChunkLoaderRecord record : data.getLoaders()) {
            BlockPos pos = record.blockPos();
            if (level.isLoaded(pos) && !level.getBlockState(pos).is(ModContent.chunkLoader())) {
                toRemove.add(pos);
            }
        }
        for (BlockPos pos : toRemove) {
            data.remove(pos);
        }

        applyWorld(level, data);
    }

    public static void upsert(ServerLevel level, BlockPos pos, boolean enabled, int expansionLevel) {
        ChunkLoaderSavedData data = getData(level);
        if (data.putIfChanged(pos, enabled, expansionLevel)) {
            applyWorld(level, data);
        }
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        ChunkLoaderSavedData data = getData(level);
        data.remove(pos);
        applyWorld(level, data);
    }

    public static ChunkBounds getChunkBounds(BlockPos pos, int expansionLevel) {
        int chunkX = Math.floorDiv(pos.getX(), CHUNK_SIZE);
        int chunkZ = Math.floorDiv(pos.getZ(), CHUNK_SIZE);
        return new ChunkBounds(
                chunkX - expansionLevel, chunkX + expansionLevel,
                chunkZ - expansionLevel, chunkZ + expansionLevel
        );
    }

    private static void applyWorld(ServerLevel level, ChunkLoaderSavedData data) {
        Set<Long> desired = new HashSet<>();
        for (ChunkLoaderRecord record : data.getLoaders()) {
            if (!record.enabled()) {
                continue;
            }

            ChunkBounds bounds = getChunkBounds(record.blockPos(), record.expansionLevel());
            for (int chunkX = bounds.minChunkX(); chunkX <= bounds.maxChunkX(); chunkX++) {
                for (int chunkZ = bounds.minChunkZ(); chunkZ <= bounds.maxChunkZ(); chunkZ++) {
                    desired.add(ChunkPos.asLong(chunkX, chunkZ));
                }
            }
        }

        Set<Long> previous = data.getManagedChunks();
        for (long packed : previous) {
            if (!desired.contains(packed)) {
                level.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), false);
            }
        }

        for (long packed : desired) {
            if (!previous.contains(packed)) {
                level.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), true);
            }
        }

        data.setManagedChunks(desired);
    }

    private static ChunkLoaderSavedData getData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ChunkLoaderSavedData.TYPE);
    }
}
