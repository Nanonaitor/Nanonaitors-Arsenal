package com.nanonaitor.arsenal.registry;

import com.nanonaitor.arsenal.ArsenalMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ArsenalMod.MOD_ID);
    public static final RegistryObject<SoundEvent> REVERSO_HEAL = SOUNDS.register("reverso_heal",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArsenalMod.MOD_ID, "reverso_heal")));
    public static final RegistryObject<SoundEvent> BLADE_STAFF_REFLECT = SOUNDS.register("blade_staff_reflect",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArsenalMod.MOD_ID, "blade_staff_reflect")));
    public static final RegistryObject<SoundEvent> SCIMITAR_BASH = SOUNDS.register("scimitar_bash",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArsenalMod.MOD_ID, "scimitar_bash")));
    /** Chain rattle layered on each wind-up swing and throw. */
    public static final RegistryObject<SoundEvent> BALL_CHAIN_RATTLE = SOUNDS.register("ball_chain.rattle",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArsenalMod.MOD_ID, "ball_chain.rattle")));
    /** Heavier chain clash when a thrown ball crashes into a block. */
    public static final RegistryObject<SoundEvent> BALL_CHAIN_CRASH = SOUNDS.register("ball_chain.crash",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArsenalMod.MOD_ID, "ball_chain.crash")));
    /** Battering Ram: the log creaks as a charge starts. */
    public static final RegistryObject<SoundEvent> RAM_CHARGE = SOUNDS.register("battering_ram.charge",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArsenalMod.MOD_ID, "battering_ram.charge")));
    /** Battering Ram: a heavy bash on hits and walls. */
    public static final RegistryObject<SoundEvent> RAM_IMPACT = SOUNDS.register("battering_ram.impact",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArsenalMod.MOD_ID, "battering_ram.impact")));
    /** Claws: a high metal slash on every hit. */
    public static final RegistryObject<SoundEvent> CLAWS_SLASH = SOUNDS.register("claws.slash",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArsenalMod.MOD_ID, "claws.slash")));
    /** Claws: a ringing accent on every third hit. */
    public static final RegistryObject<SoundEvent> CLAWS_RING = SOUNDS.register("claws.ring",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ArsenalMod.MOD_ID, "claws.ring")));

    private ModSounds() {}
}
