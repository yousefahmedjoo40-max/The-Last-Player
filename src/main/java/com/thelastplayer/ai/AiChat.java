package com.thelastplayer.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thelastplayer.TheLastPlayerMod;
import com.thelastplayer.config.EchoConfig;
import com.thelastplayer.story.StoryData;
import com.thelastplayer.story.StoryManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Side conversations with ECHO through an LLM API (Anthropic or OpenAI-compatible). */
public final class AiChat {
    private static final class Msg {
        final String role, content;
        Msg(String r, String c) { role = r; content = c; }
    }

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ECHO-AI");
        t.setDaemon(true);
        return t;
    });
    private static final LinkedList<Msg> HISTORY = new LinkedList<>();
    private static boolean busy;
    private static long last;
    private static boolean warnedNoKey;

    public static void ask(ServerPlayerEntity p, String text) {
        MinecraftServer server = p.getServer();
        if (busy) return;
        long now = System.currentTimeMillis();
        if (now - last < EchoConfig.AI_COOLDOWN.get() * 1000L) return;
        last = now;

        StoryData d = StoryData.get(server);
        final UUID id = p.getUUID();
        final String system = EchoPrompt.build(d, p.getName().getString(), StoryManager.currentDay(server, d));
        final String userText = text.length() > 300 ? text.substring(0, 300) : text;

        final List<Msg> convo = new ArrayList<>(HISTORY);
        convo.add(new Msg("user", userText));

        final boolean enabled = EchoConfig.AI_ENABLED.get();
        final String key = EchoConfig.apiKey();
        if (!enabled || key.isEmpty()) {
            if (enabled && !warnedNoKey) {
                warnedNoKey = true;
                TheLastPlayerMod.LOGGER.warn("No API key set (config 'apiKey' or env ECHO_API_KEY). ECHO uses offline lines.");
            }
            deliver(server, id, StoryManager.offlineLine(d), userText, false);
            return;
        }
        final String format = EchoConfig.AI_FORMAT.get(), url = EchoConfig.AI_URL.get(), model = EchoConfig.AI_MODEL.get();
        final int maxTokens = EchoConfig.AI_MAX_TOKENS.get(), timeout = EchoConfig.AI_TIMEOUT.get() * 1000;
        final double temp = EchoConfig.AI_TEMPERATURE.get();

        busy = true;
        POOL.submit(() -> {
            String reply = null;
            try {
                reply = request(format, url, key, model, system, convo, maxTokens, temp, timeout);
            } catch (Exception e) {
                TheLastPlayerMod.LOGGER.warn("ECHO AI request failed: {}", e.toString());
            }
            final String r = reply;
            server.execute(() -> {
                busy = false;
                String out = (r == null || r.isEmpty()) ? StoryManager.offlineLine(StoryData.get(server)) : r;
                deliver(server, id, out, userText, r != null && !r.isEmpty());
            });
        });
    }

    private static void deliver(MinecraftServer server, UUID id, String reply, String userText, boolean fromAi) {
        ServerPlayerEntity pl = server.getPlayerList().getPlayer(id);
        if (pl == null || StoryManager.sceneLocked) return;   // a scripted scene started meanwhile: drop it
        String clean = reply.replace("\n", " ").trim();
        if (clean.startsWith("ECHO:")) clean = clean.substring(5).trim();
        if (clean.length() > 220) clean = clean.substring(0, 220);
        if (fromAi) {
            HISTORY.add(new Msg("user", userText));
            HISTORY.add(new Msg("assistant", clean));
            while (HISTORY.size() > 12) HISTORY.removeFirst();
        }
        StoryManager.aiReply(pl, clean);
    }

    public static void clearHistory() { HISTORY.clear(); busy = false; }

    // ------------------------------------------------------------------ HTTP
    private static String request(String format, String url, String key, String model, String system,
                                  List<Msg> convo, int maxTokens, double temp, int timeoutMs) throws Exception {
        boolean anthropic = !"openai".equalsIgnoreCase(format);
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", temp);
        JsonArray msgs = new JsonArray();
        if (anthropic) {
            body.addProperty("system", system);
        } else {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", system);
            msgs.add(sys);
        }
        // roles must alternate (anthropic) and start with "user": merge consecutive same-role turns
        String lastRole = null;
        StringBuilder acc = new StringBuilder();
        List<Msg> merged = new ArrayList<>();
        for (Msg m : convo) {
            if (lastRole != null && lastRole.equals(m.role)) {
                acc.append('\n').append(m.content);
            } else {
                if (lastRole != null) merged.add(new Msg(lastRole, acc.toString()));
                lastRole = m.role;
                acc.setLength(0);
                acc.append(m.content);
            }
        }
        if (lastRole != null) merged.add(new Msg(lastRole, acc.toString()));
        while (!merged.isEmpty() && !"user".equals(merged.get(0).role)) merged.remove(0);
        for (Msg m : merged) {
            JsonObject o = new JsonObject();
            o.addProperty("role", m.role);
            o.addProperty("content", m.content);
            msgs.add(o);
        }
        body.add("messages", msgs);

        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(timeoutMs);
        c.setReadTimeout(timeoutMs);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        if (anthropic) {
            c.setRequestProperty("x-api-key", key);
            c.setRequestProperty("anthropic-version", "2023-06-01");
        } else {
            c.setRequestProperty("Authorization", "Bearer " + key);
        }
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String resp = readAll(is);
        if (code >= 400) throw new RuntimeException("HTTP " + code + ": " + (resp.length() > 200 ? resp.substring(0, 200) : resp));

        JsonObject json = new JsonParser().parse(resp).getAsJsonObject();
        if (anthropic) {
            JsonArray content = json.getAsJsonArray("content");
            for (int i = 0; i < content.size(); i++) {
                JsonObject b = content.get(i).getAsJsonObject();
                if (b.has("text")) return b.get("text").getAsString().trim();
            }
            return null;
        }
        return json.getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content").getAsString().trim();
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private AiChat() {}
}
