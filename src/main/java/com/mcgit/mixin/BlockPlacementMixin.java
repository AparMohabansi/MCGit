package com.mcgit.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockPlacementMixin {

    // Target method: setPlacedBy
    // Arguments MUST MATCH the vanilla signature exactly!
    @Inject(
            method = "setPlacedBy",
            at = @At("TAIL")
    )
    public void onSetPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        // We only care if a player placed it
        if (!level.isClientSide() && placer instanceof net.minecraft.world.entity.player.Player) {
            net.minecraft.world.entity.player.Player player = (net.minecraft.world.entity.player.Player) placer;

            String playerName = player.getName().getString();
            String blockName = state.getBlock().getName().getString();

            player.displayClientMessage(
                    Component.literal(playerName).append(" placed ")
                            .append(blockName)
                            .append(" at ")
                            .append(pos.toShortString()),
                    false
            );
        }
    }
}