package com.nanonaitor.arsenal.config;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraftforge.common.config.Config;

@Config(modid = NanonaitorsArsenal.MOD_ID, name = "nanonaitors_arsenal")
@Config.RequiresMcRestart
public final class ArsenalConfig {
    @Config.RequiresMcRestart
    public static Weapons weapons = new Weapons();
    public static Reach reach = new Reach();
    public static Effects effects = new Effects();
    public static Shields shields = new Shields();
    public static Stunned stunned = new Stunned();

    public static final class Weapons {
        @Config.Comment("Recipe switches only. False removes crafting/upgrade recipes; existing weapons remain registered, visible and usable. Restart required.")
        public boolean morningStar = true;
        public boolean scimitar = true;
        public boolean bladeStaff = true;
        public boolean claws = true;
        public boolean flail = true;
        public boolean batteringRam = true;
        public boolean ballAndChain = true;
        public boolean tartsyShield = true;
        public boolean sunWarBulwark = true;
    }
    public static final class Reach {
        @Config.RangeDouble(min=1D,max=64D)
        public double flailRadius = 4.0D;
        @Config.RangeDouble(min=1D,max=64D)
        public double ballWindupReach = 3.0D;
        @Config.RangeDouble(min=1D,max=64D)
        public double ballThrowReachPerCharge = 4.0D;
    }
    public static final class Shields {
        @Config.Comment("Allow shield-specific enchantments on Arsenal shields (including compatible modded shield types). Unbreaking/Mending remain allowed. Restart recommended.")
        public boolean allowShieldEnchantments = true;
    }
    public static final class Stunned {
        @Config.Comment("Entity registry IDs immune to Arsenal Stunned, e.g. minecraft:wither or modid:mob. Empty list means no extra immunities.")
        public String[] entityBlacklist = {};
    }
    public static final class Effects {
        @Config.Comment({"Effect lists: modid:potion@level@seconds, e.g. minecraft:weakness@2@10.",
            "Levels are 1-based. Use tier for the existing tier-dependent level/duration.",
            "Empty lists or blank entries apply no effect. Missing optional mod effects are skipped.",
            "These replace the named potion procs, not direct damage, armor piercing, fire or lightning."})
        public String[] scimitarHit = {"minecraft:weakness@tier@10"};
        public String[] morningStarFracture = {"nanonaitors_arsenal:armor_fracture@tier@tier"};
        public String[] morningStarStun = {"nanonaitors_arsenal:stunned@1@3"};
        public String[] ballPlayerFracture = {"nanonaitors_arsenal:armor_fracture@tier@10"};
        public String[] bladeStaffReflect = {"nanonaitors_arsenal:stunned@1@1"};
        public String[] tartsyDash = {"nanonaitors_arsenal:stunned@1@1"};
        public String[] venomHit = {"minecraft:poison@3@10"};
        public String[] icedDragonboneHit = {"minecraft:slowness@3@5", "minecraft:mining_fatigue@3@5"};
        public String[] livingMorningStar = {"srparasites:corrosive@tier@10"};
        public String[] livingClaws = {"srparasites:bleed@tier@10"};
        public String[] livingFlail = {"srparasites:antimall@tier@10"};
        public String[] livingBallAndChain = {"srparasites:debar@tier@10"};
        @Config.Comment("Additional successful-hit effects for each weapon family. Empty by default.")
        public String[] clawsHit = {};
        public String[] flailHit = {};
        public String[] ballAndChainHit = {};
        public String[] batteringRamHit = {};
        public String[] morningStarHit = {};
        public String[] bladeStaffHit = {};
    }
    @Config.Name("enchantments")
    public static Enchantments enchantments = new Enchantments();

    @Config.Name("morningStar")
    public static MorningStar morningStar = new MorningStar();

    @Config.Name("raceWeaponAffinity")
    public static RaceWeaponAffinity raceWeaponAffinity = new RaceWeaponAffinity();

    private ArsenalConfig() {}

    public static final class Enchantments {
        @Config.RequiresMcRestart public boolean enableLongChain = true;
        @Config.RequiresMcRestart public boolean enableRotationForce = true;
        @Config.RequiresMcRestart public boolean enableRecovery = true;
        @Config.RequiresMcRestart public boolean enableBreeched = true;
        @Config.Name("longChainMaxLevel")
        @Config.Comment("Maximum level of Long Chain. The natural default cap is 2.")
        @Config.RangeInt(min = 1, max = 100)
        public int longChainMaxLevel = 2;

        @Config.Name("rotationForceMaxLevel")
        @Config.Comment("Maximum level of Rotation Force. Each level adds 0.2 attack speed.")
        @Config.RangeInt(min = 1, max = 100)
        public int rotationForceMaxLevel = 2;
    }

    public static final class MorningStar {
        @Config.Name("fullChargeSeconds")
        @Config.Comment("Seconds required for a Morning Star to reach full charge. Each quarter adds 10% damage and widens the sweep.")
        @Config.RangeDouble(min = 0.25D, max = 60.0D)
        public double fullChargeSeconds = 2.0D;
    }

    public static final class RaceWeaponAffinity {
        @Config.Name("enabled")
        @Config.Comment({
            "Enables race weapon affinities when Trinkets and Baubles/XAT is installed.",
            "Does nothing when XAT is absent."
        })
        public boolean enabled = true;

        @Config.Name("damageMultiplier")
        @Config.Comment("Matching-race damage multiplier. 1.25 means +25% damage.")
        @Config.RangeDouble(min = 1.0D, max = 100.0D)
        public double damageMultiplier = 1.25D;

        @Config.Name("minimumBonusDamage")
        @Config.Comment("Minimum damage added by a matching affinity when 25% would be smaller.")
        @Config.RangeDouble(min = 0.0D, max = 1000.0D)
        public double minimumBonusDamage = 3.0D;
    }
}
