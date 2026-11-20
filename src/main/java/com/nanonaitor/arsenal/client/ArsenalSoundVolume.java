package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Scales Arsenal's own sound events by the client's {@code sounds.volume} setting. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class ArsenalSoundVolume {
    private ArsenalSoundVolume() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlay(PlaySoundEvent event) {
        ISound sound = event.getResultSound();
        double volume = ArsenalConfig.sounds.volume;
        if (sound == null || volume == 1.0D || sound instanceof Scaled
            || !NanonaitorsArsenal.MOD_ID.equals(sound.getSoundLocation().getResourceDomain())) return;
        event.setResultSound(volume <= 0.0D ? null : new Scaled(sound, (float) volume));
    }

    /** The same sound, louder or quieter. */
    private static final class Scaled implements ISound {
        private final ISound inner;
        private final float factor;

        Scaled(ISound inner, float factor) {
            this.inner = inner;
            this.factor = factor;
        }

        @Override public ResourceLocation getSoundLocation() { return inner.getSoundLocation(); }
        @Override public SoundEventAccessor createAccessor(SoundHandler handler) { return inner.createAccessor(handler); }
        @Override public Sound getSound() { return inner.getSound(); }
        @Override public SoundCategory getCategory() { return inner.getCategory(); }
        @Override public boolean canRepeat() { return inner.canRepeat(); }
        @Override public int getRepeatDelay() { return inner.getRepeatDelay(); }
        @Override public float getVolume() { return inner.getVolume() * factor; }
        @Override public float getPitch() { return inner.getPitch(); }
        @Override public float getXPosF() { return inner.getXPosF(); }
        @Override public float getYPosF() { return inner.getYPosF(); }
        @Override public float getZPosF() { return inner.getZPosF(); }
        @Override public AttenuationType getAttenuationType() { return inner.getAttenuationType(); }
    }
}
