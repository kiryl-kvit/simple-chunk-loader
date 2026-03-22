package com.kvit.network;

import com.kvit.SimpleChunkLoader;
import eu.pb4.polymer.networking.api.ContextByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChunkLoaderPresencePayload() implements CustomPacketPayload {
	public static final int PROTOCOL_VERSION = 1;
	public static final Identifier CHANNEL_ID = SimpleChunkLoader.id("presence");
	public static final ChunkLoaderPresencePayload INSTANCE = new ChunkLoaderPresencePayload();
	public static final Type<ChunkLoaderPresencePayload> TYPE = new Type<>(CHANNEL_ID);
	public static final StreamCodec<ContextByteBuf, ChunkLoaderPresencePayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<ChunkLoaderPresencePayload> type() {
		return TYPE;
	}
}
