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
import java.util.Map;

public class BlockIterator {

    public static void iterateRegion(Level level, BlockPos pos1, BlockPos pos2, Player player) {

        if (level.isClientSide()) {
            return; // Server-side only
        }

        // 1. Setup file handling
        // This saves to the root of your server or "run" folder
        File outputFile = new File("MCGit.txt");

        // Notify player
        player.displayClientMessage(Component.literal("Saving blocks to " + outputFile.getAbsolutePath() + "..."), false);

        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());

        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        // 2. Use try-with-resources to automatically close the writer
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            int count = 0;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {

                        BlockPos currentPos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(currentPos);

                        // SKIP AIR (Optional: Reduces file size significantly)
                        if (state.isAir()) {
                            continue;
                        }

                        // --- Capture ID ---
                        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

                        // --- Capture Properties ---
                        StringBuilder propertiesBuilder = new StringBuilder();
                        if (!state.getValues().isEmpty()) {
                            propertiesBuilder.append("[");
                            int propCount = 0;
                            for (Map.Entry<?, ?> entry : state.getValues().entrySet()) {
                                if (propCount > 0) propertiesBuilder.append(",");
                                // Cast to specific types to ensure correct .getName() access
                                // Using raw entry with toString() is usually safe for basic dumping
                                Property<?> property = (Property<?>) entry.getKey();
                                String key = property.getName();
                                String value = entry.getValue().toString();
                                // In official mappings, property keys are Property<?>, so we can assume logic holds
                                // But to be safe and simple with raw types:
                                propertiesBuilder.append(key).append("=").append(entry.getValue().toString());
                                propCount++;
                            }
                            propertiesBuilder.append("]");
                        }
                        String stateString = propertiesBuilder.toString();

                        // --- Capture NBT ---
                        String nbtString = "";
                        BlockEntity blockEntity = level.getBlockEntity(currentPos);
                        if (blockEntity != null) {
                            // For Fabric (Intermediary Mappings)
                            CompoundTag nbtTag = blockEntity.saveWithFullMetadata(level.registryAccess());
                            nbtString = nbtTag.toString();
                        }

                        // --- Format Line ---
                        // 10,64,-50|minecraft:chest[facing=north]{Items:...}
                        String line = x + "," + y + "," + z + "|" + blockId + stateString + nbtString;

                        // --- Write to File ---
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