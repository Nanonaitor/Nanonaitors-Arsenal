package com.nanonaitor.arsenal.registry;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Dedicated IDs distinguish actual chain hit windows from predicted melee audio. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ModSounds {
    public static final SoundEvent BALL_CHAIN_SWING = create("ball_chain.swing");
    public static final SoundEvent BALL_CHAIN_HIT = create("ball_chain.hit");
    /** Chain rattle layered on each wind-up swing and throw. */
    public static final SoundEvent BALL_CHAIN_RATTLE = create("ball_chain.rattle");
    /** Heavier chain clash when a thrown ball crashes into a block. */
    public static final SoundEvent BALL_CHAIN_CRASH = create("ball_chain.crash");
    public static final SoundEvent SCIMITAR_BASH = create("scimitar.bash");
    public static final SoundEvent REVERSO_HEAL = create("reverso_heal");
    public static final SoundEvent BLADE_STAFF_REFLECT = create("blade_staff_reflect");

    private ModSounds() {}

    private static SoundEvent create(String name) {
        ResourceLocation id = new ResourceLocation(NanonaitorsArsenal.MOD_ID, name);
        return new SoundEvent(id).setRegistryName(id);
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(BALL_CHAIN_SWING, BALL_CHAIN_HIT, BALL_CHAIN_RATTLE,
            BALL_CHAIN_CRASH, SCIMITAR_BASH,
            REVERSO_HEAL, BLADE_STAFF_REFLECT);
    }
}
