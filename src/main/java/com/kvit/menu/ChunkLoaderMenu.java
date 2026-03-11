package com.kvit.menu;

import com.kvit.ModContent;
import com.kvit.blocks.chunkLoader.entity.ChunkLoaderBlockEntity;
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
	private static final int ENABLE_SLOT = 11;
	private static final int PREVIEW_SLOT = 13;
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
		this.refresh();
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
			if (clickType == ClickType.PICKUP) {
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
			case PREVIEW_SLOT -> {
				if (player instanceof ServerPlayer serverPlayer) {
					ChunkLoaderPreviewManager.toggle(serverPlayer, blockEntity);
				}
			}
			case ENABLE_SLOT -> blockEntity.setEnabled(!blockEntity.isEnabled());
			default -> {
				return;
			}
		}

		this.refresh();
		this.broadcastFullState();
	}

	private void refresh() {
		ChunkLoaderBlockEntity blockEntity = this.getBlockEntity();
		for (int i = 0; i < MENU_SIZE; i++) {
			this.container.setItem(i, filler());
		}

		if (blockEntity == null) {
			return;
		}

		boolean previewing = ChunkLoaderPreviewManager.isPreviewing(this.playerId, this.level, this.pos);

		this.container.setItem(ENABLE_SLOT, actionItem(
			blockEntity.isEnabled() ? Items.LEVER : Items.REDSTONE_TORCH,
			Component.literal(blockEntity.isEnabled() ? "Disable loader" : "Enable loader")
				.withStyle(blockEntity.isEnabled() ? ChatFormatting.RED : ChatFormatting.GREEN),
			Component.literal(blockEntity.isEnabled() ? "Loader status: enabled" : "Loader status: disabled")
				.withStyle(blockEntity.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.GOLD)
		));

		this.container.setItem(PREVIEW_SLOT, actionItem(
			previewing ? Items.ENDER_EYE : Items.SPYGLASS,
			Component.literal(previewing ? "Hide Area" : "Show Area")
				.withStyle(previewing ? ChatFormatting.YELLOW : ChatFormatting.GREEN),
			Component.literal("Preview stays visible until you turn it off.").withStyle(ChatFormatting.GRAY)
		));
	}

	private ChunkLoaderBlockEntity getBlockEntity() {
		return this.level.getBlockEntity(this.pos) instanceof ChunkLoaderBlockEntity blockEntity ? blockEntity : null;
	}

	private static ItemStack filler() {
		ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		stack.set(DataComponents.ITEM_NAME, plain(Component.literal(" ")));
		return stack;
	}

	private static ItemStack actionItem(Item item, Component name, Component... loreLines) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.ITEM_NAME, plain(name));

		if (loreLines.length > 0) {
			List<Component> lines = new ArrayList<>(loreLines.length);
			for (Component loreLine : loreLines) {
				lines.add(plain(loreLine));
			}
			stack.set(DataComponents.LORE, new ItemLore(lines));
		}

		return stack;
	}

	private static Component plain(Component component) {
		return component.copy().withStyle(style -> style.withItalic(false));
	}
}
