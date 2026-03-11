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
        return this.minChunkX * ChunkLoaderManager.CHUNK_SIZE;
    }

    public int maxBlockXExclusive() {
        return (this.maxChunkX + 1) * ChunkLoaderManager.CHUNK_SIZE;
    }

    public int minBlockZ() {
        return this.minChunkZ * ChunkLoaderManager.CHUNK_SIZE;
    }

    public int maxBlockZExclusive() {
        return (this.maxChunkZ + 1) * ChunkLoaderManager.CHUNK_SIZE;
    }
}
