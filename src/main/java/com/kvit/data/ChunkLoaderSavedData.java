package com.kvit.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public final class ChunkLoaderSavedData extends SavedData {
    private static final String FIELD_LOADERS = "loaders";
    private static final String FIELD_MANAGED_CHUNKS = "managed_chunks";
    private static final String FIELD_SPAWNING_CHUNKS = "spawning_chunks";
    private static final String DATA_NAME = "simple_chunk_loader";

    private static final Codec<ChunkLoaderSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, ChunkLoaderRecord.CODEC)
                    .optionalFieldOf(FIELD_LOADERS, Map.of())
                    .forGetter(data -> {
                        Map<String, ChunkLoaderRecord> out = new HashMap<>(data.loaders.size() * 2);
                        data.loaders.forEach((k, v) -> out.put(Long.toString(k), v));
                        return out;
                    }),
            Codec.LONG.listOf()
                    .optionalFieldOf(FIELD_MANAGED_CHUNKS, List.of())
                    .forGetter(data -> List.copyOf(data.managedChunks)),
            Codec.LONG.listOf()
                    .optionalFieldOf(FIELD_SPAWNING_CHUNKS, List.of())
                    .forGetter(data -> List.copyOf(data.spawningChunks))
    ).apply(instance, (stringLoaders, managedChunks, spawningChunks) -> {
        Map<Long, ChunkLoaderRecord> loaders = new HashMap<>();
        stringLoaders.values().forEach(record -> loaders.put(ChunkLoaderRecord.key(record.blockPos()), record));
        return new ChunkLoaderSavedData(loaders, new HashSet<>(managedChunks), new HashSet<>(spawningChunks));
    }));

    public static final SavedDataType<ChunkLoaderSavedData> TYPE = new SavedDataType<>(
            DATA_NAME,
            ChunkLoaderSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<Long, ChunkLoaderRecord> loaders;
    private final Set<Long> managedChunks;
    private final Set<Long> spawningChunks;

    public ChunkLoaderSavedData() {
        this(new HashMap<>(), new HashSet<>(), new HashSet<>());
    }

    private ChunkLoaderSavedData(Map<Long, ChunkLoaderRecord> loaders, Set<Long> managedChunks, Set<Long> spawningChunks) {
        this.loaders = new HashMap<>(loaders);
        this.managedChunks = new HashSet<>(managedChunks);
        this.spawningChunks = new HashSet<>(spawningChunks);
    }

    public Collection<ChunkLoaderRecord> getLoaders() {
        return Collections.unmodifiableCollection(this.loaders.values());
    }

    public int loaderCount() {
        return this.loaders.size();
    }

    public Optional<ChunkLoaderRecord> get(BlockPos pos) {
        long key = ChunkLoaderRecord.key(pos);
        return Optional.ofNullable(this.loaders.get(key));
    }

    public boolean putIfChanged(ChunkLoaderRecord record) {
        return this.putIfChanged(record.blockPos(), record);
    }

    public boolean putIfChanged(BlockPos pos, ChunkLoaderRecord record) {
        long key = ChunkLoaderRecord.key(pos);
        ChunkLoaderRecord previous = this.loaders.get(key);
        if (record.equals(previous)) {
            return false;
        }
        this.loaders.put(key, record);
        this.setDirty();
        return true;
    }

    public void put(BlockPos pos, int id, boolean enabled, int expansionLevel, boolean allowNaturalSpawning, String name) {
        this.putIfChanged(pos, id, enabled, expansionLevel, allowNaturalSpawning, name);
    }

    public boolean putIfChanged(BlockPos pos, int id, boolean enabled, int expansionLevel, boolean allowNaturalSpawning, String name) {
        return this.putIfChanged(pos, recordAt(pos, id, enabled, expansionLevel, allowNaturalSpawning, name));
    }

    public void remove(BlockPos pos) {
        if (this.loaders.remove(ChunkLoaderRecord.key(pos)) != null) {
            this.setDirty();
        }
    }

    public Set<Long> getManagedChunks() {
        return Collections.unmodifiableSet(this.managedChunks);
    }

    public Optional<ChunkLoaderRecord> getById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }

        for (ChunkLoaderRecord record : this.loaders.values()) {
            if (record.id() == id) {
                return Optional.of(record);
            }
        }

        return Optional.empty();
    }

    public void setManagedChunks(Set<Long> managedChunks) {
        this.replaceSet(this.managedChunks, managedChunks);
    }

    public Set<Long> getSpawningChunks() {
        return Collections.unmodifiableSet(this.spawningChunks);
    }

    public void setSpawningChunks(Set<Long> spawningChunks) {
        this.replaceSet(this.spawningChunks, spawningChunks);
    }

    private void replaceSet(Set<Long> target, Set<Long> incoming) {
        if (target.equals(incoming)) {
            return;
        }

        target.clear();
        target.addAll(incoming);
        this.setDirty();
    }

    private static ChunkLoaderRecord recordAt(BlockPos pos, int id, boolean enabled, int expansionLevel,
                                              boolean allowNaturalSpawning, String name) {
        return new ChunkLoaderRecord(id, pos.getX(), pos.getY(), pos.getZ(), enabled, expansionLevel, allowNaturalSpawning, name);
    }
}
