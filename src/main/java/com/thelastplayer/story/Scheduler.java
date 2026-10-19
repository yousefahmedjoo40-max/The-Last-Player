package com.thelastplayer.story;

import com.thelastplayer.TheLastPlayerMod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tiny tick scheduler (server thread only). */
public final class Scheduler {
    private static final class Task {
        int ticks;
        final Runnable action;
        Task(int ticks, Runnable action) { this.ticks = ticks; this.action = action; }
    }

    private static final List<Task> TASKS = new ArrayList<>();

    public static void later(int ticks, Runnable action) {
        TASKS.add(new Task(Math.max(1, ticks), action));
    }

    public static void tick() {
        if (TASKS.isEmpty()) return;
        List<Runnable> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (--t.ticks <= 0) {
                due.add(t.action);
                it.remove();
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (Exception e) {
                TheLastPlayerMod.LOGGER.error("Scheduled story task failed", e);
            }
        }
    }

    public static void clear() { TASKS.clear(); }

    private Scheduler() {}
}
