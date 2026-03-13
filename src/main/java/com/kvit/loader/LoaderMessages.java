package com.kvit.loader;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class LoaderMessages {
	private LoaderMessages() {
	}

	public static MutableComponent namedComponent(String name) {
		return Component.literal("\"" + name + "\"")
			.withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC);
	}

	public static MutableComponent renameResult(String name) {
		return renameResult("Cleared loader name", "Renamed loader to ", name);
	}

	public static MutableComponent renameResult(int id, String name) {
		return renameResult("Cleared name for loader #" + id, "Renamed loader #" + id + " to ", name);
	}

	private static MutableComponent renameResult(String clearedText, String renamedText, String name) {
		return Component.empty()
			.append(Component.literal(name.isEmpty() ? clearedText : renamedText)
				.withStyle(ChatFormatting.GREEN))
			.append(name.isEmpty() ? Component.empty() : namedComponent(name));
	}
}
