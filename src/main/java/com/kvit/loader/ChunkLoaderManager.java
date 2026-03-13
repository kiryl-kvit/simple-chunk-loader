package com.kvit.loader;

import com.kvit.ModContent;
import com.kvit.SimpleChunkLoader;
import com.kvit.data.ChunkLoaderRecord;
import com.kvit.data.ChunkLoaderSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

        ensureStableIds(level.getServer());
        applyWorld(level, data);
    }

    public static void upsert(ServerLevel level, BlockPos pos, boolean enabled, int expansionLevel, boolean allowNaturalSpawning) {
        ChunkLoaderSavedData data = getData(level);
        int id = data.get(pos)
                .map(ChunkLoaderRecord::id)
                .filter(existingId -> existingId > 0)
                .orElseGet(() -> allocateNextId(level.getServer()));
        if (data.putIfChanged(pos, id, enabled, expansionLevel, allowNaturalSpawning)) {
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

    /**
     * Returns {@code true} if the given chunk belongs to an enabled loader that has
     * natural mob spawning enabled. Used by {@code ChunkMapMixin} to bypass the
     * player proximity check for natural spawning.
     */
    public static boolean isNaturalSpawningChunk(ServerLevel level, ChunkPos chunkPos) {
        return getData(level).getSpawningChunks().contains(chunkPos.toLong());
    }

    public static List<LoaderReference> getAllLoaders(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        ensureStableIds(server);

        List<LoaderReference> loaders = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (ChunkLoaderRecord record : getData(level).getLoaders()) {
                loaders.add(new LoaderReference(level, record));
            }
        }

        loaders.sort(Comparator
                .comparingInt((LoaderReference loader) -> loader.record().id())
                .thenComparing(loader -> loader.level().dimension().toString())
                .thenComparingInt(loader -> loader.record().x())
                .thenComparingInt(loader -> loader.record().y())
                .thenComparingInt(loader -> loader.record().z()));
        return loaders;
    }

    public static Optional<LoaderReference> getLoader(MinecraftServer server, int id) {
        if (id <= 0) {
            return Optional.empty();
        }

        for (LoaderReference loader : getAllLoaders(server)) {
            if (loader.record().id() == id) {
                return Optional.of(loader);
            }
        }

        return Optional.empty();
    }

    public static void ensureStableIds(MinecraftServer server) {
        Objects.requireNonNull(server, "server");

        List<LoaderReference> loaders = new ArrayList<>();
        int maxId = 0;

        for (ServerLevel level : server.getAllLevels()) {
            for (ChunkLoaderRecord record : getData(level).getLoaders()) {
                loaders.add(new LoaderReference(level, record));
                if (record.id() > 0) {
                    maxId = Math.max(maxId, record.id());
                }
            }
        }

        loaders.sort(Comparator
                .comparingInt((LoaderReference loader) -> loader.record().id() > 0 ? loader.record().id() : Integer.MAX_VALUE)
                .thenComparing(loader -> loader.level().dimension().toString())
                .thenComparingInt(loader -> loader.record().x())
                .thenComparingInt(loader -> loader.record().y())
                .thenComparingInt(loader -> loader.record().z()));

        Set<Integer> usedIds = new HashSet<>();
        int nextId = maxId + 1;

        for (LoaderReference loader : loaders) {
            ChunkLoaderRecord record = loader.record();
            if (record.id() > 0 && usedIds.add(record.id())) {
                continue;
            }

            while (usedIds.contains(nextId)) {
                nextId++;
            }

            getData(loader.level()).put(record.blockPos(), nextId, record.enabled(), record.expansionLevel(), record.allowNaturalSpawning());
            usedIds.add(nextId);
            nextId++;
        }
    }

    private static int allocateNextId(MinecraftServer server) {
        int maxId = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (ChunkLoaderRecord record : getData(level).getLoaders()) {
                if (record.id() > 0) {
                    maxId = Math.max(maxId, record.id());
                }
            }
        }
        return maxId + 1;
    }

    private static void applyWorld(ServerLevel level, ChunkLoaderSavedData data) {
        Set<Long> desired = new HashSet<>();
        Set<Long> spawning = new HashSet<>();
        for (ChunkLoaderRecord record : data.getLoaders()) {
            if (!record.enabled()) {
                continue;
            }

            ChunkBounds bounds = getChunkBounds(record.blockPos(), record.expansionLevel());
            for (int chunkX = bounds.minChunkX(); chunkX <= bounds.maxChunkX(); chunkX++) {
                for (int chunkZ = bounds.minChunkZ(); chunkZ <= bounds.maxChunkZ(); chunkZ++) {
                    long packed = ChunkPos.asLong(chunkX, chunkZ);
                    desired.add(packed);
                    if (record.allowNaturalSpawning()) {
                        spawning.add(packed);
                    }
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
        data.setSpawningChunks(spawning);
    }

    private static ChunkLoaderSavedData getData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ChunkLoaderSavedData.TYPE);
    }

    public record LoaderReference(ServerLevel level, ChunkLoaderRecord record) {
        public BlockPos blockPos() {
            return this.record.blockPos();
        }

        public Identifier dimensionId() {
            return this.level.dimension().identifier();
        }
    }
}
