package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.config.ArsenalClientConfig;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.event.sound.PlaySoundEvent;

/** Scales Arsenal's own sound events by the client's {@code sounds.volume} option. */
public final class ArsenalSoundVolume {
    static void onPlay(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        double volume = ArsenalClientConfig.get(ArsenalClientConfig.SOUND_VOLUME);
        if (sound == null || volume == 1.0D || sound instanceof Scaled
            || !ArsenalMod.MOD_ID.equals(sound.getLocation().getNamespace())) return;
        event.setSound(volume <= 0.0D ? null : new Scaled(sound, (float)volume));
    }

    /** The same sound, louder or quieter. */
    private record Scaled(SoundInstance inner, float factor) implements SoundInstance {
        @Override public ResourceLocation getLocation() { return inner.getLocation(); }
        @Override public WeighedSoundEvents resolve(SoundManager manager) { return inner.resolve(manager); }
        @Override public Sound getSound() { return inner.getSound(); }
        @Override public SoundSource getSource() { return inner.getSource(); }
        @Override public boolean isLooping() { return inner.isLooping(); }
        @Override public boolean isRelative() { return inner.isRelative(); }
        @Override public int getDelay() { return inner.getDelay(); }
        @Override public float getVolume() { return inner.getVolume() * factor; }
        @Override public float getPitch() { return inner.getPitch(); }
        @Override public double getX() { return inner.getX(); }
        @Override public double getY() { return inner.getY(); }
        @Override public double getZ() { return inner.getZ(); }
        @Override public Attenuation getAttenuation() { return inner.getAttenuation(); }
        @Override public boolean canStartSilent() { return inner.canStartSilent(); }
        @Override public boolean canPlaySound() { return inner.canPlaySound(); }
        @Override public java.util.concurrent.CompletableFuture<net.minecraft.client.sounds.AudioStream> getStream(
                net.minecraft.client.sounds.SoundBufferLibrary library, Sound sound, boolean looping) {
            return inner.getStream(library, sound, looping);
        }
    }

    private ArsenalSoundVolume() {}
}
