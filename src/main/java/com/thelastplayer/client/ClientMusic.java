package com.thelastplayer.client;

import com.thelastplayer.TheLastPlayerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

@OnlyIn(Dist.CLIENT)
public final class ClientMusic {
    private static ISound current;

    public static void play(String track) {
        stop();
        if (track == null || track.isEmpty()) return;
        SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(TheLastPlayerMod.MODID, "music_" + track));
        if (ev == null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.getMusicManager().stopPlaying();
        current = SimpleSound.forMusic(ev);
        mc.getSoundManager().play(current);
    }

    public static void stop() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }

    private ClientMusic() {}
}
