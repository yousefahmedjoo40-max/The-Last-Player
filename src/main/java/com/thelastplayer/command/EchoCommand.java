package com.thelastplayer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.thelastplayer.ai.AiChat;
import com.thelastplayer.story.EchoVoice;
import com.thelastplayer.story.StoryManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

public final class EchoCommand {
    public static void register(CommandDispatcher<CommandSource> d) {
        d.register(Commands.literal("echo")
                .then(Commands.literal("say")
                        .then(Commands.argument("message", StringArgumentType.greedyString()).executes(c -> {
                            ServerPlayerEntity p = c.getSource().getPlayerOrException();
                            if (StoryManager.canChat(p)) AiChat.ask(p, StringArgumentType.getString(c, "message"));
                            else EchoVoice.system(p, "إيكو ليس قريبًا منك الآن.");
                            return 1;
                        })))
                .then(Commands.literal("choice")
                        .then(Commands.argument("option", StringArgumentType.word()).executes(c -> {
                            StoryManager.choose(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "option"));
                            return 1;
                        })))
                .then(Commands.literal("compass").executes(c -> {
                    StoryManager.giveCompass(c.getSource().getPlayerOrException());
                    return 1;
                }))
                // ---- admin / testing
                .then(Commands.literal("status").requires(s -> s.hasPermission(2)).executes(c -> {
                    c.getSource().sendSuccess(new StringTextComponent(StoryManager.status(c.getSource().getServer())), false);
                    return 1;
                }))
                .then(Commands.literal("day").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 10)).executes(c -> {
                            ServerPlayerEntity p = c.getSource().getPlayerOrException();
                            StoryManager.forceDay(p, IntegerArgumentType.getInteger(c, "n"));
                            c.getSource().sendSuccess(new StringTextComponent("Story day set. Scenes trigger within a few seconds (one stage at a time)."), false);
                            return 1;
                        })))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(c -> {
                    StoryManager.resetStory(c.getSource().getServer());
                    AiChat.clearHistory();
                    c.getSource().sendSuccess(new StringTextComponent("Story reset. The device can be used again."), true);
                    return 1;
                })));
    }

    private EchoCommand() {}
}
