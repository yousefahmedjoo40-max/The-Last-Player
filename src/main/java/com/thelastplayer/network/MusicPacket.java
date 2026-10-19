package com.thelastplayer.network;

import com.thelastplayer.client.ClientMusic;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MusicPacket {
    private final String track;

    public MusicPacket(String track) {
        this.track = track == null ? "" : track;
    }

    public static void encode(MusicPacket msg, PacketBuffer buf) {
        buf.writeUtf(msg.track);
    }

    public static MusicPacket decode(PacketBuffer buf) {
        return new MusicPacket(buf.readUtf(64));
    }

    public static void handle(MusicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientMusic.play(msg.track)));
        ctx.get().setPacketHandled(true);
    }
}
