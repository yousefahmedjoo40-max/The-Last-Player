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

/** The strange device in the hidden underground room. Right-click to wake ECHO. */
public class EchoDeviceBlock extends Block {
    public EchoDeviceBlock(Properties props) { super(props); }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                Hand hand, BlockRayTraceResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResultType.PASS;
        if (!world.isClientSide && player instanceof ServerPlayerEntity) {
            StoryManager.onDeviceUse((ServerPlayerEntity) player, pos);
        }
        return ActionResultType.sidedSuccess(world.isClientSide);
    }
}
