package com.mcgit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockIterator {

    /**
     * Iterates through all blocks in the bounding box defined by pos1 and pos2.
     * @param level The Level (World) object to read the blocks from.
     * @param pos1 The first corner of the region (e.g., lower-X, lower-Y, lower-Z).
     * @param pos2 The second corner of the region (e.g., higher-X, higher-Y, higher-Z).
     */
    public static void iterateRegion(Level level, BlockPos pos1, BlockPos pos2, Player player) {

        if (level.isClientSide()) {
            // Only run this logic on the server!
            return;
        }

        // 1. Determine the Min and Max coordinates for safe iteration
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());

        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        System.out.println("Starting iteration over " + totalBlocks + " blocks...");

        // 2. Perform the triple-nested iteration
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {

                    // Create a BlockPos object for the current coordinate
                    BlockPos currentPos = new BlockPos(x, y, z);

                    // Get the BlockState at that position
                    BlockState state = level.getBlockState(currentPos);

                    // Get the block's registry name (e.g., minecraft:stone)
                    String blockId = state.getBlock().toString();

                    // 3. Print the result

                    player.displayClientMessage(
                            Component.literal("Block at ").append(String.valueOf(x))
                                    .append(", ")
                                    .append(String.valueOf(y))
                                    .append(", ")
                                    .append(String.valueOf(z))
                                    .append("): ")
                                    .append(blockId),
                            false
                    );

                    // Optional: You can also use the BlockState to get properties
                    // System.out.println("State: " + state.toString());
                }
            }
        }

        System.out.println("Iteration finished.");
    }
}