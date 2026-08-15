package com.nanonaitor.arsenal.enchantment;

import com.nanonaitor.arsenal.config.ArsenalConfig;

public final class EnchantmentLongChain extends EnchantmentChainWeapon {
    public EnchantmentLongChain() {
        super(Rarity.UNCOMMON, "long_chain");
    }

    @Override
    public int getMinEnchantability(int level) {
        return 10 + (level - 1) * 10;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 25;
    }

    @Override
    public int getMaxLevel() {
        return ArsenalConfig.enchantments.longChainMaxLevel;
    }
}
