package com.nanonaitor.arsenal.enchantment;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalShield;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/** Curse counterpart to Recovery: Arsenal shield cooldowns take twice as long. */
public final class EnchantmentBreeched extends Enchantment {
    public EnchantmentBreeched() {
        super(Rarity.VERY_RARE, EnumEnchantmentType.BREAKABLE,
            new EntityEquipmentSlot[] {EntityEquipmentSlot.MAINHAND,
                EntityEquipmentSlot.OFFHAND});
        setRegistryName(NanonaitorsArsenal.MOD_ID, "breeched");
        setName(NanonaitorsArsenal.MOD_ID + ".breeched");
    }

    @Override public boolean canApply(ItemStack stack) {
        return stack.getItem() instanceof ItemArsenalShield;
    }

    @Override public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return false;
    }

    @Override protected boolean canApplyTogether(Enchantment other) {
        return !(other instanceof EnchantmentRecovery) && super.canApplyTogether(other);
    }

    @Override public boolean isCurse() { return true; }
    @Override public boolean isTreasureEnchantment() { return true; }
    @Override public int getMinEnchantability(int level) { return 25; }
    @Override public int getMaxEnchantability(int level) { return 50; }
    @Override public int getMaxLevel() { return 1; }
}
