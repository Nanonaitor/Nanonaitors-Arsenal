package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.combat.ScimitarCombat;
import com.nanonaitor.arsenal.client.ArsenalTooltip;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.item.EnumAction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

public final class ItemScimitar extends ItemArsenalWeapon {
    public ItemScimitar(WeaponTier tier) {
        super(tier, "scimitar", roundedAttackDamage(tier) - 1.0D, -2.2D);
    }

    @Override public int getMaxItemUseDuration(ItemStack stack) { return 72000; }
    // Changes only with the equipment pairing, never with each held-use tick.
    @Override public EnumAction getItemUseAction(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean("ArsenalScimitarPaired")
            ? EnumAction.BLOCK:EnumAction.NONE;
    }
    private static void setPaired(ItemStack stack,boolean paired) {
        if(!stack.hasTagCompound()) {
            if(!paired)return;
            stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }
        if(stack.getTagCompound().getBoolean("ArsenalScimitarPaired")!=paired)
            stack.getTagCompound().setBoolean("ArsenalScimitarPaired",paired);
    }
    @Override public void onUpdate(ItemStack stack,World world,net.minecraft.entity.Entity entity,int slot,boolean selected) {
        super.onUpdate(stack,world,entity,slot,selected);
        if(entity instanceof EntityPlayer) {
            EntityPlayer player=(EntityPlayer)entity;
            boolean paired=com.nanonaitor.arsenal.compat.ScimitarShieldCompat.isPair(player)
                && (stack==player.getHeldItemMainhand() || stack==player.getHeldItemOffhand());
            setPaired(stack,paired);
            if(!paired && player.isHandActive() && player.getActiveItemStack()==stack)player.resetActiveHand();
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        boolean dual = player.getHeldItemMainhand().getItem() instanceof ItemScimitar
            && player.getHeldItemOffhand().getItem() instanceof ItemScimitar;
        if (!dual) {
            EnumHand other=hand==EnumHand.MAIN_HAND?EnumHand.OFF_HAND:EnumHand.MAIN_HAND;
            if (player.getHeldItem(other).getItem() instanceof net.minecraft.item.ItemShield)
                return new ActionResult<>(EnumActionResult.PASS,held);
            return new ActionResult<>(EnumActionResult.FAIL,held);
        }
        if (player.getCooldownTracker().hasCooldown(player.getHeldItemMainhand().getItem())
            || player.getCooldownTracker().hasCooldown(player.getHeldItemOffhand().getItem()))
            return new ActionResult<>(EnumActionResult.FAIL,held);
        setPaired(held,true);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, held);
    }

    /** Ten percent below the original final damage, rounded to the nearest half point. */
    private static double roundedAttackDamage(WeaponTier tier) {
        double original = 3.5D + tier.getMaterial().getAttackDamage();
        return Math.round(original * 0.90D * 2.0D) / 2.0D;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (!attacker.world.isRemote) {
            ScimitarCombat.applyWeakness(target, attacker, this);
        }
        return super.hitEntity(stack, target, attacker);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        if (!ArsenalTooltip.begin(tooltip, TextFormatting.GOLD,
                "Fast blade that weakens enemies on hit.")) return;
        tooltip.add(TextFormatting.GOLD + "Dual wield to alternate attacks and cross-guard.");
        if ((getTier() == WeaponTier.GOLD || getTier() == WeaponTier.SILVER) && Loader.isModLoaded("setbonus")) {
            tooltip.add(TextFormatting.DARK_PURPLE
                + "Default: Weakness I; II with the matching RLCraft armor-set bonus.");
        } else if (getTier() == WeaponTier.SENTIENT) {
            tooltip.add(TextFormatting.DARK_PURPLE
                + "Hits inflict Weakness III for 10 secs.");
        } else if (ScimitarCombat.getBaseWeaknessAmplifier(getTier()) == 1) {
            tooltip.add(TextFormatting.DARK_PURPLE
                + "Hits inflict Weakness II for 10 secs.");
        } else {
            tooltip.add(TextFormatting.DARK_PURPLE
                + "Hits inflict Weakness I for 10 secs.");
        }
    }
}
