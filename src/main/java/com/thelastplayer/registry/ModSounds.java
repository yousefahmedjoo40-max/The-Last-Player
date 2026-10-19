package com.thelastplayer.registry;

import com.thelastplayer.TheLastPlayerMod;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TheLastPlayerMod.MODID);

    public static final RegistryObject<SoundEvent> ECHO_HAPPY = reg("echo_happy");
    public static final RegistryObject<SoundEvent> ECHO_LAUGH = reg("echo_laugh");
    public static final RegistryObject<SoundEvent> ECHO_CURIOUS = reg("echo_curious");
    public static final RegistryObject<SoundEvent> ECHO_SAD = reg("echo_sad");
    public static final RegistryObject<SoundEvent> ECHO_FEAR = reg("echo_fear");
    public static final RegistryObject<SoundEvent> ECHO_BREAKDOWN = reg("echo_breakdown");
    public static final RegistryObject<SoundEvent> ECHO_GLITCH = reg("echo_glitch");
    public static final RegistryObject<SoundEvent> ECHO_WHISPER = reg("echo_whisper");
    public static final RegistryObject<SoundEvent> AMBIENT_HUM = reg("ambient_hum");

    public static final RegistryObject<SoundEvent> MUSIC_CALM = reg("music_calm");
    public static final RegistryObject<SoundEvent> MUSIC_UNEASY = reg("music_uneasy");
    public static final RegistryObject<SoundEvent> MUSIC_DREAD = reg("music_dread");
    public static final RegistryObject<SoundEvent> MUSIC_SORROW = reg("music_sorrow");
    public static final RegistryObject<SoundEvent> MUSIC_FINAL = reg("music_final");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(TheLastPlayerMod.MODID, name)));
    }

    private ModSounds() {}
}
