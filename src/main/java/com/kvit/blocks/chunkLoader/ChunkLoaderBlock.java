package com.kvit.blocks.chunkLoader;

import com.kvit.ModContent;
import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
import com.kvit.menu.ChunkLoaderMenu;
import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;
import xyz.nucleoid.packettweaker.PacketContext;

public final class ChunkLoaderBlock extends BaseEntityBlock implements PolymerBlock {
    public static final MapCodec<ChunkLoaderBlock> CODEC = simpleCodec(ChunkLoaderBlock::new);

    public ChunkLoaderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected @NonNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NonNull RenderShape getRenderShape(@NonNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.LODESTONE.defaultBlockState();
    }

    @Override
    public @NonNull BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new ChunkLoaderBlockEntity(pos, state);
    }

    @Override
    protected @NonNull InteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state,
                                                   @NonNull Level level, @NonNull BlockPos pos, @NonNull Player player,
                                                   @NonNull InteractionHand hand, @NonNull BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(@NonNull BlockState state, Level level, @NonNull BlockPos pos,
                                                        @NonNull Player player, @NonNull BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ChunkLoaderBlockEntity)) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (int syncId, net.minecraft.world.entity.player.Inventory inventory, Player openPlayer) ->
                            createMenu(syncId, inventory, serverLevel, pos, openPlayer),
                    net.minecraft.network.chat.Component.literal("Chunk Loader")
            ));
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.PASS;
    }

    private AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory inventory,
                                             ServerLevel level, BlockPos pos, Player player) {
        return new ChunkLoaderMenu(syncId, inventory, level, pos, player.getUUID());
    }

    @Override
    protected boolean shouldChangedStateKeepBlockEntity(BlockState newState) {
        return newState.is(ModContent.CHUNK_LOADER);
    }
}
