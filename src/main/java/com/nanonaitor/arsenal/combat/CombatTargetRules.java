package com.nanonaitor.arsenal.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/** Shared eligibility checks for custom area attacks, without imposing melee reach on throws. */
public final class CombatTargetRules {
    private CombatTargetRules() {}
    public static boolean canHit(EntityPlayer player, EntityLivingBase target) {
        return target != null && target != player && !target.isDead && !player.isSpectator()
            && !player.isOnSameTeam(target)
            && (!(target instanceof EntityPlayer) || (!((EntityPlayer)target).isSpectator()
                && player.canAttackPlayer((EntityPlayer)target)));
    }

    /**
     * Secondary splash damage around a hit target. Stricter than {@link #canHit}:
     * it also never reaches the attacker's own tamed animals.
     */
    public static boolean canSplash(EntityPlayer player, EntityLivingBase target) {
        return canHit(player, target) && !isOwnPet(player, target);
    }

    public static boolean isOwnPet(EntityPlayer player, EntityLivingBase target) {
        return target instanceof net.minecraft.entity.IEntityOwnable
            && player.getUniqueID().equals(
                ((net.minecraft.entity.IEntityOwnable) target).getOwnerId());
    }
}
