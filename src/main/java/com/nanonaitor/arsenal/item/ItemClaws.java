package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.combat.ClawCombat;
import com.nanonaitor.arsenal.client.ArsenalTooltip;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
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
            return !ClawCombat.prepareMainHandAttack(player, (EntityLivingBase) entity,
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

    @Override
    public void onUpdate(ItemStack stack,World world,Entity entity,int slot,boolean selected) {
        super.onUpdate(stack,world,entity,slot,selected);
        if(!world.isRemote)resetPair(stack); // Remove obsolete banked-crit data from existing claws.
    }

    public void resetPair(ItemStack stack) {
        if (stack.hasTagCompound()) {
            // Remove obsolete critical/alternation state without touching item data.
            stack.getTagCompound().removeTag("LastConfirmedClaw");
            stack.getTagCompound().removeTag("LastConfirmedClawTarget");
            stack.getTagCompound().removeTag(CRIT_CHAIN_TAG);
        }
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        if (!ArsenalTooltip.begin(tooltip, TextFormatting.GOLD,
                "Automatically equips its paired offhand claw.")) return;
        tooltip.add(TextFormatting.GRAY + "Hold left/right click to auto-attack with each claw.");
        tooltip.add(TextFormatting.GRAY + "Fully charged paired hits pierce i-frames, at most once per 4 ticks per target.");
        tooltip.add(TextFormatting.DARK_GRAY + "A different offhand item disables all paired abilities.");
    }

}
