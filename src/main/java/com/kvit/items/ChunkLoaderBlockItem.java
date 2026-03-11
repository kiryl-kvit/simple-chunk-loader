package com.kvit.items;

import com.kvit.SimpleChunkLoader;
import com.kvit.loader.ChunkLoaderManager;
import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;
import xyz.nucleoid.packettweaker.PacketContext;

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

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context) {
        ItemStack result = super.getPolymerItemStack(itemStack, tooltipType, context);
        result.set(DataComponents.ITEM_NAME, Component.literal("Chunk Loader"));
        return result;
    }
}
