package com.nanonaitor.arsenal.config;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraftforge.common.config.Config;

@Config(modid = NanonaitorsArsenal.MOD_ID, name = "nanonaitors_arsenal")
public final class ArsenalConfig {
    @Config.Name("enchantments")
    public static Enchantments enchantments = new Enchantments();

    @Config.Name("morningStar")
    public static MorningStar morningStar = new MorningStar();

    @Config.Name("raceWeaponAffinity")
    public static RaceWeaponAffinity raceWeaponAffinity = new RaceWeaponAffinity();

    private ArsenalConfig() {}

    public static final class Enchantments {
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
