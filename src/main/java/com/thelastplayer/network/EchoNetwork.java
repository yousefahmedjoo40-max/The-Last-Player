package com.thelastplayer.network;

import com.thelastplayer.TheLastPlayerMod;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class EchoNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TheLastPlayerMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, MusicPacket.class, MusicPacket::encode, MusicPacket::decode, MusicPacket::handle);
    }

    /** track = "calm", "uneasy", "dread", "sorrow", "final" or "" to stop. */
    public static void music(ServerPlayerEntity player, String track) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MusicPacket(track));
    }

    private EchoNetwork() {}
}
