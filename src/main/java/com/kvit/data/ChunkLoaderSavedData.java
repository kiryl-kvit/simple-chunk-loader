package com.kvit.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public final class ChunkLoaderSavedData extends SavedData {
    private static final Codec<ChunkLoaderSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, ChunkLoaderRecord.CODEC)
                    .optionalFieldOf("loaders", Map.of())
                    .forGetter(data -> data.loaders),
            Codec.LONG.listOf()
                    .optionalFieldOf("managed_chunks", List.of())
                    .forGetter(data -> List.copyOf(data.managedChunks))
    ).apply(instance, (loaders, managedChunks) -> new ChunkLoaderSavedData(loaders, new HashSet<>(managedChunks))));

    public static final SavedDataType<ChunkLoaderSavedData> TYPE = new SavedDataType<>(
            "simple_chunk_loader",
            ChunkLoaderSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<String, ChunkLoaderRecord> loaders;
    private final Set<Long> managedChunks;

    public ChunkLoaderSavedData() {
        this(new HashMap<>(), new HashSet<>());
    }

    private ChunkLoaderSavedData(Map<String, ChunkLoaderRecord> loaders, Set<Long> managedChunks) {
        this.loaders = new HashMap<>(loaders);
        this.managedChunks = new HashSet<>(managedChunks);
    }

    public Collection<ChunkLoaderRecord> getLoaders() {
        return Collections.unmodifiableCollection(this.loaders.values());
    }

    public int loaderCount() {
        return this.loaders.size();
    }

    public void put(BlockPos pos, boolean enabled) {
        String key = ChunkLoaderRecord.key(pos);
        ChunkLoaderRecord next = new ChunkLoaderRecord(pos.getX(), pos.getY(), pos.getZ(), enabled);
        ChunkLoaderRecord previous = this.loaders.put(key, next);
        if (!next.equals(previous)) {
            this.setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (this.loaders.remove(ChunkLoaderRecord.key(pos)) != null) {
            this.setDirty();
        }
    }

    public Set<Long> getManagedChunks() {
        return Collections.unmodifiableSet(this.managedChunks);
    }

    public void setManagedChunks(Set<Long> managedChunks) {
        if (!this.managedChunks.equals(managedChunks)) {
            this.managedChunks.clear();
            this.managedChunks.addAll(managedChunks);
            this.setDirty();
        }
    }
}
