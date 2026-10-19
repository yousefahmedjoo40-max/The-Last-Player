package com.thelastplayer.registry;

import com.thelastplayer.TheLastPlayerMod;
import com.thelastplayer.block.EchoDeviceBlock;
import com.thelastplayer.block.LabTerminalBlock;
import com.thelastplayer.block.ResetCoreBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TheLastPlayerMod.MODID);

    // Story blocks are unbreakable in survival so the story can't be soft-locked.
    public static final RegistryObject<Block> ECHO_DEVICE = BLOCKS.register("echo_device",
            () -> new EchoDeviceBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.METAL).lightLevel(s -> 9)));

    public static final RegistryObject<Block> LAB_TERMINAL = BLOCKS.register("lab_terminal",
            () -> new LabTerminalBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.METAL).lightLevel(s -> 5)));

    public static final RegistryObject<Block> RESET_CORE = BLOCKS.register("reset_core",
            () -> new ResetCoreBlock(AbstractBlock.Properties.of(Material.METAL)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.METAL).lightLevel(s -> 12)));

    private ModBlocks() {}
}
