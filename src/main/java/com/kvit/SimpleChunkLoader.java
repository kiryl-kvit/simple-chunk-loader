package com.kvit;

import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
import com.kvit.config.SimpleChunkLoaderConfig;
import com.kvit.loader.ChunkLoaderManager;
import com.kvit.preview.ChunkLoaderPreviewManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleChunkLoader implements ModInitializer {
	public static final String MOD_ID = "simple-chunk-loader";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static SimpleChunkLoaderConfig config;

	@Override
	public void onInitialize() {
		config = SimpleChunkLoaderConfig.load();
		ModContent.register();

		ServerWorldEvents.LOAD.register((server, world) -> ChunkLoaderManager.handleWorldLoad(world));
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
			if (blockEntity instanceof ChunkLoaderBlockEntity chunkLoader) {
				ChunkLoaderManager.upsert(world, chunkLoader.getBlockPos(), chunkLoader.isEnabled());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(ChunkLoaderPreviewManager::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ChunkLoaderPreviewManager.clearAll());

		LOGGER.info("{} initialized with max_loaders={}", MOD_ID, config.maxLoaders());
	}

	public static SimpleChunkLoaderConfig getConfig() {
		return config;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
