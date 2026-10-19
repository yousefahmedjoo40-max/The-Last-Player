package com.thelastplayer.story;

import com.thelastplayer.registry.ModSounds;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextFormatting;

import java.util.function.Supplier;

/** ECHO's voice types. Each one plays a different sound and tints the chat line. */
public enum Voice {
    NONE(null, TextFormatting.WHITE, false),
    HAPPY(ModSounds.ECHO_HAPPY, TextFormatting.WHITE, false),
    LAUGH(ModSounds.ECHO_LAUGH, TextFormatting.WHITE, false),
    CURIOUS(ModSounds.ECHO_CURIOUS, TextFormatting.WHITE, false),
    SAD(ModSounds.ECHO_SAD, TextFormatting.GRAY, true),
    FEAR(ModSounds.ECHO_FEAR, TextFormatting.AQUA, true),
    BREAKDOWN(ModSounds.ECHO_BREAKDOWN, TextFormatting.RED, true),
    GLITCH(ModSounds.ECHO_GLITCH, TextFormatting.DARK_GRAY, false),
    WHISPER(ModSounds.ECHO_WHISPER, TextFormatting.DARK_GRAY, true);

    public final Supplier<SoundEvent> sound;
    public final TextFormatting color;
    public final boolean italic;

    Voice(Supplier<SoundEvent> sound, TextFormatting color, boolean italic) {
        this.sound = sound;
        this.color = color;
        this.italic = italic;
    }
}
