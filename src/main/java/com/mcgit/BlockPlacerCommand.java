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

public class BlockPlacerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("GitRestore")
                        .executes(BlockPlacerCommand::execute)
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {

        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        BlockPlacer.restoreFromFile(level,source.getPlayer());

        return 1;
    }
}