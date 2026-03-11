package com.kvit;

import com.kvit.blocks.chunkLoader.ChunkLoaderBlock;
import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
import com.kvit.items.ChunkLoaderBlockItem;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModContent {
	private static Block chunkLoader;
	private static Item chunkLoaderItem;
	private static BlockEntityType<ChunkLoaderBlockEntity> chunkLoaderBlockEntity;

	private ModContent() {
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
		PolymerBlockUtils.registerBlockEntity(chunkLoaderBlockEntity);

		CreativeModeTab tab = PolymerItemGroupUtils.builder()
			.title(Component.literal("Simple Chunk Loader"))
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
