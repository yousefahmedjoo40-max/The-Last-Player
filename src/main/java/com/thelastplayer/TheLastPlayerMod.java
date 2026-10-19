package com.thelastplayer;

import com.thelastplayer.config.EchoConfig;
import com.thelastplayer.entity.EchoEntity;
import com.thelastplayer.network.EchoNetwork;
import com.thelastplayer.registry.ModBlocks;
import com.thelastplayer.registry.ModEntities;
import com.thelastplayer.registry.ModItems;
import com.thelastplayer.registry.ModSounds;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TheLastPlayerMod.MODID)
public class TheLastPlayerMod {
    public static final String MODID = "thelastplayer";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public TheLastPlayerMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EchoConfig.SPEC, "thelastplayer-common.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            GlobalEntityTypeAttributes.put(ModEntities.ECHO.get(), EchoEntity.createAttributes().build());
            EchoNetwork.register();
        });
    }
}
