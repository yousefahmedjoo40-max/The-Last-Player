package com.thelastplayer.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** A story item with a tooltip that prints its text in chat on right-click. */
public class ReadableItem extends Item {
    private final String tooltipKey;
    private final String[] text;

    public ReadableItem(Properties props, String tooltipKey, String[] text) {
        super(props);
        this.tooltipKey = tooltipKey;
        this.text = text;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent(tooltipKey).withStyle(TextFormatting.GRAY, TextFormatting.ITALIC));
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide && player instanceof ServerPlayerEntity && text != null) {
            for (String line : text) {
                player.sendMessage(new StringTextComponent(line).withStyle(TextFormatting.GRAY), Util.NIL_UUID);
            }
        }
        return ActionResult.sidedSuccess(stack, world.isClientSide);
    }
}
