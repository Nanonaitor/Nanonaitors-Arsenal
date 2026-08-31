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
    // Keep the use action immutable. Toggling it through synchronized stack
    // NBT made the client briefly leave BLOCK between updates, causing the
    // crossed guard to lower and raise repeatedly.
    @Override public EnumAction getItemUseAction(ItemStack stack) { return EnumAction.BLOCK; }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        boolean dual = player.getHeldItemMainhand().getItem() instanceof ItemScimitar
            && player.getHeldItemOffhand().getItem() instanceof ItemScimitar;
        if (!dual) return new ActionResult<>(EnumActionResult.PASS, held);
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
        if (getTier() == WeaponTier.GOLD && Loader.isModLoaded("setbonus")) {
            tooltip.add(TextFormatting.DARK_PURPLE
                + "Hits inflict Weakness I, or Weakness II with a full Gold armor set.");
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
