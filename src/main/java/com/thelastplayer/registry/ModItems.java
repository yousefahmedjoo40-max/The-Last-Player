package com.thelastplayer.registry;

import com.thelastplayer.TheLastPlayerMod;
import com.thelastplayer.item.ReadableItem;
import com.thelastplayer.story.StoryScript;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TheLastPlayerMod.MODID);

    public static final RegistryObject<Item> BLUE_GLASS_SHARD = ITEMS.register("blue_glass_shard",
            () -> new ReadableItem(new Item.Properties().tab(ItemGroup.TAB_MISC).stacksTo(1),
                    "tooltip.thelastplayer.blue_glass_shard", StoryScript.SHARD_TEXT));

    public static final RegistryObject<Item> OLD_LETTER = ITEMS.register("old_letter",
            () -> new ReadableItem(new Item.Properties().tab(ItemGroup.TAB_MISC).stacksTo(1),
                    "tooltip.thelastplayer.old_letter", StoryScript.LETTER_TEXT));

    public static final RegistryObject<Item> MEMORY_LOG = ITEMS.register("memory_log",
            () -> new ReadableItem(new Item.Properties().tab(ItemGroup.TAB_MISC).stacksTo(1),
                    "tooltip.thelastplayer.memory_log", StoryScript.MEMORY_TEXT));

    public static final RegistryObject<Item> ECHO_DEVICE = blockItem("echo_device", ModBlocks.ECHO_DEVICE);
    public static final RegistryObject<Item> LAB_TERMINAL = blockItem("lab_terminal", ModBlocks.LAB_TERMINAL);
    public static final RegistryObject<Item> RESET_CORE = blockItem("reset_core", ModBlocks.RESET_CORE);

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().tab(ItemGroup.TAB_DECORATIONS)));
    }

    private ModItems() {}
}
