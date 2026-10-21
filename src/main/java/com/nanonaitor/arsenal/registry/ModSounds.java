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

    private ModSounds() {}
}
