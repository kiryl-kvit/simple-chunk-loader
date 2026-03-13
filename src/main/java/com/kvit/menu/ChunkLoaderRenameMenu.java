package com.kvit.menu;

import com.kvit.ModContent;
import com.kvit.loader.ChunkLoaderManager;
import com.kvit.loader.LoaderMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

public final class ChunkLoaderRenameMenu extends AnvilMenu {
	private final ServerLevel level;
	private final BlockPos pos;
	private String pendingName = "";

	public ChunkLoaderRenameMenu(int syncId, Inventory playerInventory, ServerLevel level, BlockPos pos) {
		super(syncId, playerInventory, ContainerLevelAccess.create(level, pos));
		this.level = level;
		this.pos = pos.immutable();
		this.initializeInput();
	}

	@Override
	public boolean stillValid(@NonNull Player player) {
		return stillValid(ContainerLevelAccess.create(this.level, this.pos), player, ModContent.chunkLoader());
	}

	@Override
	protected boolean mayPickup(@NonNull Player player, boolean hasStack) {
		return hasStack && !this.resultSlots.getItem(0).isEmpty();
	}

	@Override
	protected void onTake(@NonNull Player player, @NonNull ItemStack stack) {
		String nextName = this.pendingName;
		ChunkLoaderManager.rename(this.level, this.pos, nextName);

		this.inputSlots.removeItemNoUpdate(0);
		this.inputSlots.removeItemNoUpdate(1);
		this.resultSlots.setItem(0, ItemStack.EMPTY);
		this.setCarried(ItemStack.EMPTY);
		this.broadcastChanges();

		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(LoaderMessages.renameResult(nextName));
			serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
				(syncId, inventory, openPlayer) -> new ChunkLoaderMenu(syncId, inventory, this.level, this.pos, openPlayer.getUUID()),
				Component.literal("Chunk Loader")
			));
		}
	}

	@Override
	public boolean setItemName(String name) {
		this.pendingName = ChunkLoaderManager.normalizeName(name);
		return super.setItemName(this.pendingName);
	}

	@Override
	public void createResult() {
		ItemStack base = this.inputSlots.getItem(0);
		if (base.isEmpty()) {
			this.resultSlots.setItem(0, ItemStack.EMPTY);
			this.broadcastChanges();
			return;
		}

		ItemStack result = base.copy();
		if (this.pendingName.isEmpty()) {
			result.remove(DataComponents.CUSTOM_NAME);
		} else {
			result.set(DataComponents.CUSTOM_NAME, MenuComponents.plain(Component.literal(this.pendingName)));
		}
		this.resultSlots.setItem(0, result);
		this.broadcastChanges();
	}

	private void initializeInput() {
		String currentName = ChunkLoaderManager.getLoader(this.level, this.pos)
			.map(loader -> loader.name())
			.orElse("");
		this.pendingName = currentName;

		ItemStack stack = new ItemStack(Items.NAME_TAG);
		stack.set(DataComponents.ITEM_NAME, MenuComponents.plain(Component.literal("Chunk Loader")));
		if (!currentName.isBlank()) {
			stack.set(DataComponents.CUSTOM_NAME, MenuComponents.plain(Component.literal(currentName)));
		}

		this.inputSlots.setItem(0, stack);
		this.slotsChanged(this.inputSlots);
		this.setItemName(currentName);
	}

}
