package com.mcgit;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;

public class ItemBrokeHandler {
    public static void register() {
        // Note: 'world' is now 'level', 'player' is 'Player' (not PlayerEntity)
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!level.isClientSide()) { // Mojang mapping: isClientSide
                String playerName = player.getName().getString();
                // Mojang mapping: getName() returns Component, getString() gets text
                String blockName = state.getBlock().getName().getString();
                String coordinates = pos.toShortString();

                player.displayClientMessage(
                        Component.literal(playerName).append(" broke ")
                                .append(blockName)
                                .append(" at ")
                                .append(coordinates),
                        false
                );
            }
        });
    }
}
