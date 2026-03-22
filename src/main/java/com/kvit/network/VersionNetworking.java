package com.kvit.network;

import com.kvit.SimpleChunkLoader;
import eu.pb4.polymer.networking.api.PolymerNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;

public final class VersionNetworking {
	private VersionNetworking() {
	}

	public static void register() {
		PolymerNetworking.registerCommonVersioned(
			ChunkLoaderPresencePayload.TYPE,
			ChunkLoaderPresencePayload.PROTOCOL_VERSION,
			ChunkLoaderPresencePayload.CODEC
		);
		PayloadTypeRegistry.playC2S().register(ClientVersionPayload.TYPE, ClientVersionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ClientVersionPayload.TYPE, (payload, context) -> {
			String serverVersion = SimpleChunkLoader.getModVersion();
			String clientVersion = payload.version();

			if (serverVersion.equals(clientVersion)) {
				return;
			}

			context.player().sendSystemMessage(Component.literal(buildMismatchMessage(serverVersion, clientVersion)), false);
		});
	}

	private static String buildMismatchMessage(String serverVersion, String clientVersion) {
		return "Your version of " + SimpleChunkLoader.MOD_ID + " does not match one installed on the server, this could lead to issues with the mod's client-side. Please, install matching version.\n"
			+ "Server version - " + serverVersion + "\n"
			+ "Client version - " + clientVersion;
	}
}
