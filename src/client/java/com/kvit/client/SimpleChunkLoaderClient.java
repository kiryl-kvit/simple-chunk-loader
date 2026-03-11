package com.kvit.client;

import com.kvit.SimpleChunkLoader;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleChunkLoaderClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleChunkLoader.MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("{} client initialized", SimpleChunkLoader.MOD_ID);
	}
}
