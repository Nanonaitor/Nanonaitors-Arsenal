package com.nanonaitor.arsenal.config;

import net.minecraft.item.Item;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
public final class ContentSwitches {
    private ContentSwitches() {}
    public static boolean enabled(Item item) {
        return item != null && item.getRegistryName() != null && enabled(item.getRegistryName().getResourcePath());
    }
    public static boolean enabled(String id) {
        ArsenalConfig.Weapons w = ArsenalConfig.weapons;
        if (id.startsWith("morning_star")) return w.morningStar;
        if (id.startsWith("blade_staff")) return w.bladeStaff;
        if (id.startsWith("scimitar")) return w.scimitar;
        if (id.startsWith("claws") || id.startsWith("linked_claw")) return w.claws;
        if (id.startsWith("flail")) return w.flail;
        if (id.startsWith("battering_ram")) return w.batteringRam;
        if (id.startsWith("ball_and_chain")) return w.ballAndChain;
        if (id.startsWith("tartsy_shield")) return w.tartsyShield;
        if (id.startsWith("sun_war_bulwark")) return w.sunWarBulwark;
        return true;
    }

    /**
     * Enchantment IDs are always registered so disabling one never damages an
     * existing save. This method controls whether it can be obtained or have
     * an effect in the current configuration.
     */
    public static boolean enchantmentEnabled(Enchantment enchantment) {
        return enchantment != null && enchantment.getRegistryName() != null
            && enchantmentEnabled(enchantment.getRegistryName().getResourcePath());
    }

    public static boolean enchantmentEnabled(String id) {
        ArsenalConfig.Enchantments e = ArsenalConfig.enchantments;
        if (!e.enableAll) return false;
        if ("long_chain".equals(id)) return e.enableLongChain;
        if ("rotation_force".equals(id)) return e.enableRotationForce;
        if ("recovery".equals(id)) return e.enableRecovery;
        if ("breeched".equals(id)) return e.enableBreeched;
        if ("reverso".equals(id)) return e.enableReverso;
        if ("luna".equals(id)) return e.enableLuna;
        if ("magic_log".equals(id)) return e.enableMagicLog;
        if ("endure".equals(id)) return e.enableEndure;
        return true;
    }

    public static int enabledEnchantmentLevel(Enchantment enchantment, ItemStack stack) {
        return enchantmentEnabled(enchantment)
            ? EnchantmentHelper.getEnchantmentLevel(enchantment, stack) : 0;
    }
}
