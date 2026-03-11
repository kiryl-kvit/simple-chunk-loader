package com.kvit;

import com.kvit.blocks.chunkLoader.ChunkLoaderBlock;
import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
import com.kvit.items.ChunkLoaderBlockItem;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModContent {
	public static Block CHUNK_LOADER;
	public static Item CHUNK_LOADER_ITEM;
	public static BlockEntityType<ChunkLoaderBlockEntity> CHUNK_LOADER_BLOCK_ENTITY;

	private ModContent() {
	}

	public static void register() {
		CHUNK_LOADER = Registry.register(BuiltInRegistries.BLOCK, SimpleChunkLoader.id("chunk_loader"),
			new ChunkLoaderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LODESTONE)
				.setId(ResourceKey.create(Registries.BLOCK, SimpleChunkLoader.id("chunk_loader")))));

		CHUNK_LOADER_ITEM = Registry.register(BuiltInRegistries.ITEM, SimpleChunkLoader.id("chunk_loader"),
			new ChunkLoaderBlockItem(
				CHUNK_LOADER,
				new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, SimpleChunkLoader.id("chunk_loader"))),
				Items.LODESTONE,
				false
			));

		CHUNK_LOADER_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleChunkLoader.id("chunk_loader"),
			FabricBlockEntityTypeBuilder.create(ChunkLoaderBlockEntity::new, CHUNK_LOADER).build());
		PolymerBlockUtils.registerBlockEntity(CHUNK_LOADER_BLOCK_ENTITY);
	}
}
