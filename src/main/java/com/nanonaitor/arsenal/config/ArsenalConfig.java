package com.nanonaitor.arsenal.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ArsenalConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue LONG_CHAIN_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue ROTATION_FORCE_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MORNING_STAR_CHARGE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FLAIL_REACH, BALL_WINDUP_REACH, BALL_THROW_REACH;
    public static final ForgeConfigSpec.BooleanValue SHIELD_ENCHANTMENTS, MATCHING_ARMOR_BONUSES, ENABLE_CUSTOM_ENCHANTMENTS, MOB_EQUIPMENT, LOOT_INTEGRATION;
    public static final ForgeConfigSpec.DoubleValue MOB_REPLACEMENT_CHANCE, LOOT_REPLACEMENT_CHANCE;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> STUN_BLACKLIST, PARASITE_MODS;
    public static final ForgeConfigSpec.IntValue EVOLUTION_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue CHAIN_LIGHTNING;
    public static final ForgeConfigSpec.IntValue CHAIN_LIGHTNING_RANGE;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends Double>> CHAIN_LIGHTNING_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SCIMITAR_COUNTER_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SCIMITAR_COUNTER_SECONDS;
    public static final java.util.Map<String,ForgeConfigSpec.BooleanValue> WEAPONS=new java.util.LinkedHashMap<>(), ENCHANTMENTS=new java.util.LinkedHashMap<>();
    public static final java.util.Map<String,ForgeConfigSpec.ConfigValue<java.util.List<? extends String>>> EFFECTS=new java.util.LinkedHashMap<>();
    public static final ForgeConfigSpec SPEC;

    private static final java.util.Map<String,String> ENCHANT_INFO=java.util.Map.of(
        "long_chain","Long Chain: +1 block of Flail and Ball and Chain reach per level.",
        "rotation_force","Rotation Force: +0.2 Flail and Ball and Chain attack speed per level.",
        "recovery","Recovery: halves the ability cooldown of the Tartsy Shield, Sun-War Bulwark and Blade Staffs.",
        "breeched","Breeched (curse): doubles Tartsy Shield and Sun-War Bulwark ability cooldowns.",
        "reverso","Reverso: Blade Staff reflection heals you instead and repairs the staff.",
        "luna","Luna: +25% Morning Star area and -25% charge time per level.",
        "magic_log","Magic Log: halves Battering Ram hunger use and turns its damage into magic.",
        "endure","Endure: Scimitars resist guard breaks and recover from a failed resistance faster.");

    static {
        BUILDER.push("enchantments");
        ENABLE_CUSTOM_ENCHANTMENTS=BUILDER.comment("Master switch for every Nanonaitor's Arsenal enchantment.").define("enabled",true);
        for(String id:new String[]{"long_chain","rotation_force","recovery","breeched","reverso","luna","magic_log","endure"}) ENCHANTMENTS.put(id,BUILDER.comment(ENCHANT_INFO.get(id)+" False removes it from enchanting tables and books; enchantments already on items stop working.").define(id,true));
        LONG_CHAIN_MAX_LEVEL = BUILDER.comment(
            "Maximum effective Long Chain level. Natural enchanting is capped at level 2; this also caps higher levels supplied by commands or other mods.")
            .defineInRange("longChainMaxLevel", 2, 1, 100);
        ROTATION_FORCE_MAX_LEVEL = BUILDER.comment(
            "Maximum effective Rotation Force level. Natural enchanting is capped at level 3; this also caps higher levels supplied by commands or other mods.")
            .defineInRange("rotationForceMaxLevel", 3, 1, 100);
        BUILDER.pop();
        BUILDER.push("morningStar");
        MORNING_STAR_CHARGE_SECONDS = BUILDER.comment(
            "Seconds required for a Morning Star to reach full charge. The attack gains one 10% damage step per quarter charge.")
            .defineInRange("fullChargeSeconds", 2.0D, 0.25D, 60.0D);
        BUILDER.pop();
        BUILDER.push("scimitars");
        SCIMITAR_COUNTER_MULTIPLIER=BUILDER.comment(
            "Scimitar counter: blocking an attack while holding a scimitar (dual-scimitar guard or any shield) multiplies the next scimitar hit or shield bash by this. The bash multiplies its combined dual-wield damage.")
            .defineInRange("counterMultiplier",1.3D,1.0D,5.0D);
        SCIMITAR_COUNTER_SECONDS=BUILDER.comment("Seconds a block's counter stays ready before fading.")
            .defineInRange("counterWindowSeconds",5,1,60);
        BUILDER.pop();
        BUILDER.push("weapons");
        for(String id:new String[]{"morning_star","scimitar","blade_staff","claws","flail","battering_ram","ball_and_chain","sun_war_bulwark","tartsy_shield"})
            WEAPONS.put(id,BUILDER.comment("False disables crafting only; existing items remain usable.").define(id,true));
        BUILDER.pop();
        BUILDER.push("worldEquipment");
        LOOT_INTEGRATION=BUILDER.comment("Let plain swords and axes rolled by loot tables (chests and other loot) become a same-tier Arsenal weapon. Named, lore, attribute or otherwise customized loot items are never replaced.").define("lootIntegration",true);
        LOOT_REPLACEMENT_CHANCE=BUILDER.comment("Chance (0-1) that an eligible loot weapon becomes the Arsenal one.").defineInRange("lootReplacementChance",.25D,0D,1D);
        MOB_EQUIPMENT=BUILDER.comment("Allow newly spawned vanilla humanoids already carrying a vanilla melee weapon to use a same-tier Arsenal weapon. Named mobs and named, lore, attribute or otherwise customized gear are never replaced.").define("mobEquipment",true);
        MOB_REPLACEMENT_CHANCE=BUILDER.comment("Chance (0-1) that an eligible mob swaps its weapon for the Arsenal one.").defineInRange("mobReplacementChance",.25D,0D,1D);
        BUILDER.pop();
        BUILDER.push("reach");
        FLAIL_REACH=BUILDER.comment("Blocks the Flail's spinning head reaches while swung.").defineInRange("flail",4D,0.5D,32D);
        BALL_WINDUP_REACH=BUILDER.comment("Blocks the Ball and Chain hits while it spins around you (wind-up).").defineInRange("ballWindup",3D,0.5D,32D);
        BALL_THROW_REACH=BUILDER.comment("Blocks a Ball and Chain throw travels per full spin of charge; more spins throw farther.").defineInRange("ballThrowPerCharge",4D,0.5D,32D);
        BUILDER.pop();
        BUILDER.push("compatibility");
        SHIELD_ENCHANTMENTS=BUILDER.comment("Let the Sun War Bulwark and Tartsy Shield take shield enchantments from other mods.").define("allowShieldEnchantments",true);
        MATCHING_ARMOR_BONUSES=BUILDER.comment("Rebuild the 1.12.2 gold/silver full-set bonuses using armor items/tags; no SetBonus mod required.").define("matchingArmorBonuses",true);
        STUN_BLACKLIST=BUILDER.comment("Exact entity IDs that cannot receive Stunned.").defineListAllowEmpty("stunnedEntityBlacklist",java.util.List.of(),v->v instanceof String);
        CHAIN_LIGHTNING=BUILDER.comment("Electric Dragonbone hits arc chain lightning to nearby enemies, as Ice & Fire's 1.12.2 lightning weapons did.").define("electricChainLightning",true);
        CHAIN_LIGHTNING_RANGE=BUILDER.comment("Blocks the chain lightning can jump between enemies.").defineInRange("chainLightningRange",8,1,32);
        CHAIN_LIGHTNING_DAMAGE=BUILDER.comment("Armor-bypassing lightning damage per hop, starting with the struck target; one entry per hop.")
            .defineListAllowEmpty("chainLightningDamage",java.util.List.of(5.0D,4.0D,3.0D,2.0D,1.0D),v->v instanceof Number n && n.doubleValue()>=0);
        EVOLUTION_THRESHOLD=BUILDER.comment("Living weapons evolve after accumulating more than this much slain parasite mob maximum health (see parasiteMods). No evolution occurs without parasite entities.").defineInRange("livingEvolutionHealth",1000,1,10000000);
        PARASITE_MODS=BUILDER.comment("Mod ids whose mobs count as parasites for Living and Sentient weapons: their kills evolve Living weapons, and the Living Ball and Chain's effect only hits them. srparasites is Scape and Run: Parasites, spore is Fungal Infection: Spore.")
            .defineListAllowEmpty("parasiteMods",java.util.List.of("srparasites","spore"),v->v instanceof String);
        BUILDER.pop();
        BUILDER.push("effects");
        // Optional effects on each scimitar hit. Empty by default: the scimitar's ability is the
        // block counter (see scimitars). The old default was minecraft:weakness@tier@10.
        effect("scimitarHitExtra");
        effect("morningStarFracture","nanonaitors_arsenal:armor_fracture@tier@tier");
        effect("morningStarStun","nanonaitors_arsenal:stunned@1@3");
        effect("ballPlayerFracture","nanonaitors_arsenal:armor_fracture@tier@10");
        effect("bladeStaffReflect","nanonaitors_arsenal:stunned@1@1");
        effect("tartsyDash","nanonaitors_arsenal:stunned@1@1");
        effect("venomHit","minecraft:poison@3@10");
        effect("icedDragonboneHit","minecraft:slowness@3@5","minecraft:mining_fatigue@3@5");
        // Each list may name effects from several mods; effects whose mod is absent are skipped.
        effect("livingMorningStar","srparasites:corrosive@tier@10","spore:corrosion@tier@10");
        effect("livingClaws","srparasites:bleed@tier@10","spore:hemorrhage@tier@10");
        effect("livingFlail","srparasites:antimall@tier@10");
        effect("livingBallAndChain","srparasites:debar@tier@10");
        for(String id:new String[]{"morning_star","scimitar","blade_staff","claws","flail","battering_ram","ball_and_chain"}) effect(id+"Extra");
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private static void effect(String name,String... defaults){EFFECTS.put(name,BUILDER.comment("modid:effect@level@seconds; tier uses the weapon default. Empty list disables effects; unknown optional IDs are skipped.").defineListAllowEmpty(name,java.util.List.of(defaults),v->v instanceof String));}

    private ArsenalConfig() {}
}
