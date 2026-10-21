package com.nanonaitor.arsenal.enchantment;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import java.util.function.Predicate;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/** A small configurable exclusive-enchantment implementation for Arsenal weapons. */
public final class EnchantmentWeaponExclusive extends Enchantment {
    private final Predicate<ItemStack> weapon;
    private final int maxLevel;

    public EnchantmentWeaponExclusive(String id, int maxLevel, Predicate<ItemStack> weapon) {
        super(Rarity.RARE, EnumEnchantmentType.WEAPON,
            new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND, EntityEquipmentSlot.OFFHAND});
        this.weapon = weapon;
        this.maxLevel = maxLevel;
        setRegistryName(NanonaitorsArsenal.MOD_ID, id);
        setName(NanonaitorsArsenal.MOD_ID + "." + id);
    }

    @Override public boolean canApply(ItemStack stack) { return weapon.test(stack); }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack) { return canApply(stack); }
    @Override public int getMinEnchantability(int level) { return 15 + (level - 1) * 15; }
    @Override public int getMaxEnchantability(int level) { return getMinEnchantability(level) + 30; }
    @Override public int getMaxLevel() { return maxLevel; }
}
