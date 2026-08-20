package com.nanonaitor.arsenal.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ArsenalConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue LONG_CHAIN_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue ROTATION_FORCE_MAX_LEVEL;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("enchantments");
        LONG_CHAIN_MAX_LEVEL = BUILDER.comment(
            "Maximum effective Long Chain level. Natural enchanting is capped by the enchantment data at level 2; datapacks may raise it.")
            .defineInRange("longChainMaxLevel", 2, 1, 100);
        ROTATION_FORCE_MAX_LEVEL = BUILDER.comment(
            "Maximum effective Rotation Force level. Natural enchanting is capped by the enchantment data at level 2; datapacks may raise it.")
            .defineInRange("rotationForceMaxLevel", 2, 1, 100);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ArsenalConfig() {}
}
