package com.nanonaitor.arsenal.enchantment;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalShield;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/** Halves the disabled/cooldown period of Arsenal shields. */
public final class EnchantmentRecovery extends Enchantment {
    public EnchantmentRecovery() {
        super(Rarity.RARE, EnumEnchantmentType.BREAKABLE,
            new EntityEquipmentSlot[] {EntityEquipmentSlot.MAINHAND,
                EntityEquipmentSlot.OFFHAND});
        setRegistryName(NanonaitorsArsenal.MOD_ID, "recovery");
        setName(NanonaitorsArsenal.MOD_ID + ".recovery");
    }

    @Override public boolean canApply(ItemStack stack) {
        return com.nanonaitor.arsenal.config.ArsenalConfig.shields.allowShieldEnchantments
            && stack.getItem() instanceof ItemArsenalShield;
    }

    @Override public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return canApply(stack);
    }

    @Override protected boolean canApplyTogether(Enchantment other) {
        return !(other instanceof EnchantmentBreeched) && super.canApplyTogether(other);
    }

    @Override public int getMinEnchantability(int level) { return 20; }
    @Override public int getMaxEnchantability(int level) { return 50; }
    @Override public int getMaxLevel() { return 1; }
}
