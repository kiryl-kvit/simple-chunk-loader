package com.kvit.loader;

import com.kvit.ModContent;
import com.kvit.SimpleChunkLoader;
import com.kvit.data.ChunkLoaderRecord;
import com.kvit.data.ChunkLoaderSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class ChunkLoaderManager {
    private ChunkLoaderManager() {
    }

    public static boolean canPlaceAnother(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return getTotalLoaderCount(server) < SimpleChunkLoader.getConfig().maxLoaders();
    }

    public static int getTotalLoaderCount(MinecraftServer server) {
        int total = 0;
        for (ServerLevel level : server.getAllLevels()) {
            total += getData(level).loaderCount();
        }
        return total;
    }

    public static void handleWorldLoad(ServerLevel level) {
        ChunkLoaderSavedData data = getData(level);
        for (ChunkLoaderRecord record : Set.copyOf(data.getLoaders())) {
            BlockPos pos = record.blockPos();
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(ModContent.chunkLoader())) {
                data.remove(pos);
            }
        }
        applyWorld(level);
    }

    public static void upsert(ServerLevel level, BlockPos pos, boolean enabled) {
        ChunkLoaderSavedData data = getData(level);
        data.put(pos, enabled);
        applyWorld(level);
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        ChunkLoaderSavedData data = getData(level);
        data.remove(pos);
        applyWorld(level);
    }

    public static ChunkBounds getChunkBounds(BlockPos pos) {
        int chunkX = Math.floorDiv(pos.getX(), 16);
        int chunkZ = Math.floorDiv(pos.getZ(), 16);
        return new ChunkBounds(chunkX, chunkX, chunkZ, chunkZ);
    }

    private static void applyWorld(ServerLevel level) {
        ChunkLoaderSavedData data = getData(level);
        Set<Long> desired = new HashSet<>();
        for (ChunkLoaderRecord record : data.getLoaders()) {
            if (!record.enabled()) {
                continue;
            }

            ChunkBounds bounds = getChunkBounds(record.blockPos());
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
