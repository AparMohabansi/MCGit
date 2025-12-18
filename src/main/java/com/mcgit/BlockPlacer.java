package com.mcgit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Optional;

public class BlockPlacer {

    public static void restoreFromFile(Level level, Player player) {
        if (level.isClientSide()) return;

        File inputFile = new File("MCGit.txt");
        if (!inputFile.exists()) {
            player.displayClientMessage(Component.literal("File not found: " + inputFile.getAbsolutePath()), false);
            return;
        }

        player.displayClientMessage(Component.literal("Restoring blocks from " + inputFile.getAbsolutePath() + "..."), false);

        int count = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                try {
                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 2) continue;

                    // 1. Parse Coordinates
                    String[] coords = parts[0].split(",");
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int z = Integer.parseInt(coords[2]);
                    BlockPos pos = new BlockPos(x, y, z);

                    // 2. Parse Block ID
                    String blockId = parts[1];

                    // --- MAPPING CHECK ---
                    // If 'ResourceLocation' is red, you are likely using Fabric YARN mappings.
                    // REPLACE the lines below with:
                     net.minecraft.resources.Identifier resLoc = net.minecraft.resources.Identifier.tryParse(blockId);

                    Block block = BuiltInRegistries.BLOCK.get(resLoc)
                            .map(Holder::value)
                            .orElse(Blocks.AIR);

                    if (block == Blocks.AIR && !blockId.equals("minecraft:air")) {
                        continue;
                    }

                    // 3. Parse Properties
                    BlockState state = block.defaultBlockState();
                    String propertiesRaw = parts.length > 2 ? parts[2] : "";

                    if (!propertiesRaw.isEmpty()) {
                        String[] props = propertiesRaw.split(",");
                        for (String propPair : props) {
                            String[] kv = propPair.split("=");
                            if (kv.length == 2) {
                                String key = kv[0];
                                String value = kv[1];
                                Property<?> property = block.getStateDefinition().getProperty(key);
                                if (property != null) {
                                    state = setValueHelper(state, property, value);
                                }
                            }
                        }
                    }

                    // 4. Set the Block
                    level.setBlock(pos, state, 3);

                    // 5. Parse and Apply NBT
                    String nbtRaw = parts.length > 3 ? parts[3] : "";
                    if (!nbtRaw.isEmpty()) {
                        String nbtClean = nbtRaw.replace("\\n", "\n");
                        CompoundTag tag = TagParser.parseCompoundFully(nbtClean);

                        // Inject correct position into NBT
                        tag.putInt("x", pos.getX());
                        tag.putInt("y", pos.getY());
                        tag.putInt("z", pos.getZ());

                        // FIX: Use static factory method 'loadStatic'.
                        // This bypasses the protected 'load' methods and incompatible 'loadWithComponents'.
                        // loadStatic is the standard way the game creates BEs from disk.
                        BlockEntity newBe = BlockEntity.loadStatic(pos, state, tag, level.registryAccess());

                        if (newBe != null) {
                            // Force our newly created, fully-loaded BlockEntity into the level
                            level.setBlockEntity(newBe);
                        }
                    }

                    count++;

                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line);
                    e.printStackTrace();
                }
            }
            player.displayClientMessage(Component.literal("Restoration Complete. Restored " + count + " blocks."), false);

        } catch (Exception e) {
            e.printStackTrace();
            player.displayClientMessage(Component.literal("Error reading file: " + e.getMessage()), false);
        }
    }

    private static <T extends Comparable<T>> BlockState setValueHelper(BlockState state, Property<T> property, String value) {
        Optional<T> parsedValue = property.getValue(value);
        return parsedValue.map(t -> state.setValue(property, t)).orElse(state);
    }
}