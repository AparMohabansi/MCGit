package com.mcgit;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class ItemPlacedHandler {
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide()) {
                ItemStack stack = player.getItemInHand(hand);

                if (stack.getItem() instanceof BlockItem) {

                    // 1. Get the block we clicked on
                    BlockPos clickedPos = hitResult.getBlockPos();

                    // 2. Get the face (side) we clicked
                    Direction face = hitResult.getDirection();

                    // 3. Calculate the EMPTY spot next to it
                    // "relative" adds 1 to the coordinate in that direction
                    BlockPos placePos = clickedPos.relative(face);

                    // 4. Format the message
                    String coordString = "(" + placePos.getX() + ", " + placePos.getY() + ", " + placePos.getZ() + ")";

                    player.displayClientMessage(
                            Component.literal("You placed: ")
                                    .append(stack.getHoverName())
                                    .append(" at ")
                                    .append(coordString),
                            false
                    );
                }
            }
            return InteractionResult.PASS;
        });
    }
}