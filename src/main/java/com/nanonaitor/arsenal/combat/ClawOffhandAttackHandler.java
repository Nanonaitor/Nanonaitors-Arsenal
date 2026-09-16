package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.compat.ReskillableCompat;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ClawOffhandAttackHandler {
    private static final Map<EntityPlayer, Long> LAST_ATTACK_TICK = new WeakHashMap<>();
    private static final Map<EntityPlayer, Long> PENDING_SWING_TICK = new WeakHashMap<>();
    private static final Map<EntityPlayer, DeferredAttack> DEFERRED = new WeakHashMap<>();

    private ClawOffhandAttackHandler() {}

    public static void tryServerAttack(EntityPlayer player, EntityLivingBase target) {
        tryServerAttack(player,target,true);
    }

    private static void tryServerAttack(EntityPlayer player, EntityLivingBase target,boolean mayDefer) {
        if (!CombatTargetRules.canHit(player, target)) return;
        ItemStack main = player.getHeldItemMainhand();
        if (!(main.getItem() instanceof ItemClaws)) {
            return;
        }
        if (!ReskillableCompat.canUse(player, main)) return;
        ItemClaws claws = (ItemClaws) main.getItem();
        if (!ClawPairHandler.hasMatchingLinkedClaw(player, claws)) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        long last = LAST_ATTACK_TICK.containsKey(player) ? LAST_ATTACK_TICK.get(player) : Long.MIN_VALUE;
        double cooldownTicks = 20.0D / claws.getDisplayedAttackSpeed();
        float strength = last == Long.MIN_VALUE ? 1.0F
            : MathHelper.clamp((float) ((now - last + 0.5D) / cooldownTicks), 0.0F, 1.0F);
        boolean fullyCharged = strength >= 1.0F;
        boolean canPierce = fullyCharged;
        if(canPierce && !ClawCombat.canPierceNow(player,target)) {
            if(mayDefer && !DEFERRED.containsKey(player))DEFERRED.put(player,new DeferredAttack(target,main,now+4));
            return;
        }
        DEFERRED.remove(player);
        LAST_ATTACK_TICK.put(player, now);
        ClawCombat.clearPending(player);
        int previousResistance = target.hurtResistantTime;
        if (canPierce) {
            target.hurtResistantTime = 0;
        }

        float baseDamage = (float) player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        float enchantmentDamage = EnchantmentHelper.getModifierForCreature(
            main, target.getCreatureAttribute());
        float damage = baseDamage * (0.2F + strength * strength * 0.8F)
            + enchantmentDamage * strength;

        // RLCombat inspects the equipped offhand while resolving player damage
        // and can apply its generic weaker-offhand rule to this custom paired
        // attack. The linked claw already derives its full damage, enchants,
        // quality and cooldown from the main claw, so hide only the generated
        // visual partner for the synchronous damage call when RLCombat exists.
        ItemStack linked = player.getHeldItemOffhand();
        boolean hideLinkedForRlCombat = Loader.isModLoaded("bettercombatmod");
        if (hideLinkedForRlCombat) {
            player.inventory.offHandInventory.set(0, ItemStack.EMPTY);
        }
        boolean hit;
        try {
            hit = target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
        } finally {
            if (hideLinkedForRlCombat) {
                player.inventory.offHandInventory.set(0, linked);
            }
        }
        if (!hit) {
            target.hurtResistantTime = previousResistance;
            return;
        }

        // Broadcast the offhand animation only after damage is resolved. RLCombat's
        // default weakerOffhand rule keys off the active swing hand and would
        // otherwise halve this paired-weapon attack.
        PENDING_SWING_TICK.put(player, now + 2L);
        if(canPierce)ClawCombat.recordPiercingHit(player,target);
        main.damageItem(1, player);
        player.addExhaustion(0.1F);

        int fireAspect = EnchantmentHelper.getFireAspectModifier(player);
        if (fireAspect > 0) {
            target.setFire(fireAspect * 4);
        }
        int knockback = EnchantmentHelper.getKnockbackModifier(player);
        if (knockback > 0) {
            target.knockBack(player, knockback * 0.5F,
                MathHelper.sin(player.rotationYaw * 0.017453292F),
                -MathHelper.cos(player.rotationYaw * 0.017453292F));
        }
        EnchantmentHelper.applyThornEnchantments(target, player);
        EnchantmentHelper.applyArthropodEnchantments(player, target);

        // Play impact feedback only after the server confirms damage. Using no
        // excluded player lets the wielder and nearby players hear it exactly
        // once; whiffs remain silent while still animating on the client.
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, player.getSoundCategory(),
            0.8F, 1.15F);
    }

    @SubscribeEvent
    public static void animateDelayedOffhand(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        DeferredAttack attack=DEFERRED.get(event.player);
        if(attack!=null) {
            EntityLivingBase target=attack.target.get();
            long now=event.player.world.getTotalWorldTime();
            if(target==null || now>attack.expires || event.player.getHeldItemMainhand()!=attack.stack
                || !CombatTargetRules.canHit(event.player,target) || event.player.getDistanceSq(target)>36
                || !event.player.canEntityBeSeen(target))DEFERRED.remove(event.player);
            else if(ClawCombat.canPierceNow(event.player,target)) {
                DEFERRED.remove(event.player);
                tryServerAttack(event.player,target,false);
            }
        }
        Long due = PENDING_SWING_TICK.get(event.player);
        if (due == null || event.player.world.getTotalWorldTime() < due) return;
        PENDING_SWING_TICK.remove(event.player);
        event.player.swingArm(EnumHand.OFF_HAND);
    }

    private static final class DeferredAttack {
        final java.lang.ref.WeakReference<EntityLivingBase> target;
        final ItemStack stack;
        final long expires;
        DeferredAttack(EntityLivingBase target,ItemStack stack,long expires) {
            this.target=new java.lang.ref.WeakReference<>(target);this.stack=stack;this.expires=expires;
        }
    }

}
