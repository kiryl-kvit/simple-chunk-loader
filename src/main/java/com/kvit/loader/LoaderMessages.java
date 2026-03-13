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
		return Component.empty()
			.append(Component.literal(name.isEmpty() ? "Cleared loader name" : "Renamed loader to ")
				.withStyle(ChatFormatting.GREEN))
			.append(name.isEmpty() ? Component.empty() : namedComponent(name));
	}
}
