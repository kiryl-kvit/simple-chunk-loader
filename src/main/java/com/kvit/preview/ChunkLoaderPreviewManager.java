package com.kvit.preview;

import com.kvit.SimpleChunkLoader;
import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
import com.kvit.loader.ChunkBounds;
import com.kvit.loader.ChunkLoaderManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkLoaderPreviewManager {
	private static final Map<UUID, PreviewSession> ACTIVE_PREVIEWS = new ConcurrentHashMap<>();

	private ChunkLoaderPreviewManager() {
	}

	public static void toggle(ServerPlayer player, ChunkLoaderBlockEntity blockEntity) {
		if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		PreviewSession next = new PreviewSession(serverLevel.dimension(), blockEntity.getBlockPos().immutable());
		PreviewSession current = ACTIVE_PREVIEWS.get(player.getUUID());

		if (next.equals(current)) {
			ACTIVE_PREVIEWS.remove(player.getUUID());
		} else {
			ACTIVE_PREVIEWS.put(player.getUUID(), next);
		}
	}

	public static boolean isPreviewing(UUID playerId, ServerLevel level, BlockPos pos) {
		PreviewSession session = ACTIVE_PREVIEWS.get(playerId);
		return session != null && session.dimension().equals(level.dimension()) && session.pos().equals(pos);
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % SimpleChunkLoader.getConfig().previewRefreshTicks() != 0) {
			return;
		}

		Iterator<Map.Entry<UUID, PreviewSession>> iterator = ACTIVE_PREVIEWS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, PreviewSession> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				iterator.remove();
				continue;
			}

			ServerLevel level = server.getLevel(entry.getValue().dimension());
			if (level == null || player.level() != level) {
				iterator.remove();
				continue;
			}

			if (!(level.getBlockEntity(entry.getValue().pos()) instanceof ChunkLoaderBlockEntity blockEntity)) {
				iterator.remove();
				continue;
			}

			render(level, player, blockEntity);
		}
	}

	public static void clearAll() {
		ACTIVE_PREVIEWS.clear();
	}

	private static void render(ServerLevel level, ServerPlayer player, ChunkLoaderBlockEntity blockEntity) {
		ChunkBounds bounds = ChunkLoaderManager.getChunkBounds(blockEntity.getBlockPos());
		DustParticleOptions particle = new DustParticleOptions(blockEntity.isEnabled() ? 0x33CC66 : 0xFFAA00, 2.5F);
		int step = SimpleChunkLoader.getConfig().previewParticleStep();
		double y = blockEntity.getBlockPos().getY() + 1.1D;

		double minX = bounds.minBlockX();
		double maxX = bounds.maxBlockXInclusive() + 1.0D;
		double minZ = bounds.minBlockZ();
		double maxZ = bounds.maxBlockZInclusive() + 1.0D;

		// Render all 4 corners
		send(level, player, particle, minX, y, minZ);
		send(level, player, particle, minX, y, maxZ);
		send(level, player, particle, maxX, y, minZ);
		send(level, player, particle, maxX, y, maxZ);

		// North/south edges (excluding corners)
		for (int x = bounds.minBlockX() + step; x <= bounds.maxBlockXInclusive(); x += step) {
			send(level, player, particle, x + 0.5D, y, minZ);
			send(level, player, particle, x + 0.5D, y, maxZ);
		}

		// West/east edges (excluding corners)
		for (int z = bounds.minBlockZ() + step; z <= bounds.maxBlockZInclusive(); z += step) {
			send(level, player, particle, minX, y, z + 0.5D);
			send(level, player, particle, maxX, y, z + 0.5D);
		}

		// Corner pillars
		for (double pillarY = y + 1.0D; pillarY <= y + 6.0D; pillarY += 1.0D) {
			send(level, player, particle, minX, pillarY, minZ);
			send(level, player, particle, minX, pillarY, maxZ);
			send(level, player, particle, maxX, pillarY, minZ);
			send(level, player, particle, maxX, pillarY, maxZ);
		}
	}

	private static void send(ServerLevel level, ServerPlayer player, DustParticleOptions particle, double x, double y, double z) {
		level.sendParticles(player, particle, true, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
	}
}
