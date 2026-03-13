package com.kvit.blocks.chunkLoader.entity;

import com.kvit.ModContent;
import com.kvit.SimpleChunkLoader;
import com.kvit.loader.ChunkLoaderManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

public final class ChunkLoaderBlockEntity extends BlockEntity {
    private static final String NBT_ENABLED = "Enabled";
    private static final String NBT_EXPANSION_LEVEL = "ExpansionLevel";
    private static final String NBT_ALLOW_NATURAL_SPAWNING = "AllowNaturalSpawning";

    private boolean enabled = true;
    private int expansionLevel = 0;
    private boolean allowNaturalSpawning = false;

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.chunkLoaderBlockEntity(), pos, state);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.applyEnabled(enabled)) {
            this.syncToManager();
        }
    }

    public int getExpansionLevel() {
        return this.expansionLevel;
    }

    public void setExpansionLevel(int expansionLevel) {
        int clamped = Math.clamp(expansionLevel, 0, SimpleChunkLoader.getConfig().maxExpansionLevel());
        if (this.expansionLevel == clamped) {
            return;
        }

        this.expansionLevel = clamped;
        this.setChanged();
        this.syncToManager();
    }

    public boolean isAllowNaturalSpawning() {
        return this.allowNaturalSpawning;
    }

    public void setAllowNaturalSpawning(boolean allowNaturalSpawning) {
        if (this.applyAllowNaturalSpawning(allowNaturalSpawning)) {
            this.syncToManager();
        }
    }

    public boolean setEnabledSilently(boolean enabled) {
        return this.applyEnabled(enabled);
    }

    public boolean setAllowNaturalSpawningSilently(boolean allowNaturalSpawning) {
        return this.applyAllowNaturalSpawning(allowNaturalSpawning);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        this.enabled = input.getBooleanOr(NBT_ENABLED, true);
        this.expansionLevel = input.getIntOr(NBT_EXPANSION_LEVEL, 0);
        this.allowNaturalSpawning = input.getBooleanOr(NBT_ALLOW_NATURAL_SPAWNING, false);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean(NBT_ENABLED, this.enabled);
        output.putInt(NBT_EXPANSION_LEVEL, this.expansionLevel);
        output.putBoolean(NBT_ALLOW_NATURAL_SPAWNING, this.allowNaturalSpawning);
    }

    @Override
    public void preRemoveSideEffects(@NonNull BlockPos pos, @NonNull BlockState state) {
        if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ChunkLoaderManager.remove(serverLevel, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    private boolean applyEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return false;
        }

        this.enabled = enabled;
        this.setChanged();
        return true;
    }

    private boolean applyAllowNaturalSpawning(boolean allowNaturalSpawning) {
        if (this.allowNaturalSpawning == allowNaturalSpawning) {
            return false;
        }

        this.allowNaturalSpawning = allowNaturalSpawning;
        this.setChanged();
        return true;
    }

    private void syncToManager() {
        if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ChunkLoaderManager.upsert(serverLevel, this.getBlockPos(), this.enabled, this.expansionLevel, this.allowNaturalSpawning);
        }
    }
}
