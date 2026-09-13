package com.nanonaitor.arsenal.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ArsenalConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue LONG_CHAIN_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue ROTATION_FORCE_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MORNING_STAR_CHARGE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FLAIL_REACH, BALL_WINDUP_REACH, BALL_THROW_REACH;
    public static final ForgeConfigSpec.BooleanValue SHIELD_ENCHANTMENTS, MATCHING_ARMOR_BONUSES;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> STUN_BLACKLIST;
    public static final ForgeConfigSpec.IntValue EVOLUTION_THRESHOLD;
    public static final java.util.Map<String,ForgeConfigSpec.BooleanValue> WEAPONS=new java.util.LinkedHashMap<>(), ENCHANTMENTS=new java.util.LinkedHashMap<>();
    public static final java.util.Map<String,ForgeConfigSpec.ConfigValue<java.util.List<? extends String>>> EFFECTS=new java.util.LinkedHashMap<>();
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("enchantments");
        for(String id:new String[]{"long_chain","rotation_force","recovery","breeched"}) ENCHANTMENTS.put(id,BUILDER.define(id,true));
        LONG_CHAIN_MAX_LEVEL = BUILDER.comment(
            "Maximum effective Long Chain level. Natural enchanting is capped at level 2; this also caps higher levels supplied by commands or other mods.")
            .defineInRange("longChainMaxLevel", 2, 1, 100);
        ROTATION_FORCE_MAX_LEVEL = BUILDER.comment(
            "Maximum effective Rotation Force level. Natural enchanting is capped at level 2; this also caps higher levels supplied by commands or other mods.")
            .defineInRange("rotationForceMaxLevel", 2, 1, 100);
        BUILDER.pop();
        BUILDER.push("morningStar");
        MORNING_STAR_CHARGE_SECONDS = BUILDER.comment(
            "Seconds required for a Morning Star to reach full charge. The attack gains one 10% damage step per quarter charge.")
            .defineInRange("fullChargeSeconds", 2.0D, 0.25D, 60.0D);
        BUILDER.pop();
        BUILDER.push("weapons");
        for(String id:new String[]{"morning_star","scimitar","blade_staff","claws","flail","battering_ram","ball_and_chain","sun_war_bulwark","tartsy_shield"})
            WEAPONS.put(id,BUILDER.comment("False disables crafting only; existing items remain usable.").define(id,true));
        BUILDER.pop();
        BUILDER.push("reach");
        FLAIL_REACH=BUILDER.defineInRange("flail",4D,0.5D,32D);
        BALL_WINDUP_REACH=BUILDER.defineInRange("ballWindup",3D,0.5D,32D);
        BALL_THROW_REACH=BUILDER.defineInRange("ballThrowPerCharge",4D,0.5D,32D);
        BUILDER.pop();
        BUILDER.push("compatibility");
        SHIELD_ENCHANTMENTS=BUILDER.define("allowShieldEnchantments",true);
        MATCHING_ARMOR_BONUSES=BUILDER.comment("Rebuild the 1.12.2 gold/silver full-set bonuses using armor items/tags; no SetBonus mod required.").define("matchingArmorBonuses",true);
        STUN_BLACKLIST=BUILDER.comment("Exact entity IDs that cannot receive Stunned.").defineListAllowEmpty("stunnedEntityBlacklist",java.util.List.of(),v->v instanceof String);
        EVOLUTION_THRESHOLD=BUILDER.comment("Living weapons evolve after accumulating more than this much slain srparasites mob maximum health. No evolution occurs without parasite entities.").defineInRange("livingEvolutionHealth",1000,1,10000000);
        BUILDER.pop();
        BUILDER.push("effects");
        effect("scimitarHit","minecraft:weakness@tier@10");
        effect("morningStarFracture","nanonaitors_arsenal:armor_fracture@tier@tier");
        effect("morningStarStun","nanonaitors_arsenal:stunned@1@3");
        effect("ballPlayerFracture","nanonaitors_arsenal:armor_fracture@tier@10");
        effect("bladeStaffReflect","nanonaitors_arsenal:stunned@1@1");
        effect("tartsyDash","nanonaitors_arsenal:stunned@1@1");
        effect("venomHit","minecraft:poison@3@10");
        effect("icedDragonboneHit","minecraft:slowness@3@5","minecraft:mining_fatigue@3@5");
        effect("livingMorningStar","srparasites:corrosive@tier@10");
        effect("livingClaws","srparasites:bleed@tier@10");
        effect("livingFlail","srparasites:antimall@tier@10");
        effect("livingBallAndChain","srparasites:debar@tier@10");
        for(String id:new String[]{"morning_star","scimitar","blade_staff","claws","flail","battering_ram","ball_and_chain"}) effect(id+"Extra");
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private static void effect(String name,String... defaults){EFFECTS.put(name,BUILDER.comment("modid:effect@level@seconds; tier uses the weapon default. Empty list disables effects; unknown optional IDs are skipped.").defineListAllowEmpty(name,java.util.List.of(defaults),v->v instanceof String));}

    private ArsenalConfig() {}
}
