package com.thelastplayer.block;

import com.thelastplayer.story.StoryManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

/** The device in the final room that controls the reset. Starts the final scene. */
public class ResetCoreBlock extends Block {
    public ResetCoreBlock(Properties props) { super(props); }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
        if (!world.isClientSide && player instanceof ServerPlayerEntity) {
            StoryManager.onResetCoreUse((ServerPlayerEntity) player, pos);
        }
        return ActionResultType.sidedSuccess(world.isClientSide);
    }
}
