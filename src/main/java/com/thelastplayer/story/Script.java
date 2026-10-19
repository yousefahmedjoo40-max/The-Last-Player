package com.thelastplayer.story;

import com.thelastplayer.network.EchoNetwork;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Builds a timed scene. Every call schedules its step immediately, "delay" ticks after the previous step.
 * Example: new Script(p).lock().say(20, Voice.HAPPY, "hi").say(60, Voice.SAD, "...").unlock(10);
 */
public final class Script {
    private final MinecraftServer server;
    private final UUID player;
    private int t = 0;

    public Script(ServerPlayerEntity p) {
        this.server = p.getServer();
        this.player = p.getUUID();
    }

    public Script run(int delay, Consumer<ServerPlayerEntity> action) {
        t += Math.max(0, delay);
        Scheduler.later(t, () -> {
            ServerPlayerEntity p = server.getPlayerList().getPlayer(player);
            if (p != null) action.accept(p);
        });
        return this;
    }

    public Script say(int delay, Voice v, String text) { return run(delay, p -> EchoVoice.say(p, v, text)); }
    public Script narrate(int delay, String text) { return run(delay, p -> EchoVoice.narrate(p, text)); }
    public Script system(int delay, String text) { return run(delay, p -> EchoVoice.system(p, text)); }
    public Script sound(int delay, Voice v) { return run(delay, p -> EchoVoice.playAtEcho(p, v)); }
    public Script title(int delay, String title, String sub) { return run(delay, p -> EchoVoice.title(p, title, sub, 10, 70, 25)); }
    public Script mood(int delay, int mood) { return run(delay, p -> StoryManager.setMood(p.getServer(), mood)); }
    public Script music(int delay, String track) { return run(delay, p -> { StoryManager.forceTrack(track); EchoNetwork.music(p, track); }); }

    /** Blocks AI chat replies while the scene plays. */
    public Script lock() { StoryManager.sceneLocked = true; return this; }
    public Script unlock(int delay) { return run(delay, p -> StoryManager.sceneLocked = false); }
}
