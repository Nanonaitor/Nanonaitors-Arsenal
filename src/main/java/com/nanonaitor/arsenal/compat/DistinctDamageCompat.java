package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.item.ItemDoubleBladedScimitar;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.item.ItemLinkedClaw;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.WeaponTier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

/**
 * Optional Distinct Damage Descriptions integration.
 *
 * DDD remains a completely optional dependency: its public API is reached by
 * reflection only after Forge confirms that the mod is loaded. Material
 * conversions mirror Dregora's DDD definitions for equivalent Spartan/I&F
 * weapons rather than introducing a second, conflicting ruleset.
 */
public final class DistinctDamageCompat {
    private static final String MOD_ID = "distinctdamagedescriptions";
    private static final Map<ItemStack, String> APPLIED =
        Collections.synchronizedMap(new WeakHashMap<ItemStack, String>());
    private static final Map<String, Object> DAMAGE_TYPES = new HashMap<>();
    private static boolean initialized;
    private static boolean unavailable;
    private static Object accessor;
    private static Method getDistribution;
    private static Method setBaseWeights;
    private static Method setWeights;
    private static Method getModifiers;
    private static Method removeModifier;
    private static Method updateDistribution;

    private DistinctDamageCompat() {}

    public static void apply(ItemStack stack, ItemArsenalWeapon weapon) {
        if (stack.isEmpty() || !Loader.isModLoaded(MOD_ID) || unavailable) return;
        String signature = weapon.getClass().getName() + ':' + weapon.getTier().getId();
        if (signature.equals(APPLIED.get(stack))) return;
        try {
            if (!initialized) initialize();
            if (unavailable) return;
            Object optional = getDistribution.invoke(accessor, stack);
            if (!(optional instanceof Optional) || !((Optional<?>) optional).isPresent()) return;
            Object distribution = ((Optional<?>) optional).get();
            Map<Object, Float> weights = weights(weapon);
            setBaseWeights.invoke(distribution, weights);
            setWeights.invoke(distribution, weights);
            // Force DDD to reapply its own enchantment/item modifiers on top of
            // the new base split, including modifiers already present on a stack.
            Object modifierNames = getModifiers.invoke(distribution);
            if (modifierNames instanceof Iterable) {
                for (Object name : new java.util.HashSet<Object>((java.util.Set<?>) modifierNames)) {
                    removeModifier.invoke(distribution, name);
                }
            }
            updateDistribution.invoke(distribution, stack);
            APPLIED.put(stack, signature);
        } catch (ReflectiveOperationException | RuntimeException error) {
            unavailable = true;
            NanonaitorsArsenal.LOGGER.warn(
                "Could not apply optional Distinct Damage Descriptions compatibility.", error);
        }
    }

    private static void initialize() throws ReflectiveOperationException {
        Class<?> api = Class.forName("yeelp.distinctdamagedescriptions.api.DDDAPI");
        Field accessorField = api.getField("accessor");
        accessor = accessorField.get(null);
        if (accessor == null) throw new IllegalStateException("DDD accessor is not initialized");
        getDistribution = accessor.getClass().getMethod("getDamageDistribution", ItemStack.class);

        Class<?> registries = Class.forName(
            "yeelp.distinctdamagedescriptions.registries.DDDRegistries");
        Object registry = registries.getField("damageTypes").get(null);
        if (registry == null) throw new IllegalStateException("DDD damage registry is not initialized");
        Method getType = registry.getClass().getMethod("get", String.class);
        for (String name : new String[] {"slashing", "bludgeoning", "radiant", "necrotic",
                "fire", "cold", "lightning", "poison"}) {
            Object type = getType.invoke(registry, name);
            if (type == null) throw new IllegalStateException("DDD damage type missing: " + name);
            DAMAGE_TYPES.put(name, type);
        }

        Class<?> distributionClass = Class.forName(
            "yeelp.distinctdamagedescriptions.capability.IDistribution");
        setBaseWeights = distributionClass.getMethod("setNewBaseWeights", Map.class);
        setWeights = distributionClass.getMethod("setNewWeights", Map.class);
        Class<?> updatableClass = Class.forName(
            "yeelp.distinctdamagedescriptions.capability.DDDUpdatableCapabilityBase");
        getModifiers = updatableClass.getMethod("getModifiers");
        removeModifier = updatableClass.getMethod("removeModifier", String.class);
        Class<?> damageDistributionClass = Class.forName(
            "yeelp.distinctdamagedescriptions.capability.IDamageDistribution");
        updateDistribution = damageDistributionClass.getMethod("update", ItemStack.class);
        initialized = true;
        NanonaitorsArsenal.LOGGER.info(
            "Enabled Distinct Damage Descriptions weapon distributions for Arsenal.");
    }

    private static Map<Object, Float> weights(ItemArsenalWeapon weapon) {
        float slashing;
        float bludgeoning;
        if (weapon instanceof ItemDoubleBladedScimitar || weapon instanceof ItemMorningStar) {
            slashing = 0.5F;
            bludgeoning = 0.5F;
        } else if (weapon instanceof ItemScimitar || weapon instanceof ItemClaws
                || weapon instanceof ItemLinkedClaw) {
            slashing = 1.0F;
            bludgeoning = 0.0F;
        } else if (weapon instanceof ItemBallAndChain || weapon instanceof ItemFlail
                || weapon instanceof ItemBatteringRam) {
            slashing = 0.0F;
            bludgeoning = 1.0F;
        } else {
            slashing = 1.0F;
            bludgeoning = 0.0F;
        }

        SpecialDamage special = specialDamage(weapon.getTier());
        float physicalShare = 1.0F - special.weight;
        Map<Object, Float> result = new HashMap<>();
        put(result, "slashing", slashing * physicalShare);
        put(result, "bludgeoning", bludgeoning * physicalShare);
        if (special.weight > 0.0F) put(result, special.type, special.weight);
        return result;
    }

    private static SpecialDamage specialDamage(WeaponTier tier) {
        switch (tier) {
            case SILVER: return new SpecialDamage("radiant", 0.10F);
            case UMBRIUM: return new SpecialDamage("necrotic", 0.10F);
            case FLAMED_DRAGONBONE: return new SpecialDamage("fire", 0.50F);
            case ICED_DRAGONBONE: return new SpecialDamage("cold", 0.50F);
            case ELECTRIC_DRAGONBONE: return new SpecialDamage("lightning", 0.50F);
            case DESERT_VENOM:
            case JUNGLE_VENOM: return new SpecialDamage("poison", 0.10F);
            default: return SpecialDamage.NONE;
        }
    }

    private static void put(Map<Object, Float> weights, String type, float value) {
        if (value > 0.0F) weights.put(DAMAGE_TYPES.get(type), value);
    }

    private static final class SpecialDamage {
        private static final SpecialDamage NONE = new SpecialDamage(null, 0.0F);
        private final String type;
        private final float weight;

        private SpecialDamage(String type, float weight) {
            this.type = type;
            this.weight = weight;
        }
    }
}
