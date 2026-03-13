package com.kvit.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record ChunkLoaderRecord(int id, int x, int y, int z, boolean enabled, int expansionLevel, boolean allowNaturalSpawning,
                                String name) {
    private static final String FIELD_ID = "id";
    private static final String FIELD_X = "x";
    private static final String FIELD_Y = "y";
    private static final String FIELD_Z = "z";
    private static final String FIELD_ENABLED = "enabled";
    private static final String FIELD_EXPANSION_LEVEL = "expansionLevel";
    private static final String FIELD_ALLOW_NATURAL_SPAWNING = "allowNaturalSpawning";
    private static final String FIELD_NAME = "name";

    public static final Codec<ChunkLoaderRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf(FIELD_ID, 0).forGetter(ChunkLoaderRecord::id),
            Codec.INT.fieldOf(FIELD_X).forGetter(ChunkLoaderRecord::x),
            Codec.INT.fieldOf(FIELD_Y).forGetter(ChunkLoaderRecord::y),
            Codec.INT.fieldOf(FIELD_Z).forGetter(ChunkLoaderRecord::z),
            Codec.BOOL.fieldOf(FIELD_ENABLED).forGetter(ChunkLoaderRecord::enabled),
            Codec.INT.optionalFieldOf(FIELD_EXPANSION_LEVEL, 0).forGetter(ChunkLoaderRecord::expansionLevel),
            Codec.BOOL.optionalFieldOf(FIELD_ALLOW_NATURAL_SPAWNING, false).forGetter(ChunkLoaderRecord::allowNaturalSpawning),
            Codec.STRING.optionalFieldOf(FIELD_NAME, "").forGetter(ChunkLoaderRecord::name)
    ).apply(instance, ChunkLoaderRecord::new));

    public ChunkLoaderRecord {
        name = name == null ? "" : name;
    }

    public static long key(BlockPos pos) {
        return pos.asLong();
    }

    public BlockPos blockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    public boolean hasName() {
        return !this.name.isBlank();
    }

    public ChunkLoaderRecord withId(int id) {
        return new ChunkLoaderRecord(id, this.x, this.y, this.z, this.enabled, this.expansionLevel, this.allowNaturalSpawning, this.name);
    }

    public ChunkLoaderRecord withEnabled(boolean enabled) {
        return new ChunkLoaderRecord(this.id, this.x, this.y, this.z, enabled, this.expansionLevel, this.allowNaturalSpawning, this.name);
    }

    public ChunkLoaderRecord withExpansionLevel(int expansionLevel) {
        return new ChunkLoaderRecord(this.id, this.x, this.y, this.z, this.enabled, expansionLevel, this.allowNaturalSpawning, this.name);
    }

    public ChunkLoaderRecord withAllowNaturalSpawning(boolean allowNaturalSpawning) {
        return new ChunkLoaderRecord(this.id, this.x, this.y, this.z, this.enabled, this.expansionLevel, allowNaturalSpawning, this.name);
    }

    public ChunkLoaderRecord withName(String name) {
        return new ChunkLoaderRecord(this.id, this.x, this.y, this.z, this.enabled, this.expansionLevel, this.allowNaturalSpawning, name);
    }
}
