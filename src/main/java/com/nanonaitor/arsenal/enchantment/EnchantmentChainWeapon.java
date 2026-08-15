package com.nanonaitor.arsenal.enchantment;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemFlail;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

abstract class EnchantmentChainWeapon extends Enchantment {
    EnchantmentChainWeapon(Rarity rarity, String id) {
        super(rarity, EnumEnchantmentType.WEAPON,
            new EntityEquipmentSlot[] {EntityEquipmentSlot.MAINHAND});
        setRegistryName(NanonaitorsArsenal.MOD_ID, id);
        setName(NanonaitorsArsenal.MOD_ID + "." + id);
    }

    @Override
    public boolean canApply(ItemStack stack) {
        return stack.getItem() instanceof ItemFlail
            || stack.getItem() instanceof ItemBallAndChain;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return canApply(stack);
    }
}
