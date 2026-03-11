package com.kvit.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record ChunkLoaderRecord(int x, int y, int z, boolean enabled, int expansionLevel) {
    private static final String FIELD_X = "x";
    private static final String FIELD_Y = "y";
    private static final String FIELD_Z = "z";
    private static final String FIELD_ENABLED = "enabled";
    private static final String FIELD_EXPANSION_LEVEL = "expansionLevel";

    public static final Codec<ChunkLoaderRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf(FIELD_X).forGetter(ChunkLoaderRecord::x),
            Codec.INT.fieldOf(FIELD_Y).forGetter(ChunkLoaderRecord::y),
            Codec.INT.fieldOf(FIELD_Z).forGetter(ChunkLoaderRecord::z),
            Codec.BOOL.fieldOf(FIELD_ENABLED).forGetter(ChunkLoaderRecord::enabled),
            Codec.INT.optionalFieldOf(FIELD_EXPANSION_LEVEL, 0).forGetter(ChunkLoaderRecord::expansionLevel)
    ).apply(instance, ChunkLoaderRecord::new));

    public static long key(BlockPos pos) {
        return pos.asLong();
    }

    public BlockPos blockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }
}
