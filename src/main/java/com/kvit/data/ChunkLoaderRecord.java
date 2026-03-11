package com.kvit.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record ChunkLoaderRecord(int x, int y, int z, boolean enabled) {
    public static final Codec<ChunkLoaderRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(ChunkLoaderRecord::x),
            Codec.INT.fieldOf("y").forGetter(ChunkLoaderRecord::y),
            Codec.INT.fieldOf("z").forGetter(ChunkLoaderRecord::z),
            Codec.BOOL.fieldOf("enabled").forGetter(ChunkLoaderRecord::enabled)
    ).apply(instance, ChunkLoaderRecord::new));

    public static long key(BlockPos pos) {
        return pos.asLong();
    }

    public BlockPos blockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }
}
