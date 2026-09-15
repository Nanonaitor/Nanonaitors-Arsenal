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
}
