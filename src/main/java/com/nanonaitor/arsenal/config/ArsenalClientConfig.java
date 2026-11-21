package com.nanonaitor.arsenal.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only look and sound options; each player sets their own. Changes apply immediately. */
public final class ArsenalClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue WEAPON_POSES, ATTACK_SPEED_SWINGS, CHAIN_MATERIAL_COLORS;
    public static final ForgeConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("animations");
        WEAPON_POSES = BUILDER.comment("Animate the player's arms for Arsenal weapons in third person:",
                "two-handed holds, ramming, flail and ball-and-chain spins, blade staff paddling, claw rakes,",
                "scimitar guard and bash, Morning Star charge, and one-handed variants of two-handed weapons.")
            .define("weaponPoses", true);
        ATTACK_SPEED_SWINGS = BUILDER.comment(
                "Stretch Arsenal weapon swings to each weapon's real attack cooldown instead of vanilla's fixed swing.")
            .define("attackSpeedSwings", true);
        CHAIN_MATERIAL_COLORS = BUILDER.comment(
                "Draw flail and ball-and-chain chains in each tier's material instead of realistic iron.",
                "Off (default): every chain is dark iron; Living and Sentient chains are red (tendon).")
            .define("chainMaterialColors", false);
        BUILDER.pop();
        BUILDER.push("sounds");
        SOUND_VOLUME = BUILDER.comment(
                "Multiplier for Arsenal's own weapon sounds (chains, ram, claws, bashes, reflects). 1 = normal, 0 = silent.")
            .defineInRange("volume", 1.0D, 0.0D, 2.0D);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /** Reads a value, falling back to its default before the config file has loaded. */
    public static boolean get(ForgeConfigSpec.BooleanValue value) {
        try { return value.get(); } catch (IllegalStateException notLoaded) { return value.getDefault(); }
    }

    public static double get(ForgeConfigSpec.DoubleValue value) {
        try { return value.get(); } catch (IllegalStateException notLoaded) { return value.getDefault(); }
    }

    private ArsenalClientConfig() {}
}
