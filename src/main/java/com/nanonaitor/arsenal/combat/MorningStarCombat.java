package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.registry.ModContent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;

public final class MorningStarCombat {
    private static final int MOB_DURATION_TICKS = 30 * 20;
    private static final int PLAYER_DURATION_TICKS = 10 * 20;
    // Server-side only: set from onLeftClickEntity and consumed by hitEntity.
    private static final SidedPlayerMap<PendingAttack> PENDING_ATTACKS = new SidedPlayerMap<>();

    private MorningStarCombat() {}

    public static void prepareAttack(EntityPlayer player, EntityLivingBase target,
                                     ItemMorningStar item, boolean fullyCharged) {
        PENDING_ATTACKS.put(player,
            new PendingAttack(target.getEntityId(), player.world.getTotalWorldTime(), item, fullyCharged));
    }

    /**
     * Fracture from a fully charged hit that went through the vanilla attack
     * path. The charged sweep applies fracture itself via {@link #applyFracture}.
     */
    public static void applyConfirmedHit(EntityPlayer player, EntityLivingBase target,
                                         ItemMorningStar item) {
        PendingAttack pending = PENDING_ATTACKS.remove(player);
        if (pending == null || !pending.fullyCharged || pending.item != item
            || pending.targetId != target.getEntityId()
            || player.world.getTotalWorldTime() - pending.worldTime > 1L) {
            return;
        }
        applyFracture(target, item);
    }

    /** Adds one Armor Fracture level, up to the tier's cap: 10 s on players, 30 s on mobs. */
    public static void applyFracture(EntityLivingBase target, ItemMorningStar item) {
        if (ModContent.ARMOR_FRACTURE == null) return;
        PotionEffect current = target.getActivePotionEffect(ModContent.ARMOR_FRACTURE);
        int currentLevel = current == null ? 0 : current.getAmplifier() + 1;
        int newLevel = Math.min(currentLevel + 1, item.getTier().getMorningStarFractureCap());
        int duration = target instanceof EntityPlayer ? PLAYER_DURATION_TICKS : MOB_DURATION_TICKS;
        com.nanonaitor.arsenal.config.ConfiguredEffects.apply(target,
            com.nanonaitor.arsenal.config.ArsenalConfig.morningStar.fractureEffects, duration, newLevel - 1);
    }

    private static final class PendingAttack {
        private final int targetId;
        private final long worldTime;
        private final ItemMorningStar item;
        private final boolean fullyCharged;

        private PendingAttack(int targetId, long worldTime, ItemMorningStar item,
                              boolean fullyCharged) {
            this.targetId = targetId;
            this.worldTime = worldTime;
            this.item = item;
            this.fullyCharged = fullyCharged;
        }
    }
}
