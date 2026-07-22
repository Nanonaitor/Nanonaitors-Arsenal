package com.nanonaitor.arsenal.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class ItemLinkedClaw extends ItemArsenalWeapon {
    public ItemLinkedClaw(WeaponTier tier) {
        super(tier, "linked_claw", tier.getClawAttackDamage() - 1.0D, -1.6D);
        setCreativeTab(null);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, EntityPlayer player) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
