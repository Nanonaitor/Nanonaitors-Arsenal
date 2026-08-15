package com.nanonaitor.arsenal.config;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraftforge.common.config.Config;

@Config(modid = NanonaitorsArsenal.MOD_ID, name = "nanonaitors_arsenal")
public final class ArsenalConfig {
    @Config.Name("enchantments")
    public static Enchantments enchantments = new Enchantments();

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
}
