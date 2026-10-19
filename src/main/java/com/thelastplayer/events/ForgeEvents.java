package com.thelastplayer.events;

import com.thelastplayer.TheLastPlayerMod;
import com.thelastplayer.ai.AiChat;
import com.thelastplayer.command.EchoCommand;
import com.thelastplayer.entity.EchoEntity;
import com.thelastplayer.story.StoryData;
import com.thelastplayer.story.StoryManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = TheLastPlayerMod.MODID)
public final class ForgeEvents {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s != null && s.overworld() != null) StoryManager.tick(s);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getPlayer() instanceof ServerPlayerEntity) StoryManager.onPlayerJoin((ServerPlayerEntity) e.getPlayer());
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent e) {
        if (e.getPlayer() != null) StoryManager.onChat(e.getPlayer(), e.getMessage());
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        EchoCommand.register(e.getDispatcher());
    }

    /** only the "official" ECHO (the one in the save data) may exist */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent e) {
        if (e.getWorld().isClientSide) return;
        Entity en = e.getEntity();
        if (!(en instanceof EchoEntity)) return;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null || s.overworld() == null) return;
        StoryData d = StoryData.get(s);
        if (d.echo == null || !d.echo.equals(en.getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity p = (ServerPlayerEntity) e.getPlayer();
        if (e.getTo() == World.NETHER) StoryManager.memoryOnce(p.getServer(), "nether", "اللاعب أخذني إلى النيذر... مكان حار ومخيف");
        if (e.getTo() == World.END) StoryManager.memoryOnce(p.getServer(), "end", "اللاعب دخل الإند");
        StoryManager.relocateEcho(p);
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayerEntity)) return;
        Item it = e.getCrafting().getItem();
        if (it == Items.DIAMOND_PICKAXE || it == Items.DIAMOND_SWORD) {
            StoryManager.memoryOnce(((ServerPlayerEntity) e.getPlayer()).getServer(), "diamond", "اللاعب صنع أول أداة ألماس");
        }
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent e) {
        if (e.getPlayer() instanceof ServerPlayerEntity) {
            StoryManager.memoryOnce(((ServerPlayerEntity) e.getPlayer()).getServer(), "sleep", "اللاعب نام في سريره وانتظر الصباح");
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntityLiving() instanceof ServerPlayerEntity) {
            ServerPlayerEntity p = (ServerPlayerEntity) e.getEntityLiving();
            StoryManager.memoryOnce(p.getServer(), "death", "اللاعب مات مرة وأنا كنت خايف عليه");
        }
    }

    @SubscribeEvent
    public static void onServerStopped(FMLServerStoppedEvent e) {
        StoryManager.clearRuntime();
        AiChat.clearHistory();
    }

    private ForgeEvents() {}
}
