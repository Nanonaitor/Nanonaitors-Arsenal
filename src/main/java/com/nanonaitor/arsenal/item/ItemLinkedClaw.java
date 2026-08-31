package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.client.ArsenalTooltip;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

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
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               ITooltipFlag flag) {
        if (!ArsenalTooltip.begin(tooltip, TextFormatting.GOLD,
                "The automatically managed half of a paired Claw.")) return;
        tooltip.add(TextFormatting.GRAY + "Copies the main Claw's durability, enchantments, and quality.");
        tooltip.add(TextFormatting.DARK_GRAY + "Disappears when its matching main Claw is unequipped.");
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
