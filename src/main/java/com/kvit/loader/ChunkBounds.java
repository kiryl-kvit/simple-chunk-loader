package com.kvit.loader;

public record ChunkBounds(int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
    public int minBlockX() {
        return this.minChunkX * 16;
    }

    public int maxBlockXInclusive() {
        return ((this.maxChunkX + 1) * 16) - 1;
    }

    public int minBlockZ() {
        return this.minChunkZ * 16;
    }

    public int maxBlockZInclusive() {
        return ((this.maxChunkZ + 1) * 16) - 1;
    }

    public int loadedChunkCount() {
        return (this.maxChunkX - this.minChunkX + 1) * (this.maxChunkZ - this.minChunkZ + 1);
    }
}
