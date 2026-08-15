package com.nanonaitor.arsenal.enchantment;

import com.nanonaitor.arsenal.config.ArsenalConfig;

public final class EnchantmentRotationForce extends EnchantmentChainWeapon {
    public EnchantmentRotationForce() {
        super(Rarity.RARE, "rotation_force");
    }

    @Override
    public int getMinEnchantability(int level) {
        return 15 + (level - 1) * 12;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 25;
    }

    @Override
    public int getMaxLevel() {
        return ArsenalConfig.enchantments.rotationForceMaxLevel;
    }
}
