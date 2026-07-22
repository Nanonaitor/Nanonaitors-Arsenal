package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.combat.ClawCombat;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ItemClaws extends ItemArsenalWeapon {
    private static final String LAST_HAND_TAG = "LastConfirmedClaw";

    public ItemClaws(WeaponTier tier) {
        super(tier, "claws", tier.getClawAttackDamage() - 1.0D, -1.6D);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!player.world.isRemote && entity instanceof EntityLivingBase) {
            boolean fullyCharged = player.getCooledAttackStrength(0.5F) >= 0.95F;
            ClawCombat.prepareMainHandAttack(player, (EntityLivingBase) entity,
                this, stack, fullyCharged);
        }
        return false;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (!attacker.world.isRemote && attacker instanceof EntityPlayer) {
            ClawCombat.confirmHit((EntityPlayer) attacker, target, this, stack);
        }
        return super.hitEntity(stack, target, attacker);
    }

    public int getLastConfirmedHand(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey(LAST_HAND_TAG)
            ? stack.getTagCompound().getInteger(LAST_HAND_TAG) : -1;
    }

    public void confirmHand(ItemStack stack, int hand) {
        NBTTagCompound tag = tag(stack);
        tag.setInteger(LAST_HAND_TAG, hand);
    }

    public void resetPair(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(LAST_HAND_TAG);
        }
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GOLD + "Automatically equips its paired offhand claw.");
        tooltip.add(TextFormatting.GRAY + "Left-click: main claw. Right-click targets: offhand claw.");
        tooltip.add(TextFormatting.GRAY + "Fully charged alternating hits pierce i-frames.");
        tooltip.add(TextFormatting.DARK_GRAY + "A different offhand item disables all paired abilities.");
    }

    private static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
