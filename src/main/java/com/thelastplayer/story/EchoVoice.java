package com.thelastplayer.story;

import com.thelastplayer.entity.EchoEntity;
import com.thelastplayer.network.EchoNetwork;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.Random;

/** Chat lines, sounds, titles. */
public final class EchoVoice {
    private static final Random RNG = new Random();

    public static void say(ServerPlayerEntity p, Voice v, String text) {
        String shown = v == Voice.BREAKDOWN ? glitch(text) : text;
        IFormattableTextComponent body = new StringTextComponent(shown).withStyle(v.color);
        if (v.italic) body = body.withStyle(TextFormatting.ITALIC);
        IFormattableTextComponent msg = new StringTextComponent("[ECHO] ").withStyle(TextFormatting.AQUA).append(body);
        p.sendMessage(msg, Util.NIL_UUID);
        playAtEcho(p, v);
    }

    /** gray italic stage direction, e.g. "* ECHO looks at the sky *" */
    public static void narrate(ServerPlayerEntity p, String text) {
        p.sendMessage(new StringTextComponent(text).withStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC), Util.NIL_UUID);
    }

    /** terminal-style system line */
    public static void system(ServerPlayerEntity p, String text) {
        p.sendMessage(new StringTextComponent("[SYSTEM] ").withStyle(TextFormatting.DARK_RED)
                .append(new StringTextComponent(text).withStyle(TextFormatting.GRAY)), Util.NIL_UUID);
    }

    public static void title(ServerPlayerEntity p, String title, String sub, int in, int stay, int out) {
        p.connection.send(new STitlePacket(in, stay, out));
        p.connection.send(new STitlePacket(STitlePacket.Type.TITLE, new StringTextComponent(title)));
        if (sub != null && !sub.isEmpty()) {
            p.connection.send(new STitlePacket(STitlePacket.Type.SUBTITLE, new StringTextComponent(sub)));
        }
    }

    public static void playAtEcho(ServerPlayerEntity p, Voice v) {
        if (v == null || v.sound == null) return;
        MinecraftServer s = p.getServer();
        EchoEntity e = StoryManager.findEcho(s);
        World w = p.level;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        if (e != null && e.level == p.level) { x = e.getX(); y = e.getY(); z = e.getZ(); }
        SoundEvent ev = v.sound.get();
        w.playSound(null, x, y, z, ev, SoundCategory.NEUTRAL, 1.0F, 0.95F + RNG.nextFloat() * 0.1F);
    }

    public static void playSound(ServerPlayerEntity p, SoundEvent ev, float vol, float pitch) {
        p.level.playSound(null, p.getX(), p.getY(), p.getZ(), ev, SoundCategory.AMBIENT, vol, pitch);
    }

    public static void music(ServerPlayerEntity p, String track) {
        EchoNetwork.music(p, track);
    }

    /** stutter / corrupt text for the breakdown scenes */
    static String glitch(String s) {
        Random r = new Random(s.hashCode());
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            b.append(c);
            if (c != ' ' && r.nextInt(13) == 0) b.append('-').append(c);
        }
        return b.toString();
    }

    private EchoVoice() {}
}
