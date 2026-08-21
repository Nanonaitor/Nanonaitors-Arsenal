package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemClaws;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ClawCombat {
    private static final Map<EntityPlayer, PendingAttack> PENDING_ATTACKS = new WeakHashMap<>();

    private ClawCombat() {}

    public static void prepareMainHandAttack(EntityPlayer player, EntityLivingBase target,
                                             ItemClaws item, ItemStack stack,
                                             boolean fullyCharged) {
        boolean paired = ClawPairHandler.hasMatchingLinkedClaw(player, item);
        boolean canPierce = paired && fullyCharged;
        boolean guaranteedCritical = paired && item.willGuaranteeCritical(stack,
            fullyCharged);
        PENDING_ATTACKS.put(player, new PendingAttack(target.getEntityId(),
            player.world.getTotalWorldTime(), item, stack, paired,
            fullyCharged, canPierce, guaranteedCritical));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        PendingAttack pending = matching((EntityPlayer) event.getSource().getTrueSource(),
            event.getEntityLiving());
        if (pending != null && pending.guaranteedCritical) {
            event.setAmount(event.getAmount() * 1.5F);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        PendingAttack pending = matching(player, event.getEntityLiving());
        if (pending != null && pending.canPierce) {
            event.getEntityLiving().hurtResistantTime = 0;
            pending.pierced = true;
        }
    }

    public static void confirmHit(EntityPlayer player, EntityLivingBase target,
                                  ItemClaws item, ItemStack stack) {
        PendingAttack pending = matching(player, target);
        PENDING_ATTACKS.remove(player);
        if (pending == null || pending.item != item || pending.stack != stack) {
            return;
        }
        if (!pending.paired) {
            item.resetPair(stack);
            return;
        }
        boolean critical = item.confirmChargedPairedHit(stack, pending.fullyCharged);
        if (pending.pierced || critical) {
            target.world.playSound(null, target.posX, target.posY, target.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, target.getSoundCategory(), 0.45F, 1.45F);
        }
    }

    /** Paired claws use reduced knockback so consecutive auto-attacks can combo. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (!(event.getAttacker() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getAttacker();
        ItemStack main = player.getHeldItemMainhand();
        if (!(main.getItem() instanceof ItemClaws)) return;
        ItemClaws claws = (ItemClaws) main.getItem();
        if (ClawPairHandler.hasMatchingLinkedClaw(player, claws)) {
            event.setStrength(event.getStrength() * 0.5F);
        }
    }

    private static PendingAttack matching(EntityPlayer player, EntityLivingBase target) {
        PendingAttack pending = PENDING_ATTACKS.get(player);
        if (pending == null || pending.targetId != target.getEntityId()
            || player.world.getTotalWorldTime() - pending.worldTime > 1L
            || player.getHeldItemMainhand() != pending.stack
            || player.getHeldItemMainhand().getItem() != pending.item) {
            return null;
        }
        return pending;
    }

    private static final class PendingAttack {
        private final int targetId;
        private final long worldTime;
        private final ItemClaws item;
        private final ItemStack stack;
        private final boolean paired;
        private final boolean fullyCharged;
        private final boolean canPierce;
        private final boolean guaranteedCritical;
        private boolean pierced;

        private PendingAttack(int targetId, long worldTime, ItemClaws item,
                              ItemStack stack, boolean paired,
                              boolean fullyCharged, boolean canPierce,
                              boolean guaranteedCritical) {
            this.targetId = targetId;
            this.worldTime = worldTime;
            this.item = item;
            this.stack = stack;
            this.paired = paired;
            this.fullyCharged = fullyCharged;
            this.canPierce = canPierce;
            this.guaranteedCritical = guaranteedCritical;
        }
    }
}
