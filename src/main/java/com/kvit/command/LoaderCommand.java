package com.kvit.command;

import com.kvit.ModContent;
import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
import com.kvit.loader.ChunkLoaderManager;
import com.kvit.loader.ChunkLoaderManager.LoaderReference;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class LoaderCommand {
	private static final int PAGE_SIZE = 8;
	private static final SuggestionProvider<CommandSourceStack> LOADER_ID_SUGGESTIONS = (context, builder) ->
		SharedSuggestionProvider.suggest(
			ChunkLoaderManager.getAllLoaders(context.getSource().getServer()).stream()
				.map(loader -> Integer.toString(loader.record().id())),
			builder
		);

	private LoaderCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("loader")
				.then(
					Commands.literal("list")
						.executes(context -> listLoaders(context.getSource(), 1))
						.then(
							Commands.argument("page", IntegerArgumentType.integer(1))
								.executes(context -> listLoaders(context.getSource(), IntegerArgumentType.getInteger(context, "page")))
						)
				)
				.then(toggleLiteral("enable", true))
				.then(toggleLiteral("disable", false))
				.then(
					Commands.literal("tp")
						.then(
							Commands.argument("id", IntegerArgumentType.integer(1))
								.suggests(LOADER_ID_SUGGESTIONS)
								.executes(context -> teleportToLoader(
									context,
									IntegerArgumentType.getInteger(context, "id")
								))
						)
				)
		);
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> toggleLiteral(String name, boolean enabled) {
		return Commands.literal(name)
			.then(
				Commands.argument("id", IntegerArgumentType.integer(1))
					.suggests(LOADER_ID_SUGGESTIONS)
					.executes(context -> setLoaderEnabled(
						context.getSource(),
						IntegerArgumentType.getInteger(context, "id"),
						enabled
					))
			);
	}

	private static int listLoaders(CommandSourceStack source, int page) {
		List<LoaderReference> loaders = ChunkLoaderManager.getAllLoaders(source.getServer());
		if (loaders.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No chunk loaders are placed yet.").withStyle(ChatFormatting.YELLOW), false);
			return 0;
		}

		int totalPages = Math.max(1, Math.ceilDiv(loaders.size(), PAGE_SIZE));
		if (page > totalPages) {
			source.sendFailure(Component.literal("Only " + totalPages + " page(s) available.").withStyle(ChatFormatting.YELLOW));
			return 0;
		}

		int fromIndex = (page - 1) * PAGE_SIZE;
		int toIndex = Math.min(fromIndex + PAGE_SIZE, loaders.size());
		MutableComponent message = Component.empty();
		appendLine(message, Component.literal("Chunk Loaders")
			.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		appendLine(message, Component.literal(loaders.size() + " total  |  Page " + page + "/" + totalPages)
			.withStyle(ChatFormatting.GRAY));

		for (LoaderReference loader : loaders.subList(fromIndex, toIndex)) {
			appendLine(message, formatLoaderLine(loader));
		}

		if (totalPages > 1) {
			appendLine(message, Component.literal("Use /loader list <page> to browse more.")
				.withStyle(ChatFormatting.DARK_GRAY));
		}

		source.sendSuccess(() -> message, false);
		return toIndex - fromIndex;
	}

	private static int setLoaderEnabled(CommandSourceStack source, int id, boolean enabled) {
		Optional<LoaderReference> optionalLoader = resolveLoader(source, id);
		if (optionalLoader.isEmpty()) {
			source.sendFailure(Component.literal("No chunk loader with id #" + id + " was found.").withStyle(ChatFormatting.RED));
			return 0;
		}

		LoaderReference loader = optionalLoader.get();
		if (loader.record().enabled() == enabled) {
			source.sendSuccess(() -> Component.empty()
				.append(Component.literal("Loader #" + id + " is already "))
				.append(statusComponent(enabled)), false);
			return 1;
		}

		BlockPos pos = loader.blockPos();
		ServerLevel level = loader.level();
		if (level.getBlockEntity(pos) instanceof ChunkLoaderBlockEntity blockEntity) {
			blockEntity.setEnabled(enabled);
		} else {
			ChunkLoaderManager.upsert(level, pos, enabled, loader.record().expansionLevel());
		}

		source.sendSuccess(() -> Component.empty()
			.append(Component.literal(enabled ? "Enabled " : "Disabled ").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GOLD))
			.append(Component.literal("loader #" + id + " ").withStyle(ChatFormatting.WHITE))
			.append(locationComponent(loader)), false);
		return 1;
	}

	private static int teleportToLoader(CommandContext<CommandSourceStack> context, int id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		Optional<LoaderReference> optionalLoader = resolveLoader(source, id);
		if (optionalLoader.isEmpty()) {
			source.sendFailure(Component.literal("No chunk loader with id #" + id + " was found.").withStyle(ChatFormatting.RED));
			return 0;
		}

		ServerPlayer player = source.getPlayerOrException();
		LoaderReference loader = optionalLoader.get();
		Optional<Vec3> target = findTeleportTarget(loader.level(), loader.blockPos());
		if (target.isEmpty()) {
			source.sendFailure(Component.literal("Could not find a safe spot next to loader #" + id + ".")
				.withStyle(ChatFormatting.RED));
			return 0;
		}

		Vec3 position = target.get();
		boolean teleported = player.teleportTo(
			loader.level(),
			position.x,
			position.y,
			position.z,
			Set.<Relative>of(),
			player.getYRot(),
			player.getXRot(),
			true
		);
		if (!teleported) {
			source.sendFailure(Component.literal("Teleport to loader #" + id + " failed.").withStyle(ChatFormatting.RED));
			return 0;
		}

		source.sendSuccess(() -> Component.empty()
			.append(Component.literal("Teleported to loader #" + id + " ").withStyle(ChatFormatting.GREEN))
			.append(locationComponent(loader)), false);
		return 1;
	}

	private static Optional<LoaderReference> resolveLoader(CommandSourceStack source, int id) {
		Optional<LoaderReference> optionalLoader = ChunkLoaderManager.getLoader(source.getServer(), id);
		if (optionalLoader.isEmpty()) {
			return Optional.empty();
		}

		LoaderReference loader = optionalLoader.get();
		if (loader.level().isLoaded(loader.blockPos()) && !loader.level().getBlockState(loader.blockPos()).is(ModContent.chunkLoader())) {
			ChunkLoaderManager.remove(loader.level(), loader.blockPos());
			return Optional.empty();
		}

		return Optional.of(loader);
	}

	private static MutableComponent formatLoaderLine(LoaderReference loader) {
		int areaSize = 1 + 2 * loader.record().expansionLevel();
		return Component.empty()
			.append(Component.literal("#" + loader.record().id()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal("  "))
			.append(statusComponent(loader.record().enabled()))
			.append(Component.literal("  "))
			.append(Component.literal(displayDimension(loader.level())).withStyle(ChatFormatting.AQUA))
			.append(Component.literal("  "))
			.append(Component.literal(formatCoordinates(loader.blockPos())).withStyle(ChatFormatting.GRAY))
			.append(Component.literal("  "))
			.append(Component.literal(areaSize + "x" + areaSize + " chunks").withStyle(ChatFormatting.DARK_GRAY));
	}

	private static MutableComponent locationComponent(LoaderReference loader) {
		return Component.empty()
			.append(Component.literal("at ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(displayDimension(loader.level())).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(formatCoordinates(loader.blockPos())).withStyle(ChatFormatting.GRAY));
	}

	private static MutableComponent statusComponent(boolean enabled) {
		return Component.literal(enabled ? "ENABLED" : "DISABLED")
			.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GOLD, ChatFormatting.BOLD);
	}

	private static Optional<Vec3> findTeleportTarget(ServerLevel level, BlockPos loaderPos) {
		for (int radius = 1; radius <= 2; radius++) {
			for (int verticalOffset : new int[]{0, 1, -1, 2}) {
				for (Direction direction : Direction.Plane.HORIZONTAL) {
					BlockPos candidate = loaderPos.relative(direction, radius).offset(0, verticalOffset, 0);
					Vec3 safe = DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, candidate, true);
					if (safe != null) {
						return Optional.of(safe);
					}
				}
			}
		}

		for (int verticalOffset : new int[]{1, 2, 0, -1}) {
			Vec3 safe = DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, loaderPos.offset(0, verticalOffset, 0), true);
			if (safe != null) {
				return Optional.of(safe);
			}
		}

		return Optional.empty();
	}

	private static String displayDimension(ServerLevel level) {
		String id = level.dimension().identifier().toString();
		return switch (id) {
			case "minecraft:overworld" -> "Overworld";
			case "minecraft:the_nether" -> "Nether";
			case "minecraft:the_end" -> "The End";
			default -> id;
		};
	}

	private static String formatCoordinates(BlockPos pos) {
		return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
	}

	private static void appendLine(MutableComponent message, Component line) {
		if (!message.getSiblings().isEmpty() || message.getString().length() > 0) {
			message.append(Component.literal("\n"));
		}
		message.append(line.copy().withStyle(unitalic()));
	}

	private static UnaryOperator<net.minecraft.network.chat.Style> unitalic() {
		return style -> style.withItalic(false);
	}
}
