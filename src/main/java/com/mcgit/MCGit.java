package com.mcgit;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult; // <--- This was ActionResult
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;  // <--- This was Text
import net.minecraft.world.level.Level;       // <--- This was World
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCGit implements ModInitializer {
	public static final String MOD_ID = "mcgit";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClientSide()) {
				ItemStack stack = player.getItemInHand(hand);
				if (stack.getItem() instanceof BlockItem) {
					player.displayClientMessage(Component.literal("You placed: ").append(stack.getHoverName()),false);
				}
			}
			return InteractionResult.PASS;
		});
	}
}