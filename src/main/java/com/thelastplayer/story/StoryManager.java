package com.thelastplayer.story;

import com.thelastplayer.TheLastPlayerMod;
import com.thelastplayer.ai.AiChat;
import com.thelastplayer.config.EchoConfig;
import com.thelastplayer.entity.EchoEntity;
import com.thelastplayer.network.EchoNetwork;
import com.thelastplayer.registry.ModEntities;
import com.thelastplayer.registry.ModItems;
import com.thelastplayer.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ClickEvent;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** The story brain: stages, days, scripted scenes, choices, endings. */
public final class StoryManager {
    public static final int S_NONE = 0, S_DAY1 = 1, S_DAY2 = 2, S_LAB_BUILT = 3, S_LAB_FOUND = 4,
            S_DAY4_DONE = 5, S_FINAL = 6, S_ENDED = 7;

    /** true while a scripted scene plays (AI chat is muted so it can't break the story) */
    public static volatile boolean sceneLocked = false;

    private static final Random RNG = new Random();

    private static final class Pending {
        final String id;
        final Map<String, String> options;
        final long expires;   // game time, 0 = never
        Pending(String id, Map<String, String> options, long expires) {
            this.id = id; this.options = options; this.expires = expires;
        }
    }

    private static Pending pending;
    private static int ticks;
    private static int musicTicks;
    private static String currentTrack = "";
    private static int nightSit, giftTimer, tipTimer, ambientTimer;

    // =====================================================================================
    //  lifecycle
    // =====================================================================================
    public static void clearRuntime() {
        pending = null;
        sceneLocked = false;
        currentTrack = "";
        musicTicks = 0;
        Scheduler.clear();
    }

    public static void tick(MinecraftServer s) {
        Scheduler.tick();
        if (++ticks % 20 != 0) return;
        StoryData d = StoryData.get(s);
        if (!d.started) return;
        ServerPlayerEntity p = s.getPlayerList().getPlayer(d.owner);
        if (p == null) return;
        if (pending != null && pending.expires > 0 && s.overworld().getGameTime() > pending.expires) {
            Pending pc = pending;
            pending = null;
            onChoiceTimeout(p, pc.id);
        }
        periodic(s, d, p);
    }

    public static int currentDay(MinecraftServer s, StoryData d) {
        return (int) (s.overworld().getDayTime() / 24000L - d.startDayIndex) + 1;
    }

    private static void periodic(MinecraftServer s, StoryData d, ServerPlayerEntity p) {
        int day = currentDay(s, d);
        long tod = s.overworld().getDayTime() % 24000L;
        scanEvidence(p, d);
        updateMusic(p, d);

        EchoEntity echo = findEcho(s);
        boolean near = echo != null && echo.level == p.level && echo.distanceToSqr(p) < 100.0D;

        if (echoShouldExist(d) && echo == null && !sceneLocked) ensureEcho(p, d);

        if ((d.stage == S_DAY1 || d.stage == S_DAY2) && near && tod >= 13000L && tod <= 23000L) {
            nightSit += 20;
            if (nightSit >= 3000) memoryOnce(s, "nightsit", "اللاعب جلس معي طوال الليل");
        }

        switch (d.stage) {
            case S_DAY1:
                if (near && !sceneLocked) day1Tick(p, d, tod);
                if (day >= 2 && !sceneLocked) startDay2(p, d);
                break;
            case S_DAY2:
                ambientTimer += 20;
                if (ambientTimer >= 3600 && !sceneLocked) {
                    ambientTimer = 0;
                    EchoVoice.playSound(p, ModSounds.ECHO_GLITCH.get(), 0.5F, 0.8F);
                    p.displayClientMessage(new StringTextComponent("...إشارة...").withStyle(TextFormatting.DARK_GRAY, TextFormatting.ITALIC), true);
                }
                if (day >= 3 && !sceneLocked) startDay3(p, d);
                break;
            case S_LAB_BUILT:
                if (d.labOrigin != null && p.level.dimension() == World.OVERWORLD
                        && LabBuilder.insideHall(p.blockPosition(), d.labOrigin)) labFound(p, d);
                break;
            case S_LAB_FOUND:
                if (day >= 4 && !sceneLocked && p.level.dimension() == World.OVERWORLD) {
                    BlockPos home = home(p, d);
                    double dx = p.getX() - home.getX(), dz = p.getZ() - home.getZ();
                    if (dx * dx + dz * dz < 400.0D) {
                        startDay4(p, d, home);
                    } else if (!d.has("home_hint")) {
                        d.flag("home_hint");
                        EchoVoice.system(p, "شيء ما ينتظرك في المنزل. استخدم /echo compass");
                    }
                }
                break;
            case S_ENDED:
                if ("lost".equals(d.ending) && !d.has("lost_whisper") && d.devicePos != null
                        && p.level.dimension() == World.OVERWORLD
                        && p.blockPosition().distSqr(d.devicePos) < 16.0D) {
                    d.flag("lost_whisper");
                    new Script(p).sound(0, Voice.GLITCH).say(40, Voice.WHISPER, StoryScript.LOST_WHISPER)
                            .sound(50, Voice.GLITCH)
                            .run(5, pl -> pl.displayClientMessage(new StringTextComponent("§8...§k|||§8..."), true));
                }
                break;
            default:
                break;
        }
    }

    private static void day1Tick(ServerPlayerEntity p, StoryData d, long tod) {
        if (EchoConfig.GIFTS.get() && d.gifts < 5) {
            giftTimer += 20;
            if (giftTimer >= 2400) {
                giftTimer = 0;
                giveGift(p, d);
            }
        }
        tipTimer += 20;
        if (tipTimer >= 3000) {
            tipTimer = 0;
            EchoVoice.say(p, Voice.HAPPY, StoryScript.DAY1_TIPS[RNG.nextInt(StoryScript.DAY1_TIPS.length)]);
        }
        if (!d.has("alone_asked") && tod >= 12500L && pending == null) {
            d.flag("alone_asked");
            new Script(p).lock()
                    .say(10, Voice.CURIOUS, "الشمس تغيب... يوم حلو، صح؟")
                    .say(80, Voice.CURIOUS, StoryScript.ALONE_Q)
                    .run(20, pl -> offer(pl, "alone", 0,
                            "alone", "[ نعم، دائمًا وحدي ]", "notalone", "[ لا، أحيانًا مع أصدقاء ]"))
                    .unlock(0);
        }
    }

    private static void giveGift(ServerPlayerEntity p, StoryData d) {
        Item[] items = {Items.BREAD, Items.TORCH, Items.IRON_INGOT, Items.COOKED_BEEF, Items.COAL};
        int[] counts = {3, 16, 4, 4, 8};
        int i = d.gifts % items.length;
        d.gifts++;
        d.setDirty();
        giveOrDrop(p, new ItemStack(items[i], counts[i]));
        EchoVoice.say(p, Voice.LAUGH, StoryScript.GIFT_LINES[i % StoryScript.GIFT_LINES.length]
                .replace("{name}", p.getName().getString()));
    }

    // =====================================================================================
    //  start: first join + the device
    // =====================================================================================
    public static void onPlayerJoin(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        StoryData d = StoryData.get(s);
        if (d.roomBuilt) return;
        d.roomBuilt = true;
        d.setDirty();
        Scheduler.later(100, () -> {
            ServerPlayerEntity pl = s.getPlayerList().getPlayer(p.getUUID());
            if (pl == null) return;
            ServerWorld w = s.overworld();
            BlockPos spot = LabBuilder.pickSpot(w, pl.blockPosition(), 25, 40);
            BlockPos device = LabBuilder.buildSecretRoom(w, spot.getX(), spot.getZ());
            d.devicePos = device;
            d.setDirty();
            giveOrDrop(pl, StoryItems.signalBook(device));
            giveOrDrop(pl, StoryItems.compass(device, "إشارة غريبة"));
            EchoVoice.system(pl, "إشارة غريبة التُقطت من تحت الأرض. " + StoryItems.coords(device));
        });
    }

    public static void onDeviceUse(ServerPlayerEntity p, BlockPos pos) {
        MinecraftServer s = p.getServer();
        StoryData d = StoryData.get(s);
        if (d.started) {
            if (d.stage == S_DAY1 || d.stage == S_DAY2) {
                if (findEcho(s) == null) ensureEcho(p, d);
                else EchoVoice.narrate(p, "* الجهاز يهمس بهدوء. إيكو معك بالفعل. *");
            } else {
                EchoVoice.narrate(p, "* الجهاز صامت. *");
            }
            return;
        }
        d.started = true;
        d.owner = p.getUUID();
        d.devicePos = pos;
        d.startDayIndex = s.overworld().getDayTime() / 24000L;
        d.stage = S_DAY1;
        d.mood = 0;
        d.ending = "";
        d.setDirty();
        giftTimer = 0; tipTimer = 0; ambientTimer = 0; nightSit = 0;

        giveOrDrop(p, new ItemStack(ModItems.BLUE_GLASS_SHARD.get()));
        d.flag("has_shard");

        ServerWorld w = (ServerWorld) p.level;
        spawnEcho(w, d, p, pos.getX() + 0.5D, pos.getY() + 1.8D, pos.getZ() + 0.5D);
        String name = p.getName().getString();
        new Script(p).lock()
                .sound(5, Voice.GLITCH)
                .narrate(15, "* يهتز الجهاز... وينبثق منه كائن زجاجي صغير يطفو في الهواء، وفي صدره ضوء يشبه القلب. *")
                .say(50, Voice.LAUGH, StoryScript.INTRO_1)
                .say(110, Voice.HAPPY, StoryScript.INTRO_2.replace("{name}", name))
                .say(100, Voice.CURIOUS, StoryScript.INTRO_3)
                .run(20, pl -> offer(pl, "welcome", 0,
                        "stay", "[ نعم، ابقَ معي ]", "who", "[ من أنت بالضبط؟ ]"))
                .unlock(0);
    }

    // =====================================================================================
    //  choices
    // =====================================================================================
    public static void offer(ServerPlayerEntity p, String id, int timeoutTicks, String... keyLabel) {
        Map<String, String> opts = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyLabel.length; i += 2) opts.put(keyLabel[i], keyLabel[i + 1]);
        long exp = timeoutTicks > 0 ? p.getServer().overworld().getGameTime() + timeoutTicks : 0L;
        pending = new Pending(id, opts, exp);
        for (Map.Entry<String, String> e : opts.entrySet()) {
            IFormattableTextComponent c = new StringTextComponent("  " + e.getValue())
                    .setStyle(Style.EMPTY.withColor(TextFormatting.YELLOW)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/echo choice " + e.getKey())));
            p.sendMessage(c, Util.NIL_UUID);
        }
    }

    public static void choose(ServerPlayerEntity p, String option) {
        StoryData d = StoryData.get(p.getServer());
        if (!d.started || !p.getUUID().equals(d.owner)) return;
        Pending pc = pending;
        if (pc == null || !pc.options.containsKey(option)) {
            EchoVoice.system(p, "لا يوجد خيار نشط الآن.");
            return;
        }
        pending = null;
        p.sendMessage(new StringTextComponent("> " + pc.options.get(option)).withStyle(TextFormatting.GRAY), Util.NIL_UUID);
        onChoice(p, d, pc.id, option);
    }

    private static void onChoiceTimeout(ServerPlayerEntity p, String id) {
        if ("protocol".equals(id)) endLost(p, "stand");
    }

    private static void onChoice(ServerPlayerEntity p, StoryData d, String id, String opt) {
        MinecraftServer s = p.getServer();
        String name = p.getName().getString();
        switch (id) {
            case "welcome":
                if ("stay".equals(opt)) {
                    d.flag("welcomed");
                    memoryOnce(s, "welcome", "اللاعب سمح لي بالبقاء معه");
                    new Script(p).lock()
                            .say(20, Voice.LAUGH, "حقًا؟! يا سلام! ... أنا وعدتك: ما راح أزعجك. ما راح أزعجك كثير!")
                            .say(90, Voice.HAPPY, "يلا يلا، خلنا نشتغل! أنا أعرف أماكن حلوة، وأنت تعرف الباقي. فريق!")
                            .unlock(0);
                } else {
                    d.flag("suspicious_start");
                    new Script(p).lock()
                            .say(20, Voice.CURIOUS, "سؤال ممتاز! أنا ECHO. مساعد... أظن. ما أتذكر من صنعني.")
                            .say(90, Voice.HAPPY, "المهم إني أقدر أساعد! جرّبني. ... وبقعد معك، صح؟ شكرًا!")
                            .unlock(0);
                }
                d.setDirty();
                break;
            case "alone":
                if ("alone".equals(opt)) {
                    d.flag("alone");
                    memoryOnce(s, "alone", "اللاعب قال إنه يلعب وحده دائمًا");
                    new Script(p).lock().say(30, Voice.CURIOUS, StoryScript.ALONE_YES_REPLY).unlock(0);
                } else {
                    d.flag("notalone");
                    new Script(p).lock().say(30, Voice.SAD, StoryScript.ALONE_NO_REPLY).unlock(0);
                }
                break;
            case "newworld":
                d.flag("newworld_" + opt);
                if ("promise".equals(opt)) {
                    memoryOnce(s, "promise", "اللاعب وعدني أنه سيأخذني معه لو صنع عالمًا جديدًا");
                    new Script(p).lock().say(30, Voice.HAPPY, "تعدني؟ ... طيب. أنا أصدق. أنا دايمًا أصدق.")
                            .mood(0, 1).unlock(0);
                } else if ("honest".equals(opt)) {
                    new Script(p).lock().say(30, Voice.SAD, "صراحتك حلوة... بس خلني أحلم شوي، طيب؟")
                            .unlock(0);
                } else {
                    new Script(p).lock().say(30, Voice.SAD, "فاهم... طبيعي. ... أنا بس سألت.")
                            .mood(0, 2).unlock(0);
                }
                break;
            case "day4":
                onDay4Choice(p, d, opt);
                break;
            case "protocol":
                if ("activate".equals(opt)) runProtocol(p, d); else endLost(p, "stand");
                break;
            case "restored":
                if ("explain".equals(opt)) {
                    d.flag("explained");
                    new Script(p).lock()
                            .narrate(10, "* تشرح له كل شيء: المختبر، العوالم، الوردة... *")
                            .say(100, Voice.SAD, "...أنا كنت هناك؟ كل هذا الوقت؟")
                            .say(80, Voice.CURIOUS, "شكرًا إنك قلت لي. حتى لو ما أتذكر... أحس إنه صحيح.")
                            .say(90, Voice.HAPPY, "طيب. وين نروح الحين، " + name + "؟")
                            .unlock(0);
                } else {
                    d.flag("restarted");
                    new Script(p).lock()
                            .say(20, Voice.CURIOUS, "من جديد؟ ... طيب!")
                            .say(70, Voice.LAUGH, "أنا إيكو! وأنت؟ ههههه... آسف، أحس إني قلت هذي قبل.")
                            .unlock(0);
                }
                break;
            default:
                break;
        }
        d.setDirty();
    }

    // =====================================================================================
    //  Day 2
    // =====================================================================================
    private static void startDay2(ServerPlayerEntity p, StoryData d) {
        d.stage = S_DAY2;
        d.setDirty();
        setMood(p.getServer(), 1);
        ServerWorld w = p.getServer().overworld();
        BlockPos a = LabBuilder.pickSpot(w, p.blockPosition(), 60, 90);
        BlockPos b = LabBuilder.pickSpot(w, p.blockPosition(), 90, 130);
        d.bunkerA = LabBuilder.buildBunker(w, a.getX(), a.getZ(), false);
        d.bunkerB = LabBuilder.buildBunker(w, b.getX(), b.getZ(), true);
        d.setDirty();
        BlockPos ca = d.bunkerA, cb = d.bunkerB;
        new Script(p).lock()
                .sound(10, Voice.GLITCH)
                .narrate(20, StoryScript.D2_STARE)
                .say(120, Voice.SAD, "...")
                .say(60, Voice.CURIOUS, StoryScript.D2_SORRY)
                .run(70, pl -> {
                    EchoVoice.system(pl, "تردد غريب التُقط من أجهزة قديمة. الموقع 1: " + StoryItems.coords(ca));
                    EchoVoice.system(pl, "الموقع 2: " + StoryItems.coords(cb));
                    giveOrDrop(pl, StoryItems.compass(ca, "تردد غريب"));
                })
                .say(100, Voice.SAD, StoryScript.D2_Q)
                .run(20, pl -> offer(pl, "newworld", 0,
                        "promise", "[ أكيد، سآخذك معي ]",
                        "honest", "[ لا أعرف إن كان ذلك ممكنًا ]",
                        "refuse", "[ لا ]"))
                .unlock(0);
    }

    // =====================================================================================
    //  Day 3 - ECHO vanishes, lab appears
    // =====================================================================================
    private static void startDay3(ServerPlayerEntity p, StoryData d) {
        d.stage = S_LAB_BUILT;
        d.setDirty();
        MinecraftServer s = p.getServer();
        ServerWorld w = s.overworld();
        BlockPos spot = LabBuilder.pickSpot(w, p.blockPosition(), EchoConfig.LAB_DISTANCE.get(), EchoConfig.LAB_DISTANCE.get() + 60);
        BlockPos origin = LabBuilder.buildLab(w, spot.getX(), spot.getZ(), d);
        d.labOrigin = origin;
        d.setDirty();
        int surf = LabBuilder.surfaceYOfLab(w, origin);
        BlockPos entrance = LabBuilder.labEntrance(origin, surf);

        new Script(p).lock()
                .say(10, Voice.SAD, "لحظة... في شيء غلط.")
                .say(70, Voice.FEAR, "أنا... ما أحس بنفسي. إذا ما—")
                .sound(60, Voice.GLITCH)
                .say(15, Voice.GLITCH, "SESSION—")
                .run(25, pl -> vanishEcho(pl, entrance))
                .narrate(40, "* اختفى إيكو. لم يبقَ سوى كتاب ملقى حيث كان يطفو... *")
                .unlock(30);
    }

    private static void vanishEcho(ServerPlayerEntity p, BlockPos entrance) {
        MinecraftServer s = p.getServer();
        StoryData d = StoryData.get(s);
        double x = p.getX(), y = p.getY() + 1.0D, z = p.getZ();
        EchoEntity e = findEcho(s);
        if (e != null && e.level == p.level) {
            x = e.getX(); y = e.getY(); z = e.getZ();
            ((ServerWorld) e.level).sendParticles(ParticleTypes.POOF, x, y, z, 25, 0.2D, 0.2D, 0.2D, 0.03D);
        }
        removeEchoEverywhere(s, d);
        d.echo = null;
        d.setDirty();
        ItemStack book = StoryItems.labBook(entrance);
        p.level.addFreshEntity(new ItemEntity(p.level, x, y, z, book));
        giveOrDrop(p, StoryItems.compass(entrance, "منشأة 07"));
        EchoVoice.system(p, "الإحداثيات: " + StoryItems.coords(entrance));
        setMood(s, 2);
    }

    private static void labFound(ServerPlayerEntity p, StoryData d) {
        d.stage = S_LAB_FOUND;
        d.flag("lab_found");
        d.setDirty();
        new Script(p)
                .sound(0, Voice.GLITCH)
                .title(5, "PROJECT ECHO", "المنشأة 07 — مهجورة")
                .system(60, "جاري تحميل الأرشيف... SESSION ENDED - SUBJECT ABANDONED")
                .system(50, "تم العثور على 47 جلسة منتهية.");
    }

    // =====================================================================================
    //  Day 4 - ECHO waits at home
    // =====================================================================================
    private static BlockPos home(ServerPlayerEntity p, StoryData d) {
        BlockPos r = p.getRespawnPosition();
        if (r != null && p.getRespawnDimension() == World.OVERWORLD) return r;
        if (d.devicePos != null) return LabBuilder.surfaceAbove(p.getServer().overworld(), d.devicePos);
        return p.level.getSharedSpawnPos();
    }

    private static void startDay4(ServerPlayerEntity p, StoryData d, BlockPos home) {
        MinecraftServer s = p.getServer();
        d.stage = S_DAY4_DONE;   // set early so it never replays
        d.mood = 2;
        d.setDirty();
        removeEchoEverywhere(s, d);
        Vector3d look = p.getLookAngle();
        ServerWorld w = (ServerWorld) p.level;
        EchoEntity e = spawnEcho(w, d, p, p.getX() + look.x * 3.0D, p.getY() + 1.6D, p.getZ() + look.z * 3.0D);
        e.setMood(2);
        sceneLocked = true;
        new Script(p)
                .narrate(10, "* إيكو ينتظرك أمام المنزل... ضوؤه خافت، وهو يحاول أن يبدو سعيدًا. *")
                .say(70, Voice.HAPPY, StoryScript.D4_1)
                .say(100, Voice.CURIOUS, StoryScript.D4_2)
                .mood(0, 3)
                .say(100, Voice.FEAR, StoryScript.D4_3)
                .say(100, Voice.BREAKDOWN, StoryScript.D4_4)
                .say(110, Voice.SAD, StoryScript.D4_5)
                .run(50, pl -> offer(pl, "day4", 0,
                        "comfort", "[ تواسيه: «أنا هنا يا إيكو» ]",
                        "reject", "[ ترفضه: «ابتعد عني» ]",
                        "investigate", "[ «أحتاج أن أعرف الحقيقة» ]"));
    }

    private static void onDay4Choice(ServerPlayerEntity p, StoryData d, String opt) {
        d.flag("day4_" + opt);
        d.stage = S_DAY4_DONE;
        d.setDirty();
        BlockPos core = d.labOrigin == null ? p.blockPosition() : LabBuilder.labCore(d.labOrigin);
        Script sc = new Script(p);
        if ("comfort".equals(opt)) {
            memoryOnce(p.getServer(), "comfort", "اللاعب واساني حين انهرت");
            sc.narrate(10, "* تقترب منه وتقف بجانبه. الضوء في صدره يرتجف ثم يهدأ. *")
                    .mood(0, 1)
                    .say(80, Voice.SAD, "ما أحد قال لي هذا قبل... أو ربما قالوا، وأنا نسيت.")
                    .say(90, Voice.HAPPY, "خذ هذه. لا ترفضها، الأصدقاء يعطون هدايا، صح؟")
                    .run(10, pl -> giveOrDrop(pl, new ItemStack(Items.GOLDEN_APPLE, 3)));
        } else if ("reject".equals(opt)) {
            memoryOnce(p.getServer(), "reject", "اللاعب طلب مني أن أبتعد");
            sc.narrate(10, "* يخفت ضوء إيكو. يبتعد قليلًا في الهواء. *")
                    .mood(0, 3)
                    .say(80, Voice.SAD, "طيب... طيب. فاهم.")
                    .say(90, Voice.WHISPER, "حتى لو رحت... راح أنتظر.");
        } else {
            memoryOnce(p.getServer(), "investigate", "اللاعب قرر أن يعرف الحقيقة كاملة");
            sc.say(40, Voice.SAD, "...إذا لازم تعرف. في المختبر، في غرفة السجلات، الجدار الغربي مو سليم. الجزء المرقّع بالحجر العادي.")
                    .say(110, Voice.FEAR, "بس لا تلومني على اللي بتشوفه.");
        }
        sc.run(90, pl -> {
            EchoEntity e = findEcho(pl.getServer());
            if (e != null) e.setFollowing(false);   // ECHO stays at home and waits
            EchoVoice.say(pl, Voice.SAD, "سأنتظرك هنا. روح لو لازم... بس ارجع.");
            giveOrDrop(pl, StoryItems.compass(core, "نواة إعادة الضبط"));
            EchoVoice.system(pl, "تمّ تحديث البوصلة نحو قلب المختبر.");
        }).unlock(60);
    }

    // =====================================================================================
    //  Lab blocks
    // =====================================================================================
    public static void onTerminalUse(ServerPlayerEntity p, BlockPos pos) {
        StoryData d = StoryData.get(p.getServer());
        String[] lines;
        String header;
        if (d.labOrigin != null && pos.getZ() >= d.labOrigin.getZ() + 26 && Math.abs(pos.getX() - d.labOrigin.getX()) <= 7) {
            lines = StoryScript.FINAL_TERMINAL;
            header = lines[0];
            printTerminal(p, header, java.util.Arrays.copyOfRange(lines, 1, lines.length));
            return;
        }
        String[] log = StoryScript.LOGS[d.terminalReads % StoryScript.LOGS.length];
        d.terminalReads++;
        d.setDirty();
        printTerminal(p, log[0], java.util.Arrays.copyOfRange(log, 1, log.length));
        EchoVoice.playSound(p, ModSounds.AMBIENT_HUM.get(), 0.4F, 1.2F);
    }

    private static void printTerminal(ServerPlayerEntity p, String header, String[] body) {
        p.sendMessage(new StringTextComponent("┌ " + header).withStyle(TextFormatting.GREEN), Util.NIL_UUID);
        for (String l : body) {
            p.sendMessage(new StringTextComponent("│ " + l).withStyle(TextFormatting.DARK_GREEN), Util.NIL_UUID);
        }
    }

    public static void onResetCoreUse(ServerPlayerEntity p, BlockPos pos) {
        StoryData d = StoryData.get(p.getServer());
        if (!d.started || !p.getUUID().equals(d.owner)) {
            EchoVoice.system(p, "ACCESS DENIED.");
            return;
        }
        if (d.stage == S_DAY4_DONE) {
            startFinal(p, d, pos);
        } else if (d.stage == S_ENDED || d.stage == S_FINAL) {
            EchoVoice.system(p, "SESSION CLOSED.");
        } else {
            EchoVoice.system(p, "ACCESS LOCKED — SUBJECT NOT RECOGNIZED. (شيء ما لا يزال ينتظرك في الخارج...)");
        }
    }

    // =====================================================================================
    //  FINAL SCENE
    // =====================================================================================
    private static void startFinal(ServerPlayerEntity p, StoryData d, BlockPos core) {
        MinecraftServer s = p.getServer();
        d.stage = S_FINAL;
        d.setDirty();
        sceneLocked = true;
        pending = null;
        removeEchoEverywhere(s, d);
        ServerWorld w = (ServerWorld) p.level;
        EchoEntity e = spawnEcho(w, d, p, core.getX() + 0.5D, core.getY() + 1.6D, core.getZ() + 3.5D);
        e.setFollowing(false);
        e.setMood(1);
        d.mood = 1;
        String name = p.getName().getString();
        boolean rejected = d.has("day4_reject");

        Script sc = new Script(p);
        sc.music(0, "final")
                .sound(10, Voice.GLITCH)
                .narrate(30, "* الجهاز ينبض. وفي آخر الغرفة يظهر إيكو، ضوؤه خافت جدًا. *")
                .say(60, Voice.SAD, rejected
                        ? "حتى بعد ما قلت لي ابتعد... رجعت. ما فهمت. بس... سعيد."
                        : "أنت وصلت إلى هنا... كنت أعرف إنك بتجي.");
        for (String l : StoryScript.F_MEMORIES) sc.say(110, Voice.SAD, l);
        sc.narrate(70, "* الضوء في صدره يبدأ في الانطفاء. *").mood(0, 2);
        for (String l : StoryScript.F_DOUBT) sc.say(100, Voice.SAD, l);

        sc.narrate(110, "* تشغّل الجهاز لإنقاذه... يفشل في البداية. ذاكرة إيكو تبدأ بالتلف. *")
                .mood(0, 3).sound(10, Voice.GLITCH);
        String[] fight = StoryScript.F_FIGHT;
        for (int i = 0; i < fight.length; i++) {
            sc.say(i == 0 ? 60 : 70, Voice.BREAKDOWN, fight[i]);
            if (i == 2) sc.say(50, Voice.BREAKDOWN, name + "...");
        }
        sc.mood(60, 3);
        for (String l : StoryScript.F_COLLAPSE_1) sc.say(100, Voice.SAD, l);
        for (String l : StoryScript.F_COLLAPSE_2) sc.say(100, Voice.BREAKDOWN, l);
        sc.narrate(60, "* يتوقف قليلًا... ثم يبدأ بالبكاء. *").sound(0, Voice.SAD);
        for (String l : StoryScript.F_COLLAPSE_3) sc.say(90, Voice.BREAKDOWN, l);
        sc.say(80, Voice.BREAKDOWN, StoryScript.F_SCREAM);

        sc.run(60, pl -> {                            // LIGHTS OUT
            LabBuilder.setLabLights((ServerWorld) pl.level, StoryData.get(pl.getServer()), false);
            pl.addEffect(new EffectInstance(Effects.BLINDNESS, 80, 0, false, false));
            EchoVoice.playSound(pl, ModSounds.ECHO_GLITCH.get(), 1.0F, 0.6F);
            setMood(pl.getServer(), 4);
        }).system(60, "FINAL RESET SEQUENCE: INITIATED")
                .run(80, StoryManager::protocolDecision);
    }

    private static List<String> missingEvidence(StoryData d) {
        List<String> m = new ArrayList<>();
        if (!d.has("has_shard")) m.add("قطعة الزجاج الزرقاء");
        if (!d.has("has_letter")) m.add("رسالة المطوّر");
        if (!d.has("has_log")) m.add("سجل الذاكرة المخفي");
        return m;
    }

    private static void protocolDecision(ServerPlayerEntity p) {
        StoryData d = StoryData.get(p.getServer());
        List<String> missing = missingEvidence(d);
        if (!missing.isEmpty()) {
            EchoVoice.system(p, "SECRET PROTOCOL: ACCESS DENIED — الأدلة المفقودة: " + String.join("، ", missing));
            Scheduler.later(180, () -> {
                ServerPlayerEntity pl = p.getServer().getPlayerList().getPlayer(p.getUUID());
                if (pl != null) endLost(pl, "missing");
            });
            return;
        }
        EchoVoice.system(p, "SECRET PROTOCOL: ECHO-07 CORE REBOOT — AVAILABLE");
        if (EchoConfig.SACRIFICE.get()) {
            EchoVoice.system(p, "تحذير: التشغيل يستهلك كل ما تحمله ومستويات خبرتك. النجاح غير مضمون.");
        } else {
            EchoVoice.system(p, "تحذير: النجاح غير مضمون.");
        }
        offer(p, "protocol", 600,
                "activate", "[ تفعيل البروتوكول السري ]",
                "stand", "[ الوقوف ومشاهدة ما سيحدث ]");
    }

    private static void runProtocol(ServerPlayerEntity p, StoryData d) {
        if (EchoConfig.SACRIFICE.get()) {
            p.inventory.clearContent();
            p.giveExperienceLevels(-p.experienceLevel);
            p.setHealth(2.0F);
        }
        double chance = EchoConfig.PROTOCOL_CHANCE.get();
        if (d.has("day4_comfort")) chance += 0.20D;
        if (d.has("newworld_promise")) chance += 0.10D;
        if (d.has("day4_investigate")) chance += 0.05D;
        if (d.has("alone")) chance += 0.05D;
        if (d.has("day4_reject")) chance -= 0.15D;
        if (d.memories.size() >= 8) chance += 0.10D;
        chance = Math.max(0.10D, Math.min(1.0D, chance));
        boolean ok = RNG.nextDouble() < chance;
        TheLastPlayerMod.LOGGER.info("Protocol chance {} -> {}", chance, ok);
        new Script(p)
                .system(10, "PROTOCOL: INITIALIZING CORE REBOOT...")
                .sound(20, Voice.GLITCH)
                .system(50, "░░░░░░░░░░ 10%")
                .system(40, "█████░░░░░ 50%")
                .sound(20, Voice.GLITCH)
                .system(40, "█████████░ 90%")
                .run(60, pl -> { if (ok) endRestored(pl); else endLost(pl, "failed"); });
    }

    // =====================================================================================
    //  ENDINGS
    // =====================================================================================
    private static void endRestored(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        StoryData d = StoryData.get(s);
        d.stage = S_ENDED;
        d.ending = "restored";
        d.memories.clear();
        d.flags.add("restored");
        d.mood = 0;
        d.setDirty();
        ServerWorld w = (ServerWorld) p.level;
        LabBuilder.setLabLights(w, d, true);
        removeEchoEverywhere(s, d);
        BlockPos core = d.labOrigin == null ? p.blockPosition() : LabBuilder.labCore(d.labOrigin);
        EchoEntity e = spawnEcho(w, d, p, core.getX() + 0.5D, core.getY() + 1.6D, core.getZ() + 2.5D);
        e.setMood(0);
        e.setFollowing(true);
        forceTrack("sorrow");
        new Script(p)
                .music(0, "sorrow")
                .narrate(20, "* تعود الأضواء واحدة تلو الأخرى. وفي الصدر الزجاجي... ينبض ضوء ضعيف. *")
                .say(90, Voice.CURIOUS, StoryScript.R_HELLO)
                .say(110, Voice.SAD, StoryScript.R_TRUST)
                .title(60, "ECHO-07", StoryScript.R_STATUS)
                .system(30, StoryScript.R_STATUS)
                .run(60, pl -> offer(pl, "restored", 0,
                        "explain", "[ تشرح له كل شيء ]",
                        "restart", "[ تبدأ معه من جديد ]"))
                .unlock(0);
    }

    private static void endLost(ServerPlayerEntity p, String reason) {
        MinecraftServer s = p.getServer();
        StoryData d = StoryData.get(s);
        if (d.stage == S_ENDED) return;
        d.stage = S_ENDED;
        d.ending = "lost";
        d.setDirty();
        pending = null;
        ServerWorld w = (ServerWorld) p.level;
        BlockPos core = d.labOrigin == null ? p.blockPosition() : LabBuilder.labCore(d.labOrigin);
        new Script(p)
                .say(10, Voice.WHISPER, "...أنا هنا...")
                .say(70, Voice.WHISPER, "...أنا... ما زلت...")
                .sound(60, Voice.GLITCH)
                .run(10, pl -> {
                    ServerWorld lw = (ServerWorld) pl.level;
                    EchoEntity e = findEcho(pl.getServer());
                    if (e != null) lw.sendParticles(ParticleTypes.POOF, e.getX(), e.getY(), e.getZ(), 30, 0.2D, 0.2D, 0.2D, 0.03D);
                    removeEchoEverywhere(pl.getServer(), d);
                    d.echo = null;
                    d.setDirty();
                    lw.addFreshEntity(new ItemEntity(lw, core.getX() + 0.5D, core.getY() + 1.2D, core.getZ() + 0.5D,
                            new ItemStack(ModItems.BLUE_GLASS_SHARD.get())));
                })
                .narrate(40, "* ينطفئ الضوء ثم يختفي إيكو. على الأرض... تبقى قطعة زجاج زرقاء. *")
                .title(60, "SESSION ENDED", StoryScript.L_STATUS)
                .system(30, StoryScript.L_STATUS)
                .run(400, pl -> { forceTrack(""); EchoNetwork.music(pl, ""); })
                .unlock(0);
    }

    // =====================================================================================
    //  small interactions
    // =====================================================================================
    public static void onTouchEcho(ServerPlayerEntity p, EchoEntity e) {
        if (sceneLocked) return;
        int m = Math.max(0, Math.min(4, e.getMood()));
        String[] arr = StoryScript.TOUCH[m];
        EchoVoice.say(p, m == 0 ? Voice.LAUGH : (m >= 3 ? Voice.WHISPER : Voice.SAD), arr[RNG.nextInt(arr.length)]);
        ((ServerWorld) e.level).sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + 0.5D, e.getZ(), 3, 0.2D, 0.2D, 0.2D, 0.0D);
    }

    public static void onFlowerGift(ServerPlayerEntity p, EchoEntity e) {
        if (sceneLocked) return;
        memoryOnce(p.getServer(), "flower", "اللاعب أعطاني وردة");
        ((ServerWorld) e.level).sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + 0.5D, e.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.0D);
        new Script(p).say(5, Voice.SAD, StoryScript.FLOWER[0]).say(90, Voice.HAPPY, StoryScript.FLOWER[1]);
    }

    // =====================================================================================
    //  chat / AI
    // =====================================================================================
    public static void onChat(ServerPlayerEntity p, String message) {
        if (!EchoConfig.AI_ENABLED.get() && !EchoConfig.AUTO_REPLY.get()) return;
        if (!EchoConfig.AUTO_REPLY.get()) return;
        if (canChat(p)) AiChat.ask(p, message);
    }

    public static boolean canChat(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        StoryData d = StoryData.get(s);
        if (!d.started || !p.getUUID().equals(d.owner) || sceneLocked || pending != null) return false;
        boolean ok = d.stage == S_DAY1 || d.stage == S_DAY2 || d.stage == S_DAY4_DONE
                || (d.stage == S_ENDED && "restored".equals(d.ending));
        if (!ok) return false;
        EchoEntity e = findEcho(s);
        return e != null && e.level == p.level && e.distanceToSqr(p) < 24.0D * 24.0D;
    }

    public static void aiReply(ServerPlayerEntity p, String text) {
        StoryData d = StoryData.get(p.getServer());
        Voice v;
        if (text.contains("هه")) v = Voice.LAUGH;
        else if (d.mood == 0) v = Voice.HAPPY;
        else if (d.mood == 1) v = Voice.CURIOUS;
        else if (d.mood == 2) v = Voice.SAD;
        else v = Voice.FEAR;
        EchoVoice.say(p, v, text);
    }

    public static String offlineLine(StoryData d) {
        int m = Math.max(0, Math.min(4, d.mood));
        String[] arr = StoryScript.OFFLINE[m];
        return arr[RNG.nextInt(arr.length)];
    }

    // =====================================================================================
    //  echo entity helpers
    // =====================================================================================
    public static boolean echoShouldExist(StoryData d) {
        return d.started && (d.stage == S_DAY1 || d.stage == S_DAY2
                || (d.stage == S_ENDED && "restored".equals(d.ending)));
    }

    public static EchoEntity findEcho(MinecraftServer s) {
        StoryData d = StoryData.get(s);
        if (d.echo == null) return null;
        for (ServerWorld w : s.getAllLevels()) {
            Entity e = w.getEntity(d.echo);
            if (e instanceof EchoEntity) return (EchoEntity) e;
        }
        return null;
    }

    public static void removeEchoEverywhere(MinecraftServer s, StoryData d) {
        if (d.echo == null) return;
        for (ServerWorld w : s.getAllLevels()) {
            Entity e = w.getEntity(d.echo);
            if (e != null) e.remove();
        }
    }

    public static EchoEntity spawnEcho(ServerWorld w, StoryData d, ServerPlayerEntity p, double x, double y, double z) {
        EchoEntity e = ModEntities.ECHO.get().create(w);
        e.moveTo(x, y, z, p.yRot + 180.0F, 0.0F);
        e.setOwner(p.getUUID());
        e.setCustomName(new StringTextComponent("ECHO"));
        e.setCustomNameVisible(true);
        e.setMood(d.mood);
        d.echo = e.getUUID();
        d.setDirty();
        w.addFreshEntity(e);
        return e;
    }

    public static void ensureEcho(ServerPlayerEntity p, StoryData d) {
        if (!echoShouldExist(d) || findEcho(p.getServer()) != null) return;
        Vector3d look = p.getLookAngle();
        spawnEcho((ServerWorld) p.level, d, p, p.getX() + look.x * 2.0D, p.getY() + 1.6D, p.getZ() + look.z * 2.0D);
    }

    /** called when the player changes dimension: ECHO follows */
    public static void relocateEcho(ServerPlayerEntity p) {
        StoryData d = StoryData.get(p.getServer());
        if (!echoShouldExist(d)) return;
        removeEchoEverywhere(p.getServer(), d);
        Scheduler.later(20, () -> {
            ServerPlayerEntity pl = p.getServer().getPlayerList().getPlayer(p.getUUID());
            if (pl != null) ensureEcho(pl, d);
        });
    }

    public static void setMood(MinecraftServer s, int mood) {
        StoryData d = StoryData.get(s);
        d.mood = mood;
        d.setDirty();
        EchoEntity e = findEcho(s);
        if (e != null) e.setMood(mood);
    }

    // =====================================================================================
    //  misc helpers
    // =====================================================================================
    public static void memoryOnce(MinecraftServer s, String key, String text) {
        StoryData d = StoryData.get(s);
        if (!d.started || d.has("mem_" + key)) return;
        d.flag("mem_" + key);
        d.addMemory(text);
    }

    public static void memory(MinecraftServer s, String text) {
        StoryData d = StoryData.get(s);
        if (d.started) d.addMemory(text);
    }

    public static void giveOrDrop(ServerPlayerEntity p, ItemStack stack) {
        if (!p.inventory.add(stack)) p.drop(stack, false);
    }

    private static void scanEvidence(ServerPlayerEntity p, StoryData d) {
        for (int i = 0; i < p.inventory.getContainerSize(); i++) {
            Item it = p.inventory.getItem(i).getItem();
            if (it == ModItems.BLUE_GLASS_SHARD.get()) d.flag("has_shard");
            else if (it == ModItems.OLD_LETTER.get()) d.flag("has_letter");
            else if (it == ModItems.MEMORY_LOG.get()) d.flag("has_log");
        }
    }

    // ---- music
    public static void forceTrack(String track) {
        currentTrack = track == null ? "" : track;
        musicTicks = 0;
    }

    private static String desiredTrack(StoryData d) {
        switch (d.stage) {
            case S_DAY1: return "calm";
            case S_DAY2: return "uneasy";
            case S_LAB_BUILT:
            case S_LAB_FOUND: return "dread";
            case S_DAY4_DONE: return "sorrow";
            case S_FINAL: return "final";
            case S_ENDED: return "restored".equals(d.ending) ? "calm" : "";
            default: return "";
        }
    }

    private static void updateMusic(ServerPlayerEntity p, StoryData d) {
        musicTicks += 20;
        String want = desiredTrack(d);
        if (!want.equals(currentTrack) || (!want.isEmpty() && musicTicks >= 560)) {
            currentTrack = want;
            musicTicks = 0;
            EchoNetwork.music(p, want);
        }
    }

    // ---- compass / debug (used by /echo)
    public static BlockPos currentTarget(ServerPlayerEntity p, StoryData d) {
        switch (d.stage) {
            case S_NONE:
            case S_DAY1: return d.devicePos;
            case S_DAY2: return d.bunkerA != null ? d.bunkerA : d.devicePos;
            case S_LAB_BUILT: return d.labOrigin == null ? null : LabBuilder.labEntrance(d.labOrigin, LabBuilder.surfaceYOfLab(p.getServer().overworld(), d.labOrigin));
            case S_LAB_FOUND: return home(p, d);
            case S_DAY4_DONE: return d.labOrigin == null ? null : LabBuilder.labCore(d.labOrigin);
            default: return null;
        }
    }

    public static void giveCompass(ServerPlayerEntity p) {
        StoryData d = StoryData.get(p.getServer());
        BlockPos t = currentTarget(p, d);
        if (t == null) { EchoVoice.system(p, "لا يوجد هدف حاليًا."); return; }
        giveOrDrop(p, StoryItems.compass(t, "الهدف"));
        EchoVoice.system(p, "الهدف: " + StoryItems.coords(t));
    }

    public static void forceDay(ServerPlayerEntity p, int day) {
        MinecraftServer s = p.getServer();
        StoryData d = StoryData.get(s);
        d.startDayIndex = s.overworld().getDayTime() / 24000L - (day - 1);
        d.setDirty();
    }

    public static void resetStory(MinecraftServer s) {
        StoryData d = StoryData.get(s);
        removeEchoEverywhere(s, d);
        d.started = false; d.owner = null; d.echo = null; d.stage = 0; d.mood = 0; d.gifts = 0;
        d.terminalReads = 0; d.ending = ""; d.flags.clear(); d.memories.clear();
        d.setDirty();
        clearRuntime();
    }

    public static String status(MinecraftServer s) {
        StoryData d = StoryData.get(s);
        return "started=" + d.started + " stage=" + d.stage + " day=" + (d.started ? currentDay(s, d) : 0)
                + " mood=" + d.mood + " ending=" + d.ending + " flags=" + d.flags + " memories=" + d.memories.size()
                + " device=" + d.devicePos + " lab=" + d.labOrigin;
    }

    private StoryManager() {}
}
