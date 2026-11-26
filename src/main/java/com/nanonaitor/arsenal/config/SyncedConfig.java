package com.nanonaitor.arsenal.config;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Combat settings the client must agree on with the server (reach, charge time,
 * enchantment switches and caps, the scimitar counter). The server sends its
 * values when a player joins and whenever the config reloads; while connected,
 * the client uses them instead of its own file, so players no longer have to
 * copy the server's config to see and predict attacks correctly.
 */
public final class SyncedConfig {
    /** Server values received over the network; null while not connected to one. */
    private static volatile Map<String, Double> overrides;

    /** Every synced value, read from this side's own config file. */
    public static Map<String, Double> snapshot() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("flailReach", ArsenalConfig.FLAIL_REACH.get());
        values.put("ballWindupReach", ArsenalConfig.BALL_WINDUP_REACH.get());
        values.put("ballThrowReach", ArsenalConfig.BALL_THROW_REACH.get());
        values.put("morningStarSeconds", ArsenalConfig.MORNING_STAR_CHARGE_SECONDS.get());
        values.put("longChainMax", (double)ArsenalConfig.LONG_CHAIN_MAX_LEVEL.get());
        values.put("rotationForceMax", (double)ArsenalConfig.ROTATION_FORCE_MAX_LEVEL.get());
        values.put("counterMultiplier", ArsenalConfig.SCIMITAR_COUNTER_MULTIPLIER.get());
        values.put("counterSeconds", (double)ArsenalConfig.SCIMITAR_COUNTER_SECONDS.get());
        values.put("shieldEnchantments", ArsenalConfig.SHIELD_ENCHANTMENTS.get() ? 1.0D : 0.0D);
        values.put("enchantments", ArsenalConfig.ENABLE_CUSTOM_ENCHANTMENTS.get() ? 1.0D : 0.0D);
        ArsenalConfig.ENCHANTMENTS.forEach((id, value) -> values.put("enchantment." + id, value.get() ? 1.0D : 0.0D));
        return values;
    }

    /** Client: the connected server's values (null clears them on disconnect). */
    public static void receive(Map<String, Double> values) {
        overrides = values == null ? null : Map.copyOf(values);
    }

    public static boolean active() {
        return overrides != null;
    }

    private static double value(String key, double local) {
        Map<String, Double> map = overrides;
        Double synced = map == null ? null : map.get(key);
        return synced == null ? local : synced;
    }

    public static double get(String key, ForgeConfigSpec.DoubleValue local) { return value(key, local.get()); }

    public static int get(String key, ForgeConfigSpec.IntValue local) { return (int)Math.round(value(key, local.get())); }

    public static boolean get(String key, ForgeConfigSpec.BooleanValue local) { return value(key, local.get() ? 1.0D : 0.0D) >= 0.5D; }

    private SyncedConfig() {}
}
