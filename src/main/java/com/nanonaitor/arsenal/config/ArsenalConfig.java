package com.nanonaitor.arsenal.config;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraftforge.common.config.Config;

/**
 * Arsenal's settings, one section per weapon plus general and enchantment
 * sections. Every option has a comment, shown as the hover text in the
 * in-game config screen. Settings from the old layout (weapons, reach,
 * effects) are carried over once by {@link ArsenalConfigMigration}.
 */
@Config(modid = NanonaitorsArsenal.MOD_ID, name = "nanonaitors_arsenal")
public final class ArsenalConfig {
    private ArsenalConfig() {}

    private static final String EFFECT_FORMAT = "Effect list: modid:potion@level@seconds, e.g. minecraft:weakness@2@10. "
        + "Levels are 1-based; use tier for the tier-dependent level or duration. Empty = none. "
        + "Missing optional mod effects are skipped.";

    // ---- General ------------------------------------------------------------

    @Config.LangKey("config.nanonaitors_arsenal.worldEquipment")
    @Config.Comment("How Arsenal weapons appear in the world: loot tables and mobs.")
    public static WorldEquipment worldEquipment = new WorldEquipment();

    @Config.LangKey("config.nanonaitors_arsenal.animations")
    @Config.Comment("Client-side third-person weapon animations. Changes apply immediately.")
    public static Animations animations = new Animations();

    @Config.LangKey("config.nanonaitors_arsenal.sounds")
    @Config.Comment("Client-side: loudness of Arsenal's own weapon sounds.")
    public static Sounds sounds = new Sounds();

    @Config.LangKey("config.nanonaitors_arsenal.stunned")
    @Config.Comment("The Stunned effect used by several weapons.")
    public static Stunned stunned = new Stunned();

    @Config.LangKey("config.nanonaitors_arsenal.materials")
    @Config.Comment("Hit effects that come from a weapon's material (any weapon of that tier).")
    public static Materials materials = new Materials();

    // ---- Weapons ------------------------------------------------------------

    @Config.LangKey("config.nanonaitors_arsenal.morningStar")
    @Config.Comment("Morning Star: hold attack to charge, release for a sweeping smash that fractures armour and stuns.")
    public static MorningStar morningStar = new MorningStar();

    @Config.LangKey("config.nanonaitors_arsenal.scimitars")
    @Config.Comment("Scimitars: dual-wield to guard and shield bash. Blocking while holding a scimitar empowers the next hit or bash.")
    public static Scimitars scimitars = new Scimitars();

    @Config.LangKey("config.nanonaitors_arsenal.bladeStaff")
    @Config.Comment("Blade Staff: two blades on a pole; use to reflect projectiles and attacks.")
    public static BladeStaff bladeStaff = new BladeStaff();

    @Config.LangKey("config.nanonaitors_arsenal.claws")
    @Config.Comment("Claws: fast paired strikes from both hands.")
    public static Claws claws = new Claws();

    @Config.LangKey("config.nanonaitors_arsenal.flail")
    @Config.Comment("Flail: hold attack to whirl it overhead, hitting everything around you.")
    public static Flail flail = new Flail();

    @Config.LangKey("config.nanonaitors_arsenal.batteringRam")
    @Config.Comment("Battering Ram: hold use to charge, breaking blocks and ramming mobs.")
    public static BatteringRam batteringRam = new BatteringRam();

    @Config.LangKey("config.nanonaitors_arsenal.ballAndChain")
    @Config.Comment("Ball and Chain: hold attack to spin it up, release to throw. Full-charge throws pierce armour.")
    public static BallAndChain ballAndChain = new BallAndChain();

    @Config.LangKey("config.nanonaitors_arsenal.tartsyShield")
    @Config.Comment("Tartsy Shield: a light shield with a stunning dash bash.")
    public static TartsyShield tartsyShield = new TartsyShield();

    @Config.LangKey("config.nanonaitors_arsenal.sunWarBulwark")
    @Config.Comment("Sun-War Bulwark: a heavy shield that can guard while your other hand is free.")
    public static SunWarBulwark sunWarBulwark = new SunWarBulwark();

    @Config.LangKey("config.nanonaitors_arsenal.shields")
    @Config.Comment("Settings shared by all Arsenal shields.")
    public static Shields shields = new Shields();

    // ---- Custom materials ---------------------------------------------------

    @Config.LangKey("config.nanonaitors_arsenal.customMaterials")
    @Config.Comment({"Your own weapon materials. Each entry adds that material's version of every enabled weapon,",
        "crafted like the iron ones with your ingredient, its metal tinted your colour. Up to 8. Restart required."})
    public static CustomMaterials customMaterials = new CustomMaterials();

    // ---- Enchantments -------------------------------------------------------

    @Config.LangKey("config.nanonaitors_arsenal.enchantments")
    @Config.Comment({"Arsenal's enchantments. Disabled ones stay registered so saves are safe,",
        "but cannot be obtained, applied or take effect. Restart required."})
    public static Enchantments enchantments = new Enchantments();

    // ---- Sections -----------------------------------------------------------

    public static final class WorldEquipment {
        @Config.Comment("Offer same-tier Arsenal alternatives in Forge loot tables containing vanilla/Spartan melee weapons. Only plain entries are swapped: named, NBT-set or attribute-set items (modpack legendaries) are never replaced. Socketed stays optional and handles its own loot rolls.")
        public boolean lootIntegration = true;
        @Config.Comment("Chance (0-1) that an eligible loot weapon becomes a same-tier Arsenal weapon.")
        @Config.RangeDouble(min = 0D, max = 1D)
        public double lootReplacementChance = 0.25D;
        @Config.Comment("Allow newly spawned vanilla humanoids already carrying a vanilla/Spartan melee weapon to use a same-tier Arsenal weapon. Does not equip empty hands, replace bows, or touch named mobs and named/lore/attribute gear.")
        public boolean mobEquipment = true;
        @Config.Comment("Chance (0-1) that an eligible mob's weapon becomes a same-tier Arsenal weapon.")
        @Config.RangeDouble(min = 0D, max = 1D)
        public double mobReplacementChance = 0.25D;
        @Config.Comment("If Socketed is installed, use its configured MOB_DROP socket roll for newly equipped Arsenal weapons. No effect without Socketed.")
        public boolean socketMobEquipment = true;
    }

    public static final class Animations {
        @Config.Comment({"Animate the player's arms for Arsenal weapons in third person:",
            "two-handed holds, ramming, flail and ball-and-chain spins, blade staff paddling,",
            "attack-speed-timed swings, and strained one-handed variants of two-handed weapons."})
        public boolean weaponPoses = true;
        @Config.Comment("With Mo' Bends installed, apply the same weapon poses through Mo' Bends' own skeleton instead of overriding it.")
        public boolean moBendsPoses = true;
        @Config.Comment("Stretch Arsenal weapon swing animations to each weapon's real attack cooldown instead of vanilla's fixed swing.")
        public boolean attackSpeedSwings = true;
        @Config.Comment({"Draw flail and ball-and-chain chains in each tier's material instead of realistic iron.",
            "Off (default): every chain is dark iron, as crafted from iron chain; Living and Sentient chains are red (tendon)."})
        public boolean chainMaterialColors = false;
    }

    public static final class Sounds {
        @Config.Comment("Multiplier for Arsenal's own weapon sounds (chains, ram, claws, bashes, reflects, armour cracks). 1 = normal, 0 = silent.")
        @Config.RangeDouble(min = 0.0D, max = 2.0D)
        public double volume = 1.0D;
    }

    public static final class Stunned {
        @Config.Comment("Entity registry IDs immune to Arsenal Stunned, e.g. minecraft:wither or modid:mob. Empty list means no extra immunities.")
        public String[] entityBlacklist = {};
    }

    public static final class Materials {
        @Config.Comment({"Venom-tier weapons: effects on every hit.", EFFECT_FORMAT})
        public String[] venomHit = {"minecraft:poison@3@10"};
        @Config.Comment({"Iced Dragonbone weapons: effects on every hit (they also freeze the target).", EFFECT_FORMAT})
        public String[] icedDragonboneHit = {"minecraft:slowness@3@5", "minecraft:mining_fatigue@3@5"};
    }

    public static final class MorningStar {
        @Config.Comment("Craftable and found as loot. False removes recipes and loot and hides it from Creative; existing items keep working. Restart required.")
        @Config.RequiresMcRestart
        public boolean enabled = true;
        @Config.Comment("Seconds required for a Morning Star to reach full charge. Each quarter adds 10% damage and widens the sweep.")
        @Config.RangeDouble(min = 0.25D, max = 60.0D)
        public double fullChargeSeconds = 2.0D;
        @Config.Comment({"Applied by charged smashes: armour fracture that stacks with each hit.", EFFECT_FORMAT})
        public String[] fractureEffects = {"nanonaitors_arsenal:armor_fracture@tier@tier"};
        @Config.Comment({"Applied by full-charge smashes.", EFFECT_FORMAT})
        public String[] stunEffects = {"nanonaitors_arsenal:stunned@1@3"};
        @Config.Comment({"Extra effects on every successful hit. Empty by default.", EFFECT_FORMAT})
        public String[] hitEffects = {};
        @Config.Comment({"Living and Sentient tiers (Scape and Run: Parasites): effect on hit.", EFFECT_FORMAT})
        public String[] livingEffects = {"srparasites:corrosive@tier@10"};
    }

    public static final class Scimitars {
        @Config.Comment("Craftable and found as loot. False removes recipes and loot and hides it from Creative; existing items keep working. Restart required.")
        @Config.RequiresMcRestart
        public boolean enabled = true;
        @Config.Comment("Damage multiplier for the next scimitar hit or shield bash after a block (the bash's combined dual-wield damage is multiplied).")
        @Config.RangeDouble(min = 1.0D, max = 5.0D)
        public double counterMultiplier = 1.3D;
        @Config.Comment("How long a block's counter stays ready before fading, in seconds.")
        @Config.RangeInt(min = 1, max = 60)
        public int counterWindowSeconds = 5;
        @Config.Comment({"Optional effects on every scimitar hit, empty by default (the old default was minecraft:weakness@tier@10).", EFFECT_FORMAT})
        public String[] hitEffects = {};
    }

    public static final class BladeStaff {
        @Config.Comment("Craftable and found as loot. False removes recipes and loot and hides it from Creative; existing items keep working. Restart required.")
        @Config.RequiresMcRestart
        public boolean enabled = true;
        @Config.Comment({"Applied to an attacker whose hit is reflected.", EFFECT_FORMAT})
        public String[] reflectEffects = {"nanonaitors_arsenal:stunned@1@1"};
        @Config.Comment({"Extra effects on every successful hit. Empty by default.", EFFECT_FORMAT})
        public String[] hitEffects = {};
    }

    public static final class Claws {
        @Config.Comment("Craftable and found as loot. False removes recipes and loot and hides it from Creative; existing items keep working. Restart required.")
        @Config.RequiresMcRestart
        public boolean enabled = true;
        @Config.Comment({"Extra effects on every successful hit. Empty by default.", EFFECT_FORMAT})
        public String[] hitEffects = {};
        @Config.Comment({"Living and Sentient tiers (Scape and Run: Parasites): effect on hit.", EFFECT_FORMAT})
        public String[] livingEffects = {"srparasites:bleed@tier@10"};
    }

    public static final class Flail {
        @Config.Comment("Craftable and found as loot. False removes recipes and loot and hides it from Creative; existing items keep working. Restart required.")
        @Config.RequiresMcRestart
        public boolean enabled = true;
        @Config.Comment("Base radius in blocks the whirling flail hits (Long Chain adds 1 per level).")
        @Config.RangeDouble(min = 1D, max = 64D)
        public double radius = 4.0D;
        @Config.Comment({"Extra effects on every successful hit. Empty by default.", EFFECT_FORMAT})
        public String[] hitEffects = {};
        @Config.Comment({"Living and Sentient tiers (Scape and Run: Parasites): effect on hit.", EFFECT_FORMAT})
        public String[] livingEffects = {"srparasites:antimall@tier@10"};
    }

    public static final class BatteringRam {
        @Config.Comment("Craftable and found as loot. False removes recipes and loot and hides it from Creative; existing items keep working. Restart required.")
        @Config.RequiresMcRestart
        public boolean enabled = true;
        @Config.Comment({"Extra effects on every successful hit. Empty by default.", EFFECT_FORMAT})
        public String[] hitEffects = {};
    }

    public static final class BallAndChain {
        @Config.Comment("Craftable and found as loot. False removes recipes and loot and hides it from Creative; existing items keep working. Restart required.")
        @Config.RequiresMcRestart
        public boolean enabled = true;
        @Config.Comment("Base reach in blocks of the spinning ball while winding up (Long Chain adds 1 per level).")
        @Config.RangeDouble(min = 1D, max = 64D)
        public double windupReach = 3.0D;
        @Config.Comment("Throw reach in blocks added per spin charge.")
        @Config.RangeDouble(min = 1D, max = 64D)
        public double throwReachPerCharge = 4.0D;
        @Config.Comment({"Full-charge throws against players: armour fracture effect (mobs lose armour directly).", EFFECT_FORMAT})
        public String[] playerFracture = {"nanonaitors_arsenal:armor_fracture@tier@10"};
        @Config.Comment({"Extra effects on every successful hit. Empty by default.", EFFECT_FORMAT})
        public String[] hitEffects = {};
        @Config.Comment({"Living and Sentient tiers (Scape and Run: Parasites): effect on hit.", EFFECT_FORMAT})
        public String[] livingEffects = {"srparasites:debar@tier@10"};
    }

    public static final class TartsyShield {
        @Config.Comment("Craftable and found as loot. False removes recipes and loot and hides it from Creative; existing items keep working. Restart required.")
        @Config.RequiresMcRestart
        public boolean enabled = true;
        @Config.Comment({"Applied to mobs hit by the dash bash.", EFFECT_FORMAT})
        public String[] dashEffects = {"nanonaitors_arsenal:stunned@1@1"};
    }

    public static final class SunWarBulwark {
        @Config.Comment("Craftable and found as loot. False removes recipes and loot and hides it from Creative; existing items keep working. Restart required.")
        @Config.RequiresMcRestart
        public boolean enabled = true;
    }

    public static final class Shields {
        @Config.Comment("Allow shield-specific enchantments on Arsenal shields (including compatible modded shield types). Unbreaking/Mending remain allowed. Restart recommended.")
        public boolean allowShieldEnchantments = true;
    }

    public static final class CustomMaterials {
        @Config.Comment({"One material per line: id, Display Name, #RRGGBB, repair ingredient, harvest level, durability, attack damage, enchantability",
            "Example: copper, Copper, #D9824B, ore:ingotCopper, 2, 300, 1.5, 14",
            "id: lowercase letters, digits, _ (used in item IDs: keep it once worlds use it). Display Name replaces \"Iron\" in item names.",
            "Repair ingredient: ore:oreDictName or modid:item. Harvest level 0-3 like wood-diamond: sets armour piercing, reflection and ram block breaking.",
            "Durability, attack damage and enchantability work like a vanilla tool material (iron: 2, 250, 2.0, 14). Lines starting with # are ignored."})
        @Config.RequiresMcRestart
        public String[] materials = {};
    }

    public static final class Enchantments {
        @Config.Comment("Master switch: false disables every Arsenal enchantment.")
        @Config.RequiresMcRestart public boolean enableAll = true;
        @Config.Comment("Long Chain (Flail, Ball and Chain): +1 block of reach per level.")
        @Config.RequiresMcRestart public boolean enableLongChain = true;
        @Config.Comment("Rotation Force (Flail, Ball and Chain): +0.2 attack speed per level, so spins and swings come faster.")
        @Config.RequiresMcRestart public boolean enableRotationForce = true;
        @Config.Comment("Recovery (Arsenal shields): halves the Tartsy Shield disable cooldown and the Sun-War Bulwark bash cooldown.")
        @Config.RequiresMcRestart public boolean enableRecovery = true;
        @Config.Comment("Breeched (curse, Arsenal shields): doubles the Tartsy Shield disable cooldown and the Sun-War Bulwark bash cooldown.")
        @Config.RequiresMcRestart public boolean enableBreeched = true;
        @Config.Comment("Reverso (Blade Staff): reflecting heals you instead and repairs the staff.")
        @Config.RequiresMcRestart public boolean enableReverso = true;
        @Config.Comment("Luna (Morning Star): +25% smash area and -25% charge time per level.")
        @Config.RequiresMcRestart public boolean enableLuna = true;
        @Config.Comment("Magic Log (Battering Ram): halves charge hunger use and turns its damage into magic damage.")
        @Config.RequiresMcRestart public boolean enableMagicLog = true;
        @Config.Comment("Endure (Scimitars): each level gives a 25% chance to resist a guard break and shortens the break by 25%.")
        @Config.RequiresMcRestart public boolean enableEndure = true;
        @Config.Comment("Maximum level of Long Chain. The natural default cap is 2.")
        @Config.RangeInt(min = 1, max = 100)
        public int longChainMaxLevel = 2;
        @Config.Comment("Maximum level of Rotation Force. Each level adds 0.2 attack speed; the default cap is 3.")
        @Config.RangeInt(min = 1, max = 100)
        public int rotationForceMaxLevel = 3;
    }
}
