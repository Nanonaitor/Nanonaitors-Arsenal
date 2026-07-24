package com.nanonaitor.arsenal.registry;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.effect.ArmorFractureEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ArsenalMod.MOD_ID);
    public static final RegistryObject<MobEffect> ARMOR_FRACTURE = EFFECTS.register("armor_fracture", ArmorFractureEffect::new);
    private ModEffects() {}
}
