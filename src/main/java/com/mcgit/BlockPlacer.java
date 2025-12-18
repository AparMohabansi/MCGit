package com.mcgit;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public class BlockPlacer {

    public static void restoreFromFile(Level level, Player player) {
        if (level.isClientSide()) return;

        File inputFile = new File("MCGit.txt");
        if (!inputFile.exists()) {
            player.displayClientMessage(Component.literal("File not found: MCGit.txt"), false);
            return;
        }

        player.displayClientMessage(Component.literal("Restoring blocks from MCGit.txt..."), false);

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // 1. Split logic
                String[] parts = line.split("\\|", 2);
                if (parts.length < 2) continue;

                String[] coords = parts[0].split(",");
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int z = Integer.parseInt(coords[2]);
                BlockPos targetPos = new BlockPos(x, y, z);
                String blockDataPart = parts[1];

                try {
                    // --- FIX: Sanitize Block Data (Uppercase Enums) ---
                    // The error "does not accept 'SINGLE'" happens because the file has uppercase properties (e.g. type=SINGLE).
                    // We must convert the block state part (before NBT) to lowercase.
                    String parseableData = blockDataPart;
                    int nbtIndex = blockDataPart.indexOf('{');

                    if (nbtIndex != -1) {
                        // Lowercase only the block state part (minecraft:chest[type=SINGLE] -> minecraft:chest[type=single])
                        String statePart = blockDataPart.substring(0, nbtIndex).toLowerCase();
                        // Keep NBT part as is (case sensitive keys/values inside)
                        String nbtPart = blockDataPart.substring(nbtIndex);
                        parseableData = statePart + nbtPart;
                    } else {
                        // No NBT, safe to lowercase whole string
                        parseableData = blockDataPart.toLowerCase();
                    }

                    // 2. Parse Block Data
                    // strict=false helps avoid crashes on minor format mismatches
                    BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(
                            level.holderLookup(Registries.BLOCK),
                            parseableData, // Use the sanitized string
                            false
                    );

                    BlockState state = result.blockState();
                    CompoundTag nbt = result.nbt();

                    // 3. Place the Block first
                    // This creates the physical block AND a fresh, empty BlockEntity if applicable
                    level.setBlock(targetPos, state, 3);

                    // 4. Handle Block Entity / NBT
                    if (nbt != null) {
                        // Update position in NBT to match where we are placing it
                        nbt.putInt("x", x);
                        nbt.putInt("y", y);
                        nbt.putInt("z", z);

                        // Ensure ID is present (critical for some load implementations)
                        if (!nbt.contains("id")) {
                            nbt.putString("id", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
                        }

                        // --- FIX: Item NBT Correction (restored .orElse patterns) ---
                        if (nbt.contains("Items")) {
                            // Using .orElse() because your mappings return Optional<ListTag>
                            ListTag items = nbt.getList("Items").orElse(new ListTag());

                            for (int i = 0; i < items.size(); i++) {
                                // Using .orElse() because your mappings return Optional<CompoundTag>
                                CompoundTag itemTag = items.getCompound(i).orElse(new CompoundTag());

                                // Fix: 'count' (lowercase) -> 'Count' (uppercase Byte)
                                // Standard Minecraft requires "Count" to be a Byte, parsers often give Int
                                if (itemTag.contains("count") && !itemTag.contains("Count")) {
                                    int rawVal = itemTag.getInt("count").orElse(0);
                                    itemTag.putByte("Count", (byte) rawVal);
                                }

                                // Fix: Ensure Slot is a Byte
                                if (itemTag.contains("Slot")) {
                                    int slotVal = itemTag.getInt("Slot").orElse(0);
                                    itemTag.putByte("Slot", (byte) slotVal);
                                }
                            }
                        }

                        // --- CRITICAL FIX: Robust Loading Logic ---
                        BlockEntity existingBe = level.getBlockEntity(targetPos);

                        if (existingBe != null) {
                            boolean loaded = false;
                            try {
                                // Try 1.20.5+ method first: loadWithComponents
                                Method loadMethod = BlockEntity.class.getMethod("loadWithComponents", CompoundTag.class, net.minecraft.core.HolderLookup.Provider.class);
                                loadMethod.invoke(existingBe, nbt, level.registryAccess());
                                loaded = true;
                            } catch (NoSuchMethodException | SecurityException e1) {
                                // Ignore, try fallback
                            }

                            if (!loaded) {
                                try {
                                    // Fallback to 1.20.4 and older: load(CompoundTag)
                                    Method loadMethod = BlockEntity.class.getMethod("load", CompoundTag.class);
                                    loadMethod.invoke(existingBe, nbt);
                                    loaded = true;
                                } catch (NoSuchMethodException e2) {
                                    player.displayClientMessage(Component.literal("MCGit Warning: Could not find 'load' method for " + state.getBlock().getName().getString()), false);
                                }
                            }

                            if (loaded) {
                                // Mark dirty so the game knows to save it to disk
                                existingBe.setChanged();
                                // Force a chunk update so clients see the changes immediately
                                level.sendBlockUpdated(targetPos, state, state, 3);
                            }
                        } else {
                            player.displayClientMessage(Component.literal("MCGit Warning: BlockEntity missing for " + state.getBlock().getName().getString() + " at " + targetPos), false);
                        }
                    }

                    count++;

                } catch (CommandSyntaxException e) {
                    player.displayClientMessage(Component.literal("MCGit Parse Error at " + x + "," + y + "," + z + ": " + e.getMessage()), false);
                } catch (Exception e) {
                    // CATCH ALL Exceptions so the loop continues even if one block fails
                    player.displayClientMessage(Component.literal("MCGit Error at " + x + "," + y + "," + z + ": " + e.getMessage()), false);
                    e.printStackTrace();
                }
            }

            player.displayClientMessage(Component.literal("Restoration complete! Placed " + count + " blocks."), false);

        } catch (IOException e) {
            e.printStackTrace();
            player.displayClientMessage(Component.literal("Error reading file."), false);
        }
    }
}