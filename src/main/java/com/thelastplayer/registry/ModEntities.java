package com.thelastplayer.registry;

import com.thelastplayer.TheLastPlayerMod;
import com.thelastplayer.entity.EchoEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, TheLastPlayerMod.MODID);

    public static final RegistryObject<EntityType<EchoEntity>> ECHO = ENTITIES.register("echo",
            () -> EntityType.Builder.<EchoEntity>of(EchoEntity::new, EntityClassification.CREATURE)
                    .sized(0.6F, 0.7F).clientTrackingRange(10).fireImmune()
                    .build(TheLastPlayerMod.MODID + ":echo"));

    private ModEntities() {}
}
