package com.thelastplayer.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EchoConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue AI_ENABLED;
    public static final ForgeConfigSpec.BooleanValue AUTO_REPLY;
    public static final ForgeConfigSpec.ConfigValue<String> AI_FORMAT;
    public static final ForgeConfigSpec.ConfigValue<String> AI_URL;
    public static final ForgeConfigSpec.ConfigValue<String> AI_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> AI_MODEL;
    public static final ForgeConfigSpec.IntValue AI_MAX_TOKENS;
    public static final ForgeConfigSpec.IntValue AI_TIMEOUT;
    public static final ForgeConfigSpec.IntValue AI_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue AI_TEMPERATURE;

    public static final ForgeConfigSpec.IntValue LAB_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue SACRIFICE;
    public static final ForgeConfigSpec.DoubleValue PROTOCOL_CHANCE;
    public static final ForgeConfigSpec.BooleanValue GIFTS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("AI dialogue. Used ONLY for side conversations - all main story scenes are scripted.").push("ai");
        AI_ENABLED = b.comment("Enable AI replies. If false (or the API fails) ECHO uses built-in offline lines.")
                .define("enabled", true);
        AUTO_REPLY = b.comment("ECHO answers normal chat messages when he is near you. If false, use /echo say <message>.")
                .define("autoReplyToChat", true);
        AI_FORMAT = b.comment("API format: \"anthropic\" (Messages API) or \"openai\" (any OpenAI-compatible /chat/completions endpoint).")
                .define("format", "anthropic");
        AI_URL = b.comment("Full endpoint URL. Anthropic: https://api.anthropic.com/v1/messages | OpenAI-compatible: https://api.openai.com/v1/chat/completions")
                .define("apiUrl", "https://api.anthropic.com/v1/messages");
        AI_KEY = b.comment("API key. PREFER leaving this empty and setting the environment variable ECHO_API_KEY instead. NEVER commit a key to GitHub.")
                .define("apiKey", "");
        AI_MODEL = b.comment("Model name, e.g. claude-haiku-5-5 (anthropic) or gpt-4o-mini (openai).")
                .define("model", "claude-haiku-5-5");
        AI_MAX_TOKENS = b.defineInRange("maxTokens", 160, 32, 1024);
        AI_TEMPERATURE = b.defineInRange("temperature", 0.9D, 0.0D, 1.0D);
        AI_TIMEOUT = b.comment("Seconds before giving up on the API.").defineInRange("timeoutSeconds", 20, 3, 120);
        AI_COOLDOWN = b.comment("Minimum seconds between AI requests.").defineInRange("cooldownSeconds", 3, 0, 60);
        b.pop();

        b.push("story");
        LAB_DISTANCE = b.comment("Approximate distance (blocks) of the abandoned lab from where you are on Day 3.")
                .defineInRange("labDistance", 180, 60, 600);
        GIFTS = b.comment("ECHO gives small gifts during Day 1.").define("giftsOnDay1", true);
        SACRIFICE = b.comment("The secret protocol consumes your inventory and XP (the 'big sacrifice').")
                .define("protocolSacrifice", true);
        PROTOCOL_CHANCE = b.comment("Base chance (0-1) that the protocol succeeds. Your choices add/subtract. Set 1.0 to always succeed.")
                .defineInRange("protocolBaseChance", 0.45D, 0.0D, 1.0D);
        b.pop();

        SPEC = b.build();
    }

    public static String apiKey() {
        String k = AI_KEY.get();
        if (k == null || k.trim().isEmpty()) k = System.getenv("ECHO_API_KEY");
        return k == null ? "" : k.trim();
    }

    private EchoConfig() {}
}
