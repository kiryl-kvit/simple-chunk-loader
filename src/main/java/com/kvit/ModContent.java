package com.kvit;

import com.kvit.blocks.chunkLoader.ChunkLoaderBlock;
import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
import com.kvit.items.ChunkLoaderBlockItem;
import com.kvit.network.ChunkLoaderPresencePayload;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import eu.pb4.polymer.networking.api.server.PolymerServerNetworking;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import xyz.nucleoid.packettweaker.PacketContext;

public final class ModContent {
	public static final String CHUNK_LOADER_DISPLAY_NAME = "Chunk Loader";

	private static Block chunkLoader;
	private static Item chunkLoaderItem;
	private static BlockEntityType<ChunkLoaderBlockEntity> chunkLoaderBlockEntity;

	private static final Identifier MOD_PRESENCE_CHANNEL = ChunkLoaderPresencePayload.CHANNEL_ID;

	private ModContent() {
	}

	/**
	 * Returns true if the player connected via this PacketContext has simple-chunk-loader
	 * installed. Returns false for vanilla clients, clients with only unrelated Polymer
	 * mods, and for null-player contexts (registry sync phase).
	 */
	public static boolean isModdedClient(PacketContext context) {
		var player = context.getPlayer();
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		return PolymerServerNetworking.getSupportedVersion(serverPlayer.connection, MOD_PRESENCE_CHANNEL)
			>= ChunkLoaderPresencePayload.PROTOCOL_VERSION;
	}

	public static Block chunkLoader() {
		return chunkLoader;
	}

	public static Item chunkLoaderItem() {
		return chunkLoaderItem;
	}

	public static BlockEntityType<ChunkLoaderBlockEntity> chunkLoaderBlockEntity() {
		return chunkLoaderBlockEntity;
	}

	public static void register() {
		chunkLoader = Registry.register(BuiltInRegistries.BLOCK, SimpleChunkLoader.id("chunk_loader"),
			new ChunkLoaderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LODESTONE)
				.setId(ResourceKey.create(Registries.BLOCK, SimpleChunkLoader.id("chunk_loader")))));

		chunkLoaderItem = Registry.register(BuiltInRegistries.ITEM, SimpleChunkLoader.id("chunk_loader"),
			new ChunkLoaderBlockItem(
				chunkLoader,
				new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, SimpleChunkLoader.id("chunk_loader"))),
				Items.LODESTONE,
				false
			));

		chunkLoaderBlockEntity = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleChunkLoader.id("chunk_loader"),
			FabricBlockEntityTypeBuilder.create(ChunkLoaderBlockEntity::new, chunkLoader).build());
		PolymerBlockUtils.registerBlockEntity(chunkLoaderBlockEntity, new PolymerSyncedObject<>() {
			@Override
			public BlockEntityType<?> getPolymerReplacement(BlockEntityType<?> obj, PacketContext context) {
				// Modded clients receive the real block entity type so data packets are not stripped.
				// Vanilla clients get null (block entity data suppressed).
				return isModdedClient(context) ? obj : null;
			}

			@Override
			public boolean canSyncRawToClient(PacketContext context) {
				return isModdedClient(context);
			}
		});

		CreativeModeTab tab = PolymerItemGroupUtils.builder()
			.title(Component.translatable("itemGroup.simple-chunk-loader.chunk_loader"))
			.icon(() -> new ItemStack(Items.LODESTONE))
			.displayItems((params, output) -> {
				output.accept(chunkLoaderItem);
			})
			.build();
		PolymerItemGroupUtils.registerPolymerItemGroup(
			SimpleChunkLoader.id("chunk_loader"),
			tab
		);

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			entries.accept(chunkLoaderItem);
		});
	}
}
