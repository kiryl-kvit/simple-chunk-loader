package com.kvit;

import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
import com.kvit.command.LoaderCommand;
import com.kvit.config.SimpleChunkLoaderConfig;
import com.kvit.loader.ChunkLoaderManager;
import com.kvit.network.VersionNetworking;
import com.kvit.preview.ChunkLoaderPreviewManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class SimpleChunkLoader implements ModInitializer {
	public static final String MOD_ID = "simple-chunk-loader";
	public static final String MOD_VERSION = FabricLoader.getInstance()
		.getModContainer(MOD_ID)
		.orElseThrow(() -> new IllegalStateException("Missing mod container for " + MOD_ID))
		.getMetadata()
		.getVersion()
		.getFriendlyString();
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static SimpleChunkLoaderConfig config;

	@Override
	public void onInitialize() {
		config = SimpleChunkLoaderConfig.load();
		ModContent.register();
		VersionNetworking.register();

		ServerWorldEvents.LOAD.register((server, world) -> ChunkLoaderManager.handleWorldLoad(world));
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
			if (blockEntity instanceof ChunkLoaderBlockEntity chunkLoader) {
				ChunkLoaderManager.upsert(world, chunkLoader.getBlockPos(), chunkLoader.isEnabled(), chunkLoader.getExpansionLevel());
			}
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> LoaderCommand.register(dispatcher));
		ServerTickEvents.END_SERVER_TICK.register(ChunkLoaderPreviewManager::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ChunkLoaderPreviewManager.clearAll());

		LOGGER.info("{} initialized with max_loaders={}", MOD_ID, config.maxLoaders());
	}

	public static String getModVersion() {
		return MOD_VERSION;
	}

	public static SimpleChunkLoaderConfig getConfig() {
		return Objects.requireNonNull(config, "Config accessed before mod initialization");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
