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

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack,
                                                ItemStack newStack,
                                                boolean slotChanged) {
        // Durability, enchantment, and Quality data are mirrored from the main
        // claw. Those synchronization-only NBT changes must not restart the
        // first-person equip animation every tick.
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
