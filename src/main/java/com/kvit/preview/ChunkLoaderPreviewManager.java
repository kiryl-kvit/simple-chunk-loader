package com.kvit.preview;

import com.kvit.SimpleChunkLoader;
import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
import com.kvit.config.SimpleChunkLoaderConfig;
import com.kvit.loader.ChunkBounds;
import com.kvit.loader.ChunkLoaderManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ChunkLoaderPreviewManager {
	private static final DustParticleOptions PARTICLE_ENABLED = new DustParticleOptions(0x33CC66, 2.5F);
	private static final DustParticleOptions PARTICLE_DISABLED = new DustParticleOptions(0xFFAA00, 2.5F);
	private static final int PILLAR_HEIGHT = 6;

	private static final Map<UUID, PreviewSession> ACTIVE_PREVIEWS = new HashMap<>();

	private ChunkLoaderPreviewManager() {
	}

	public static void toggle(ServerPlayer player, ChunkLoaderBlockEntity blockEntity) {
		if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		BlockPos pos = blockEntity.getBlockPos().immutable();
		PreviewSession next = new PreviewSession(
			serverLevel.dimension(), pos, ChunkLoaderManager.getChunkBounds(pos)
		);
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
		SimpleChunkLoaderConfig config = SimpleChunkLoader.getConfig();
		if (server.getTickCount() % config.previewRefreshTicks() != 0) {
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
				// Player is offline, dimension unloaded, or player changed dimension —
				// skip rendering but keep the session alive so the menu button state
				// remains correct if the player returns.
				continue;
			}

			if (!(level.getBlockEntity(entry.getValue().pos()) instanceof ChunkLoaderBlockEntity blockEntity)) {
				iterator.remove();
				continue;
			}

			render(level, player, blockEntity, entry.getValue().bounds(), config.previewParticleStep());
		}
	}

	public static void clearAll() {
		ACTIVE_PREVIEWS.clear();
	}

	private static void render(ServerLevel level, ServerPlayer player, ChunkLoaderBlockEntity blockEntity,
							   ChunkBounds bounds, int step) {
		DustParticleOptions particle = blockEntity.isEnabled() ? PARTICLE_ENABLED : PARTICLE_DISABLED;
		double y = blockEntity.getBlockPos().getY() + 1.1D;

		double minX = bounds.minBlockX();
		double maxX = bounds.maxBlockXExclusive();
		double minZ = bounds.minBlockZ();
		double maxZ = bounds.maxBlockZExclusive();

		// Render all 4 corners
		send(level, player, particle, minX, y, minZ);
		send(level, player, particle, minX, y, maxZ);
		send(level, player, particle, maxX, y, minZ);
		send(level, player, particle, maxX, y, maxZ);

		// North/south edges (excluding corners)
		int lastBlockX = bounds.maxBlockXExclusive() - 1;
		for (int x = bounds.minBlockX() + step; x <= lastBlockX; x += step) {
			send(level, player, particle, x + 0.5D, y, minZ);
			send(level, player, particle, x + 0.5D, y, maxZ);
		}

		// West/east edges (excluding corners)
		int lastBlockZ = bounds.maxBlockZExclusive() - 1;
		for (int z = bounds.minBlockZ() + step; z <= lastBlockZ; z += step) {
			send(level, player, particle, minX, y, z + 0.5D);
			send(level, player, particle, maxX, y, z + 0.5D);
		}

		// Corner pillars
		for (double pillarY = y + 1.0D; pillarY <= y + PILLAR_HEIGHT; pillarY += 1.0D) {
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
