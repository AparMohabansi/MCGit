package com.mcgit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

public class BlockIterator {

    public static void iterateRegion(Level level, BlockPos pos1, BlockPos pos2, Player player) {

        if (level.isClientSide()) {
            return;
        }

        File outputFile = new File("MCGit.txt");
        player.displayClientMessage(Component.literal("Saving absolute blocks to " + outputFile.getAbsolutePath() + "..."), false);

        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());

        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            int count = 0;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {

                        BlockPos currentPos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(currentPos);

                        if (state.isAir()) continue;

                        // 1. Get Block ID
                        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

                        // 2. Get Properties (Comma separated: "facing=north,waterlogged=false")
                        // We do NOT add brackets [] so it's easier to parse manually in Java later.
                        String properties = "";
                        if (!state.getValues().isEmpty()) {
                            properties = state.getValues().entrySet().stream()
                                    .map(entry -> {
                                        Property<?> property = (Property<?>) entry.getKey();
                                        return property.getName() + "=" + entry.getValue().toString();
                                    })
                                    .collect(Collectors.joining(","));
                        }

                        // 3. Get NBT
                        String nbtString = "";
                        BlockEntity blockEntity = level.getBlockEntity(currentPos);
                        if (blockEntity != null) {
                            CompoundTag nbtTag = blockEntity.saveWithFullMetadata(level.registryAccess());
                            // It is still good practice to remove internal coords from the NBT data itself,
                            // even when using absolute placement, to ensure the TileEntity accepts its new/current location.
                            nbtTag.remove("x");
                            nbtTag.remove("y");
                            nbtTag.remove("z");
                            nbtString = nbtTag.toString();

                            // Sanitize newlines to prevent file corruption
                            nbtString = nbtString.replace("\n", "\\n").replace("\r", "");
                        }

                        // 4. Format: Absolute Coords | ID | Properties | NBT
                        // Separated by pipes "|" for easy String.split("|")
                        String line = currentPos.getX() + "," + currentPos.getY() + "," + currentPos.getZ() + "|" +
                                blockId + "|" +
                                properties + "|" +
                                nbtString;

                        writer.write(line);
                        writer.newLine();

                        count++;
                    }
                }
            }

            player.displayClientMessage(Component.literal("Success! Saved " + count + " blocks."), false);

        } catch (IOException e) {
            e.printStackTrace();
            player.displayClientMessage(Component.literal("Error saving file: " + e.getMessage()), false);
        }
    }
}