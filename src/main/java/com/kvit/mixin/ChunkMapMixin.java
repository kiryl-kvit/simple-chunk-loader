package com.kvit.mixin;

import com.kvit.loader.ChunkLoaderManager;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows natural mob spawning in force-loaded chunks that have the
 * "allow natural spawning" option enabled, even when no player is nearby.
 * <p>
 * Vanilla gates natural spawning in {@code ServerChunkCache.tickChunks()} by calling
 * {@code ChunkMap.anyPlayerCloseEnoughForSpawning(ChunkPos)}, which returns {@code false}
 * when no player is within mob-spawn distance. This mixin injects at the head of that
 * method and short-circuits to {@code true} for chunks that belong to a loader with
 * natural spawning enabled.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Shadow
    @Final
    ServerLevel level;

    @Inject(method = "anyPlayerCloseEnoughForSpawning", at = @At("HEAD"), cancellable = true)
    private void allowSpawningInForcedChunks(ChunkPos chunkPos, CallbackInfoReturnable<Boolean> cir) {
        if (ChunkLoaderManager.isNaturalSpawningChunk(this.level, chunkPos)) {
            cir.setReturnValue(true);
        }
    }
}
