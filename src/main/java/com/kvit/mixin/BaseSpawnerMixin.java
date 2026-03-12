package com.kvit.mixin;

import com.kvit.SimpleChunkLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Allows mob spawner blocks in force-loaded chunks to operate without a nearby player.
 * <p>
 * Vanilla spawners check {@link Level#hasNearbyAlivePlayer} (default 16-block range)
 * every tick and skip spawning if no player is close enough. This mixin redirects that
 * check inside {@link BaseSpawner#isNearPlayer} so that spawners in force-loaded chunks
 * bypass the player proximity requirement when the config option is enabled.
 */
@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {

    @Redirect(
            method = "isNearPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;hasNearbyAlivePlayer(DDDD)Z"
            )
    )
    private boolean bypassPlayerCheckInForcedChunks(Level level, double x, double y, double z, double range) {
        if (SimpleChunkLoader.getConfig().spawnersBypassPlayerCheck()
                && level instanceof ServerLevel serverLevel) {
            ChunkPos chunkPos = new ChunkPos(BlockPos.containing(x, y, z));
            if (serverLevel.getForceLoadedChunks().contains(chunkPos.toLong())) {
                return true;
            }
        }
        return level.hasNearbyAlivePlayer(x, y, z, range);
    }
}
