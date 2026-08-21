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
    private static final String CRIT_CHAIN_TAG = "ClawCriticalChain";

    public ItemClaws(WeaponTier tier) {
        super(tier, "claws", tier.getClawAttackDamage() - 1.0D, -1.6D);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!player.world.isRemote && entity instanceof EntityLivingBase) {
            boolean fullyCharged = player.getCooledAttackStrength(0.5F) >= 1.0F;
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

    public boolean willGuaranteeCritical(ItemStack stack, boolean fullyCharged) {
        if (!fullyCharged) return false;
        int chain = stack.hasTagCompound()
            ? stack.getTagCompound().getInteger(CRIT_CHAIN_TAG) : 0;
        return chain + 1 >= 4;
    }

    public boolean confirmChargedPairedHit(ItemStack stack, boolean fullyCharged) {
        NBTTagCompound tag = tag(stack);
        int chain = fullyCharged ? tag.getInteger(CRIT_CHAIN_TAG) + 1 : 0;
        boolean critical = chain >= 4;
        if (critical) chain = 0;
        tag.setInteger(CRIT_CHAIN_TAG, chain);
        return critical;
    }

    public void resetPair(ItemStack stack) {
        if (stack.hasTagCompound()) {
            // Remove the old alternation data as well so existing claws cleanly
            // migrate to the paired auto-attack combo system.
            stack.getTagCompound().removeTag("LastConfirmedClaw");
            stack.getTagCompound().removeTag("LastConfirmedClawTarget");
            stack.getTagCompound().removeTag(CRIT_CHAIN_TAG);
        }
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GOLD + "Automatically equips its paired offhand claw.");
        tooltip.add(TextFormatting.GRAY + "Hold left/right click to auto-attack with each claw.");
        tooltip.add(TextFormatting.GRAY + "Fully charged paired hits pierce i-frames.");
        tooltip.add(TextFormatting.YELLOW + "Every 4th fully charged paired hit is a critical.");
        tooltip.add(TextFormatting.DARK_GRAY + "A different offhand item disables all paired abilities.");
    }

    private static NBTTagCompound tag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
