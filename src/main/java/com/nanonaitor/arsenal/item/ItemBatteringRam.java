package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ItemBatteringRam extends ItemArsenalWeapon {
    public ItemBatteringRam(WeaponTier tier) {
        super(tier, "battering_ram", 7.0D + tier.getMaterial().getAttackDamage(),
            tier == WeaponTier.GOLD ? -3.0D : -3.6D);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BLOCK;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        return !ArsenalCompatManager.canUseTwoHanded(player);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               ITooltipFlag flag) {
        tooltip.add(TextFormatting.RED + "Two-Handed");
        tooltip.add(TextFormatting.GOLD + "Hold left-click to charge forward.");
        tooltip.add(TextFormatting.GRAY + getBreakDescription());
        tooltip.add(TextFormatting.GRAY + "Costs 1 durability per block or enemy hit.");
        tooltip.add(TextFormatting.DARK_GRAY + "Requires an empty offhand (XAT Titans exempt).");
    }

    private String getBreakDescription() {
        int level = getTier().getRamBreakLevel();
        if (level <= 0) return "Breaks 3x3 soft soil.";
        if (level == 1) return (getTier() == WeaponTier.GOLD ? "Fast; " : "")
            + "breaks 3x3 soft soil and planks.";
        if (level == 2) return "Breaks 3x3 soil, wood, clay, and cobblestone.";
        return "Breaks 3x3 soil, wood, clay, cobble, and stone.";
    }
}
