package com.thelastplayer.story;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.StringNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.WorldSavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Everything the story remembers. Saved with the world. */
public class StoryData extends WorldSavedData {
    public static final String NAME = "thelastplayer_story";
    private static final int MAX_MEMORIES = 40;

    public boolean started;
    public boolean roomBuilt;
    public UUID owner;
    public UUID echo;
    public long startDayIndex;
    public int stage;
    public int mood;
    public int gifts;
    public int terminalReads;
    public String ending = "";

    public BlockPos devicePos;
    public BlockPos labOrigin;
    public BlockPos bunkerA;
    public BlockPos bunkerB;
    public long[] labLights = new long[0];

    public final Set<String> flags = new HashSet<>();
    public final List<String> memories = new ArrayList<>();

    public StoryData() { super(NAME); }

    public static StoryData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(StoryData::new, NAME);
    }

    public boolean has(String flag) { return flags.contains(flag); }
    public void flag(String flag) { if (flags.add(flag)) setDirty(); }

    public void addMemory(String text) {
        if (memories.contains(text)) return;
        memories.add(text);
        while (memories.size() > MAX_MEMORIES) memories.remove(0);
        setDirty();
    }

    @Override
    public void load(CompoundNBT t) {
        started = t.getBoolean("started");
        roomBuilt = t.getBoolean("roomBuilt");
        owner = t.hasUUID("owner") ? t.getUUID("owner") : null;
        echo = t.hasUUID("echo") ? t.getUUID("echo") : null;
        startDayIndex = t.getLong("startDayIndex");
        stage = t.getInt("stage");
        mood = t.getInt("mood");
        gifts = t.getInt("gifts");
        terminalReads = t.getInt("terminalReads");
        ending = t.getString("ending");
        devicePos = readPos(t, "devicePos");
        labOrigin = readPos(t, "labOrigin");
        bunkerA = readPos(t, "bunkerA");
        bunkerB = readPos(t, "bunkerB");
        labLights = t.getLongArray("labLights");
        flags.clear();
        ListNBT f = t.getList("flags", 8);
        for (int i = 0; i < f.size(); i++) flags.add(f.getString(i));
        memories.clear();
        ListNBT m = t.getList("memories", 8);
        for (int i = 0; i < m.size(); i++) memories.add(m.getString(i));
    }

    @Override
    public CompoundNBT save(CompoundNBT t) {
        t.putBoolean("started", started);
        t.putBoolean("roomBuilt", roomBuilt);
        if (owner != null) t.putUUID("owner", owner);
        if (echo != null) t.putUUID("echo", echo);
        t.putLong("startDayIndex", startDayIndex);
        t.putInt("stage", stage);
        t.putInt("mood", mood);
        t.putInt("gifts", gifts);
        t.putInt("terminalReads", terminalReads);
        t.putString("ending", ending == null ? "" : ending);
        writePos(t, "devicePos", devicePos);
        writePos(t, "labOrigin", labOrigin);
        writePos(t, "bunkerA", bunkerA);
        writePos(t, "bunkerB", bunkerB);
        t.putLongArray("labLights", labLights);
        ListNBT f = new ListNBT();
        for (String s : flags) f.add(StringNBT.valueOf(s));
        t.put("flags", f);
        ListNBT m = new ListNBT();
        for (String s : memories) m.add(StringNBT.valueOf(s));
        t.put("memories", m);
        return t;
    }

    private static BlockPos readPos(CompoundNBT t, String key) {
        return t.contains(key) ? NBTUtil.readBlockPos(t.getCompound(key)) : null;
    }

    private static void writePos(CompoundNBT t, String key, BlockPos pos) {
        if (pos != null) t.put(key, NBTUtil.writeBlockPos(pos));
    }
}
