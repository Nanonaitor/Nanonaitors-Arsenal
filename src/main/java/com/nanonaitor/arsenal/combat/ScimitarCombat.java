package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.item.ItemScimitar;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

public final class ScimitarCombat {
    public static final int PROC_CHANCE_PERCENT = 10;
    public static final int WEAKNESS_DURATION_TICKS = 2 * 20;
    private static final Map<EntityPlayer, PendingAttack> PENDING_ATTACKS = new WeakHashMap<>();

    private ScimitarCombat() {}

    public static void prepareAttack(EntityPlayer player, EntityLivingBase target,
                                     ItemScimitar item, boolean fullyCharged) {
        PENDING_ATTACKS.put(player, new PendingAttack(target.getEntityId(),
            player.world.getTotalWorldTime(), item, fullyCharged));
    }

    public static void confirmHit(EntityPlayer player, EntityLivingBase target,
                                  ItemScimitar item) {
        PendingAttack pending = PENDING_ATTACKS.remove(player);
        if (pending == null || !pending.fullyCharged || pending.item != item
            || pending.targetId != target.getEntityId()
            || player.world.getTotalWorldTime() - pending.worldTime > 1L) {
            return;
        }
        if (player.getRNG().nextInt(100) < PROC_CHANCE_PERCENT) {
            target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS,
                WEAKNESS_DURATION_TICKS, 1, false, true));
        }
    }

    private static final class PendingAttack {
        private final int targetId;
        private final long worldTime;
        private final ItemScimitar item;
        private final boolean fullyCharged;

        private PendingAttack(int targetId, long worldTime, ItemScimitar item,
                              boolean fullyCharged) {
            this.targetId = targetId;
            this.worldTime = worldTime;
            this.item = item;
            this.fullyCharged = fullyCharged;
        }
    }
}
