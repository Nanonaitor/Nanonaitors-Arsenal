package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.combat.MorningStarCombat;
import com.nanonaitor.arsenal.client.ArsenalTooltip;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.config.ConfiguredEffects;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.EnumAction;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public final class ItemMorningStar extends ItemArsenalWeapon {
    public ItemMorningStar(WeaponTier tier) {
        super(tier, "morning_star", 4.0D + tier.getMaterial().getAttackDamage(), -3.0D);
    }

    @Override public int getMaxItemUseDuration(ItemStack stack) { return 72000; }
    // Charging is driven by our input/pose handlers. Using BOW here makes
    // vanilla repeatedly select bow pull frames and produces visible flicker.
    @Override public EnumAction getItemUseAction(ItemStack stack) { return EnumAction.NONE; }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
                                                     EnumHand hand) {
        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!player.world.isRemote && entity instanceof EntityLivingBase) {
            boolean fullyCharged = player.getCooledAttackStrength(0.5F) >= 0.95F;
            MorningStarCombat.prepareAttack(player, (EntityLivingBase) entity, this, fullyCharged);
        }
        return false;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (!attacker.world.isRemote && attacker instanceof EntityPlayer) {
            MorningStarCombat.applyConfirmedHit((EntityPlayer) attacker, target, this);
        }
        return super.hitEntity(stack, target, attacker);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        if (!ArsenalTooltip.begin(tooltip, TextFormatting.GOLD,
                "Hold attack to charge for a stronger sweeping hit.")) return;
        tooltip.add(TextFormatting.GRAY + "Charge increases damage and sweep width.");
        String effects = ConfiguredEffects.displayNames(combine(
            ArsenalConfig.morningStar.fractureEffects, ArsenalConfig.morningStar.stunEffects));
        if (!effects.isEmpty()) tooltip.add(TextFormatting.GRAY + "Full charge applies: " + effects + ".");
    }

    private static String[] combine(String[] first, String[] second) {
        String[] combined = new String[first.length + second.length];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }
}
