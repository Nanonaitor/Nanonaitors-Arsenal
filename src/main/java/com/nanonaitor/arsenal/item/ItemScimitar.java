package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.combat.ScimitarCombat;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ItemScimitar extends ItemArsenalWeapon {
    public ItemScimitar(WeaponTier tier) {
        super(tier, "scimitar", roundedAttackDamage(tier) - 1.0D, -2.2D);
    }

    /** Ten percent below the original final damage, rounded to the nearest half point. */
    private static double roundedAttackDamage(WeaponTier tier) {
        double original = 3.5D + tier.getMaterial().getAttackDamage();
        return Math.round(original * 0.90D * 2.0D) / 2.0D;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (!attacker.world.isRemote) {
            ScimitarCombat.applyWeakness(target, this);
        }
        return super.hitEntity(stack, target, attacker);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        if (getTier() == WeaponTier.LIVING) {
            tooltip.add(TextFormatting.DARK_PURPLE
                + "Hits inflict Weakness II for 10 secs.");
        } else if (getTier() == WeaponTier.SENTIENT) {
            tooltip.add(TextFormatting.DARK_PURPLE
                + "Hits inflict Weakness III for 10 secs.");
        } else {
            tooltip.add(TextFormatting.DARK_PURPLE
                + "Hits inflict Weakness I for 10 secs.");
        }
    }
}
