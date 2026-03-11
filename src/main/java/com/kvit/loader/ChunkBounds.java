package com.kvit.loader;

public record ChunkBounds(int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {
    public ChunkBounds {
        if (minChunkX > maxChunkX) {
            throw new IllegalArgumentException("minChunkX (%d) > maxChunkX (%d)".formatted(minChunkX, maxChunkX));
        }
        if (minChunkZ > maxChunkZ) {
            throw new IllegalArgumentException("minChunkZ (%d) > maxChunkZ (%d)".formatted(minChunkZ, maxChunkZ));
        }
    }

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
