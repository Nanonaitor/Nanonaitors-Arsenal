package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.WeaponTier;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;

public final class ScimitarCombat {
    public static final int WEAKNESS_DURATION_TICKS = 10 * 20;

    private ScimitarCombat() {}

    /** Applies only from ItemScimitar.hitEntity, after the melee hit succeeds. */
    public static void applyWeakness(EntityLivingBase target, ItemScimitar item) {
        int amplifier = 0; // Weakness I
        if (item.getTier() == WeaponTier.LIVING) {
            amplifier = 1; // Weakness II
        } else if (item.getTier() == WeaponTier.SENTIENT) {
            amplifier = 2; // Weakness III
        }
        target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS,
            WEAKNESS_DURATION_TICKS, amplifier, false, true));
    }
}
