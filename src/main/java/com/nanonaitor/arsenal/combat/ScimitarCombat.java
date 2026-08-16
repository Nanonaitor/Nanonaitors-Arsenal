package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.WeaponTier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

public final class ScimitarCombat {
    public static final int WEAKNESS_DURATION_TICKS = 10 * 20;

    private ScimitarCombat() {}

    /** Applies only from ItemScimitar.hitEntity, after the melee hit succeeds. */
    public static void applyWeakness(EntityLivingBase target, EntityLivingBase attacker,
                                     ItemScimitar item) {
        int amplifier = getBaseWeaknessAmplifier(item.getTier());
        if (item.getTier() == WeaponTier.GOLD && hasFullGoldArmor(attacker)) {
            amplifier = 1; // Weakness II while Gold's full-set infusion is active.
        }
        target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS,
            WEAKNESS_DURATION_TICKS, amplifier, false, true));
    }

    public static int getBaseWeaknessAmplifier(WeaponTier tier) {
        if (tier == WeaponTier.SENTIENT) return 2; // Weakness III remains its upgrade.
        if (tier.getRamBreakLevel() >= 3 || tier.isOrganic()) return 1;
        return 0;
    }

    public static boolean hasFullGoldArmor(EntityLivingBase wearer) {
        return isGoldArmor(wearer.getItemStackFromSlot(EntityEquipmentSlot.HEAD))
            && isGoldArmor(wearer.getItemStackFromSlot(EntityEquipmentSlot.CHEST))
            && isGoldArmor(wearer.getItemStackFromSlot(EntityEquipmentSlot.LEGS))
            && isGoldArmor(wearer.getItemStackFromSlot(EntityEquipmentSlot.FEET));
    }

    private static boolean isGoldArmor(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemArmor
            && ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.GOLD;
    }
}
