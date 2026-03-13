package com.kvit.menu;

import com.kvit.ModContent;
import com.kvit.SimpleChunkLoader;
import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
import com.kvit.loader.ChunkLoaderManager;
import com.kvit.preview.ChunkLoaderPreviewManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ChunkLoaderMenu extends ChestMenu {
	private static final int MENU_SIZE = 27;
	// Slot layout in 3x9 grid:
	// row 2 col 2 (rename), row 2 col 3 (enable), row 2 col 5 (preview), row 2 col 7 (shrink), row 2 col 8 (expand)
	private static final int RENAME_SLOT = 10;
	private static final int ENABLE_SLOT = 11;
	private static final int PREVIEW_SLOT = 13;
	private static final int SHRINK_SLOT = 15;
	private static final int EXPAND_SLOT = 16;
	private static final int SPAWNING_SLOT = 17;
	private static final ItemStack FILLER_TEMPLATE;

	static {
		FILLER_TEMPLATE = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		FILLER_TEMPLATE.set(DataComponents.ITEM_NAME, MenuComponents.plain(Component.literal(" ")));
	}

	private final SimpleContainer container;
	private final ServerLevel level;
	private final BlockPos pos;
	private final UUID playerId;

	public ChunkLoaderMenu(int syncId, Inventory playerInventory, ServerLevel level, BlockPos pos, UUID playerId) {
		this(syncId, playerInventory, new SimpleContainer(MENU_SIZE), level, pos, playerId);
	}

	private ChunkLoaderMenu(int syncId, Inventory playerInventory, SimpleContainer container, ServerLevel level, BlockPos pos, UUID playerId) {
		super(MenuType.GENERIC_9x3, syncId, playerInventory, container, 3);
		this.container = container;
		this.level = level;
		this.pos = pos.immutable();
		this.playerId = playerId;
		this.fillBackground();
		this.refreshActionSlots();
	}

	@Override
	public boolean stillValid(@NonNull Player player) {
		return AbstractContainerMenu.stillValid(ContainerLevelAccess.create(this.level, this.pos), player, ModContent.chunkLoader());
	}

	@Override
	public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public void clicked(int slotId, int button, @NonNull ClickType clickType, @NonNull Player player) {
		if (slotId >= 0 && slotId < MENU_SIZE) {
			if (clickType == ClickType.PICKUP && button == 0) {
				this.handleMenuClick(slotId, player);
			}
			return;
		}

		super.clicked(slotId, button, clickType, player);
	}

	private void handleMenuClick(int slotId, Player player) {
		ChunkLoaderBlockEntity blockEntity = this.getBlockEntity();
		if (blockEntity == null) {
			return;
		}

			switch (slotId) {
			case RENAME_SLOT -> {
				if (player instanceof ServerPlayer serverPlayer) {
					this.openRenameMenu(serverPlayer);
				}
				return;
			}
			case PREVIEW_SLOT -> {
				if (player instanceof ServerPlayer serverPlayer) {
					ChunkLoaderPreviewManager.toggle(serverPlayer, blockEntity);
				}
			}
			case ENABLE_SLOT -> ChunkLoaderManager.setEnabled(this.level, this.pos, !blockEntity.isEnabled());
			case EXPAND_SLOT -> {
				int maxLevel = SimpleChunkLoader.getConfig().maxExpansionLevel();
				if (blockEntity.getExpansionLevel() < maxLevel) {
					blockEntity.setExpansionLevel(blockEntity.getExpansionLevel() + 1);
				}
			}
			case SHRINK_SLOT -> {
				if (blockEntity.getExpansionLevel() > 0) {
					blockEntity.setExpansionLevel(blockEntity.getExpansionLevel() - 1);
				}
			}
			case SPAWNING_SLOT -> ChunkLoaderManager.setAllowNaturalSpawning(this.level, this.pos, !blockEntity.isAllowNaturalSpawning());
			default -> {
				return;
			}
		}

		this.refreshActionSlots();
		this.broadcastFullState();
	}

	private void fillBackground() {
		for (int i = 0; i < MENU_SIZE; i++) {
			this.container.setItem(i, filler());
		}
	}

	private void refreshActionSlots() {
		ChunkLoaderBlockEntity blockEntity = this.getBlockEntity();
		if (blockEntity == null) {
			return;
		}

		boolean enabled = blockEntity.isEnabled();
		boolean previewing = ChunkLoaderPreviewManager.isPreviewing(this.playerId, this.level, this.pos);
		String loaderName = ChunkLoaderManager.getLoader(this.level, this.pos)
			.map(loader -> loader.name())
			.filter(name -> !name.isBlank())
			.orElse("none");

		this.container.setItem(RENAME_SLOT, actionItem(
			Items.NAME_TAG,
			Component.literal("Rename Loader").withStyle(ChatFormatting.AQUA),
			Component.literal("Current name: " + loaderName).withStyle(ChatFormatting.GRAY)
		));

		this.container.setItem(ENABLE_SLOT, actionItem(
			enabled ? Items.LEVER : Items.REDSTONE_TORCH,
			Component.literal(enabled ? "Disable loader" : "Enable loader")
				.withStyle(enabled ? ChatFormatting.RED : ChatFormatting.GREEN),
			Component.literal(enabled ? "Loader status: enabled" : "Loader status: disabled")
				.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GOLD)
		));

		this.container.setItem(PREVIEW_SLOT, actionItem(
			previewing ? Items.ENDER_EYE : Items.SPYGLASS,
			Component.literal(previewing ? "Hide Area" : "Show Area")
				.withStyle(previewing ? ChatFormatting.YELLOW : ChatFormatting.GREEN),
			Component.literal("Preview stays visible until you turn it off.").withStyle(ChatFormatting.GRAY)
		));

		int currentLevel = blockEntity.getExpansionLevel();
		int maxLevel = SimpleChunkLoader.getConfig().maxExpansionLevel();
		int areaSize = 1 + 2 * currentLevel;
		Component areaLore = Component.literal("Area: " + areaSize + "x" + areaSize + " chunks")
			.withStyle(ChatFormatting.GRAY);

		boolean canExpand = currentLevel < maxLevel;
		this.container.setItem(EXPAND_SLOT, actionItem(
			canExpand ? Items.LIME_DYE : Items.GRAY_DYE,
			Component.literal("Expand Area")
				.withStyle(canExpand ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY),
			areaLore
		));

		boolean canShrink = currentLevel > 0;
		this.container.setItem(SHRINK_SLOT, actionItem(
			canShrink ? Items.RED_DYE : Items.GRAY_DYE,
			Component.literal("Shrink Area")
				.withStyle(canShrink ? ChatFormatting.RED : ChatFormatting.DARK_GRAY),
			areaLore
		));

		boolean spawning = blockEntity.isAllowNaturalSpawning();
		this.container.setItem(SPAWNING_SLOT, actionItem(
			spawning ? Items.ZOMBIE_HEAD : Items.BARRIER,
			Component.literal(spawning ? "Disable Mob Spawning" : "Enable Mob Spawning")
				.withStyle(spawning ? ChatFormatting.RED : ChatFormatting.GREEN),
			Component.literal(spawning ? "Natural mob spawning: enabled" : "Natural mob spawning: disabled")
				.withStyle(spawning ? ChatFormatting.GREEN : ChatFormatting.GOLD)
		));
	}

	private ChunkLoaderBlockEntity getBlockEntity() {
		return this.level.getBlockEntity(this.pos) instanceof ChunkLoaderBlockEntity blockEntity ? blockEntity : null;
	}

	private void openRenameMenu(ServerPlayer player) {
		player.openMenu(new net.minecraft.world.SimpleMenuProvider(
			(syncId, inventory, openPlayer) -> new ChunkLoaderRenameMenu(syncId, inventory, this.level, this.pos),
			Component.literal("Rename Loader")
		));
	}

	private static ItemStack filler() {
		return FILLER_TEMPLATE.copy();
	}

	private static ItemStack actionItem(Item item, Component name, Component... loreLines) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.ITEM_NAME, MenuComponents.plain(name));

		if (loreLines.length > 0) {
			List<Component> lines = new ArrayList<>(loreLines.length);
			for (Component line : loreLines) {
				lines.add(MenuComponents.plain(line));
			}
			stack.set(DataComponents.LORE, new ItemLore(lines));
		}

		return stack;
	}
}
