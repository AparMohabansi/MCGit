package com.mcgit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.mcgit.BlockIterator; // Import your iterator

public class BlockIteratorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("GitRegion")
                        // Define the 6 integer arguments for the two corners (x1, y1, z1, x2, y2, z2)
                        .then(Commands.argument("x1", IntegerArgumentType.integer())
                                .then(Commands.argument("y1", IntegerArgumentType.integer())
                                        .then(Commands.argument("z1", IntegerArgumentType.integer())
                                                .then(Commands.argument("x2", IntegerArgumentType.integer())
                                                        .then(Commands.argument("y2", IntegerArgumentType.integer())
                                                                .then(Commands.argument("z2", IntegerArgumentType.integer())

                                                                        // Execute the command
                                                                        .executes(BlockIteratorCommand::execute)
                                                                ))))))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {

        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        // 1. Extract coordinates from arguments
        int x1 = IntegerArgumentType.getInteger(context, "x1");
        int y1 = IntegerArgumentType.getInteger(context, "y1");
        int z1 = IntegerArgumentType.getInteger(context, "z1");

        int x2 = IntegerArgumentType.getInteger(context, "x2");
        int y2 = IntegerArgumentType.getInteger(context, "y2");
        int z2 = IntegerArgumentType.getInteger(context, "z2");

        // 2. Create BlockPos objects
        BlockPos pos1 = new BlockPos(x1, y1, z1);
        BlockPos pos2 = new BlockPos(x2, y2, z2);

        // 3. Invoke the iterator logic
        BlockIterator.iterateRegion(level, pos1, pos2, source.getPlayer());

        return 1;
    }
}