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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
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
    private static Object itemConfiguration;
    private static Method configurationPut;
    private static Method configurationConfigured;
    private static Constructor<?> damageDistributionConstructor;
    private static Class<?> defaultDistributionClass;

    private DistinctDamageCompat() {}

    public static boolean apply(ItemStack stack, ItemArsenalWeapon weapon) {
        if (stack.isEmpty() || !Loader.isModLoaded(MOD_ID) || unavailable) return false;
        String signature = weapon.getClass().getName() + ':' + weapon.getTier().getId();
        if (signature.equals(APPLIED.get(stack))) return true;
        try {
            if (!initialized) initialize();
            if (unavailable) return false;
            Object optional = getDistribution.invoke(accessor, stack);
            if (!(optional instanceof Optional) || !((Optional<?>) optional).isPresent()) return false;
            Object distribution = ((Optional<?>) optional).get();
            // DDD assigns its immutable NORMAL singleton to unconfigured items.
            // Existing/early-created stacks can retain it even after Arsenal's
            // definitions are registered; the tooltip handler supplies a visual
            // fallback for those stacks rather than trying to mutate the singleton.
            if (defaultDistributionClass.isInstance(distribution)) return false;
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
            return true;
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof UnsupportedOperationException) return false;
            unavailable = true;
            NanonaitorsArsenal.LOGGER.warn(
                "Could not apply optional Distinct Damage Descriptions compatibility.", error);
            return false;
        } catch (ReflectiveOperationException | RuntimeException error) {
            unavailable = true;
            NanonaitorsArsenal.LOGGER.warn(
                "Could not apply optional Distinct Damage Descriptions compatibility.", error);
            return false;
        }
    }

    /** Registers mutable DDD item definitions before normal gameplay stacks are created. */
    public static void registerDefinitions() {
        if (!Loader.isModLoaded(MOD_ID) || unavailable) return;
        try {
            if (!initialized) initialize();
            int registered = 0;
            for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
                if (!(item instanceof ItemArsenalWeapon) || item.getRegistryName() == null) continue;
                String registryName = item.getRegistryName().toString();
                if ((Boolean) configurationConfigured.invoke(itemConfiguration, registryName)) continue;
                Object distribution = damageDistributionConstructor.newInstance(
                    weights((ItemArsenalWeapon) item));
                Object added = configurationPut.invoke(itemConfiguration,
                    registryName, distribution);
                if (!(added instanceof Boolean) || (Boolean) added) registered++;
            }
            NanonaitorsArsenal.LOGGER.info(
                "Registered {} Arsenal weapon distributions with DDD.", registered);
        } catch (ReflectiveOperationException | RuntimeException error) {
            unavailable = true;
            NanonaitorsArsenal.LOGGER.warn(
                "Could not register Arsenal weapon distributions with DDD.", error);
        }
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(MOD_ID);
    }

    /** Tooltip fallback for immutable DDD default capabilities on early-created stacks. */
    public static void addFallbackTooltip(List<String> tooltip, ItemArsenalWeapon weapon) {
        for (Map.Entry<String, Float> entry : namedWeights(weapon).entrySet()) {
            tooltip.add(color(entry.getKey()) + formatName(entry.getKey()) + ": "
                + Math.round(entry.getValue() * 100.0F) + "%");
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
            // DDD's public names are prefixed internally even though its config
            // accepts short physical aliases such as s/b/p.
            Object type = getType.invoke(registry, "ddd_" + name);
            if (type == null) type = getType.invoke(registry, name);
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
        defaultDistributionClass = Class.forName(
            "yeelp.distinctdamagedescriptions.capability.IDefaultDistribution");

        Class<?> configurations = Class.forName(
            "yeelp.distinctdamagedescriptions.config.DDDConfigurations");
        itemConfiguration = configurations.getField("items").get(null);
        if (itemConfiguration == null) throw new IllegalStateException("DDD item config is not initialized");
        Class<?> configurationInterface = Class.forName(
            "yeelp.distinctdamagedescriptions.config.IDDDConfiguration");
        configurationPut = configurationInterface.getMethod("put", String.class, Object.class);
        configurationConfigured = configurationInterface.getMethod("configured", String.class);
        Class<?> mutableDistribution = Class.forName(
            "yeelp.distinctdamagedescriptions.capability.impl.DamageDistribution");
        damageDistributionConstructor = mutableDistribution.getConstructor(Map.class);
        initialized = true;
        NanonaitorsArsenal.LOGGER.info(
            "Enabled Distinct Damage Descriptions weapon distributions for Arsenal.");
    }

    private static Map<Object, Float> weights(ItemArsenalWeapon weapon) {
        Map<Object, Float> result = new LinkedHashMap<>();
        for (Map.Entry<String, Float> entry : namedWeights(weapon).entrySet()) {
            put(result, entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static Map<String, Float> namedWeights(ItemArsenalWeapon weapon) {
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
        Map<String, Float> result = new LinkedHashMap<>();
        putNamed(result, "slashing", slashing * physicalShare);
        putNamed(result, "bludgeoning", bludgeoning * physicalShare);
        if (special.weight > 0.0F) putNamed(result, special.type, special.weight);
        return result;
    }

    private static void putNamed(Map<String, Float> weights, String type, float value) {
        if (value > 0.0F) weights.put(type, value);
    }

    private static String formatName(String type) {
        return Character.toUpperCase(type.charAt(0)) + type.substring(1) + " Damage";
    }

    private static TextFormatting color(String type) {
        if ("slashing".equals(type)) return TextFormatting.RED;
        if ("bludgeoning".equals(type)) return TextFormatting.GOLD;
        if ("radiant".equals(type)) return TextFormatting.YELLOW;
        if ("necrotic".equals(type)) return TextFormatting.DARK_PURPLE;
        if ("fire".equals(type)) return TextFormatting.DARK_RED;
        if ("cold".equals(type)) return TextFormatting.AQUA;
        if ("lightning".equals(type)) return TextFormatting.LIGHT_PURPLE;
        if ("poison".equals(type)) return TextFormatting.DARK_GREEN;
        return TextFormatting.GRAY;
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
