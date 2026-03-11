package com.kvit.items;

import com.kvit.SimpleChunkLoader;
import com.kvit.loader.ChunkLoaderManager;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;

public final class ChunkLoaderBlockItem extends PolymerBlockItem {
    public ChunkLoaderBlockItem(Block block, Item.Properties properties, Item polymerItem, boolean useModel) {
        super(block, properties, polymerItem, useModel);
    }

    @Override
    public @NonNull InteractionResult place(BlockPlaceContext context) {
        if (!context.getLevel().isClientSide()) {
            MinecraftServer server = context.getLevel().getServer();
            if (server != null && !ChunkLoaderManager.canPlaceAnother(server)) {
                if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                            Component.literal("Chunk loader limit reached (" + SimpleChunkLoader.getConfig().maxLoaders() + ").")
                                    .withStyle(ChatFormatting.RED),
                            false
                    );
                }
                return InteractionResult.FAIL;
            }
        }

        return super.place(context);
    }
}
