package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.compat.ReskillableCompat;
import com.nanonaitor.arsenal.network.FlailAnimationMessage;
import com.nanonaitor.arsenal.network.ModNetwork;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class FlailCombat {
    private static final Map<EntityPlayer, Long> LAST_SWING_TICK = new WeakHashMap<>();

    private FlailCombat() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelVanillaAttack(AttackEntityEvent event) {
        if (com.nanonaitor.arsenal.compat.ScimitarAttackBridge.isControlledOffhandAttack()) return;
        if (event.getEntityPlayer().getHeldItemMainhand().getItem() instanceof ItemFlail) {
            event.setCanceled(true);
        }
    }

    public static void tryServerSwing(EntityPlayerMP player) {
        ItemStack weapon = player.getHeldItemMainhand();
        if (!(weapon.getItem() instanceof ItemFlail) || !player.isEntityAlive()
            || player.isSpectator() || !ReskillableCompat.canUse(player, weapon)
            || isBlockingConventionalShield(player)) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        int swingInterval = ChainWeaponStats.swingIntervalTicks(player, weapon);
        Long last = LAST_SWING_TICK.get(player);
        if (last != null && now - last < swingInterval) {
            return;
        }
        LAST_SWING_TICK.put(player, now);

        double radius = ChainWeaponStats.flailReach(player, weapon);
        double verticalRadius = ChainWeaponStats.flailVerticalReach(player, weapon);

        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(
            EntityLivingBase.class, player.getEntityBoundingBox().grow(radius),
            target -> CombatTargetRules.canHit(player, target)
                && isHitboxWithinRange(player, target, radius, verticalRadius)
                && canSeeHitbox(player, target));

        float baseDamage = (float) player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        boolean critical = WeaponHits.isVanillaCritical(player);
        if (critical) {
            baseDamage *= 1.5F;
        }

        boolean hitAnything = false;
        for (EntityLivingBase target : targets) {
            float enchantmentDamage = EnchantmentHelper.getModifierForCreature(
                weapon, target.getCreatureAttribute());
            if (!target.attackEntityFrom(DamageSource.causePlayerDamage(player),
                baseDamage + enchantmentDamage)) {
                continue;
            }
            hitAnything = true;
            if (critical) {
                player.onCriticalHit(target);
            }
            if (enchantmentDamage > 0.0F) {
                player.onEnchantmentCritical(target);
            }
            WeaponHits.applyPostHitEffects(player, target, 0.5F);
        }

        player.resetCooldown();
        SoundEvent sound = hitAnything ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG
            : SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE;
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            sound, player.getSoundCategory(), hitAnything ? 1.0F : 0.65F, 0.82F);
        spawnHitboxCircle(player, radius);
        ModNetwork.CHANNEL.sendToAllAround(
            new FlailAnimationMessage(player.getEntityId()),
            new NetworkRegistry.TargetPoint(player.dimension, player.posX,
                player.posY, player.posZ, 64.0D));
        if (hitAnything) {
            weapon.damageItem(1, player);
            player.addExhaustion(0.1F);
        }
    }

    /**
     * Flails cannot orbit while the player is actively guarding with a shield.
     * Defenders deliberately remain exempt because attacking while defending is
     * one of that mod's core mechanics.
     */
    public static boolean isBlockingConventionalShield(EntityPlayer player) {
        if (player == null || !player.isHandActive()) {
            return false;
        }
        ItemStack active = player.getActiveItemStack();
        if (active.isEmpty() || active.getItemUseAction() != EnumAction.BLOCK) {
            return false;
        }
        return !isDefender(active);
    }

    public static boolean isDefender(ItemStack stack) {
        return AbilityUseRules.defender(stack);
    }

    /** Mirrors the modern build's visible four-block flail hitbox ring. */
    private static void spawnHitboxCircle(EntityPlayerMP player, double radius) {
        double base = Math.toRadians(player.rotationYaw);
        for (int index = 0; index < 32; index++) {
            double angle = base + Math.PI * 2.0D * index / 32.0D;
            player.getServerWorld().spawnParticle(EnumParticleTypes.SWEEP_ATTACK,
                player.posX + Math.cos(angle) * radius,
                player.posY + 1.0D,
                player.posZ + Math.sin(angle) * radius,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static boolean isHitboxWithinRange(EntityPlayer player,
                                                EntityLivingBase target,
                                                double radius,
                                                double verticalRadius) {
        AxisAlignedBB playerBox = player.getEntityBoundingBox();
        AxisAlignedBB targetBox = target.getEntityBoundingBox();
        double dx = Math.max(0.0D, Math.max(playerBox.minX - targetBox.maxX,
            targetBox.minX - playerBox.maxX));
        double dy = Math.max(0.0D, Math.max(playerBox.minY - targetBox.maxY,
            targetBox.minY - playerBox.maxY));
        double dz = Math.max(0.0D, Math.max(playerBox.minZ - targetBox.maxZ,
            targetBox.minZ - playerBox.maxZ));
        return dx * dx + dz * dz <= radius * radius && dy <= verticalRadius;
    }

    private static boolean canSeeHitbox(EntityPlayer player, EntityLivingBase target) {
        if (player.canEntityBeSeen(target)) {
            return true;
        }
        Vec3d eyes = player.getPositionEyes(1.0F);
        AxisAlignedBB box = target.getEntityBoundingBox();
        Vec3d nearestPoint = new Vec3d(
            MathHelper.clamp(eyes.x, box.minX, box.maxX),
            MathHelper.clamp(eyes.y, box.minY, box.maxY),
            MathHelper.clamp(eyes.z, box.minZ, box.maxZ));
        return player.world.rayTraceBlocks(eyes, nearestPoint, false, true, false) == null;
    }
}
