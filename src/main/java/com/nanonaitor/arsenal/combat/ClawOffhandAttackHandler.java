package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.item.ItemClaws;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Loader;

public final class ClawOffhandAttackHandler {
    private static final Map<EntityPlayer, Long> LAST_ATTACK_TICK = new WeakHashMap<>();

    private ClawOffhandAttackHandler() {}

    public static void tryServerAttack(EntityPlayer player, EntityLivingBase target) {
        ItemStack main = player.getHeldItemMainhand();
        if (!(main.getItem() instanceof ItemClaws)) {
            return;
        }
        ItemClaws claws = (ItemClaws) main.getItem();
        if (!ClawPairHandler.hasMatchingLinkedClaw(player, claws)) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        long last = LAST_ATTACK_TICK.containsKey(player) ? LAST_ATTACK_TICK.get(player) : Long.MIN_VALUE;
        double cooldownTicks = 20.0D / claws.getDisplayedAttackSpeed();
        float strength = last == Long.MIN_VALUE ? 1.0F
            : MathHelper.clamp((float) ((now - last + 0.5D) / cooldownTicks), 0.0F, 1.0F);
        LAST_ATTACK_TICK.put(player, now);

        boolean fullyCharged = strength >= 0.95F;
        boolean canPierce = fullyCharged && claws.getLastConfirmedHand(main) == 0
            && claws.getLastConfirmedTarget(main) == target.getEntityId();
        boolean guaranteedCritical = claws.willGuaranteeCritical(main, 1,
            target.getEntityId(), fullyCharged);
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
        if (guaranteedCritical) damage *= 1.5F;

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
        player.swingArm(EnumHand.OFF_HAND);
        boolean critical = claws.confirmChargedAlternatingHit(main, 1,
            target.getEntityId(), fullyCharged);
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

        // Match the normal main-hand weapon feedback instead of using the
        // sweeping sound for every linked-claw hit. Playing from the server with
        // no excluded player makes the wielder and nearby players hear it once.
        SoundEvent sound = fullyCharged ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG
            : SoundEvents.ENTITY_PLAYER_ATTACK_WEAK;
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            sound, player.getSoundCategory(), 1.0F, fullyCharged ? 1.0F : 1.1F);
        if (canPierce || critical) {
            player.world.playSound(null, target.posX, target.posY, target.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, target.getSoundCategory(),
                0.45F, 1.45F);
        }
    }

}
