package com.kvit.blocks.chunkLoader.entity;

import com.kvit.ModContent;
import com.kvit.loader.ChunkLoaderManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

public final class ChunkLoaderBlockEntity extends BlockEntity {
    private boolean enabled = true;

    public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.chunkLoaderBlockEntity(), pos, state);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        this.setChanged();
        if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ChunkLoaderManager.upsert(serverLevel, this.getBlockPos(), this.enabled);
        }
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        this.enabled = input.getBooleanOr("Enabled", true);
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Enabled", this.enabled);
    }

    @Override
    public void preRemoveSideEffects(@NonNull BlockPos pos, @NonNull BlockState state) {
        if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            ChunkLoaderManager.remove(serverLevel, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }
}
