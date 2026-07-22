package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemFlail;
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
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class FlailCombat {
    public static final double RADIUS = 4.0D;
    public static final int SWING_INTERVAL_TICKS = 25;
    private static final Map<EntityPlayer, Long> LAST_SWING_TICK = new WeakHashMap<>();

    private FlailCombat() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelVanillaAttack(AttackEntityEvent event) {
        if (event.getEntityPlayer().getHeldItemMainhand().getItem() instanceof ItemFlail) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventBlockBreaking(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntityPlayer().getHeldItemMainhand().getItem() instanceof ItemFlail) {
            event.setCanceled(true);
            event.setUseBlock(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        }
    }

    public static void tryServerSwing(EntityPlayerMP player) {
        ItemStack weapon = player.getHeldItemMainhand();
        if (!(weapon.getItem() instanceof ItemFlail) || !player.isEntityAlive()
            || player.isSpectator()) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        Long last = LAST_SWING_TICK.get(player);
        if (last != null && now - last < SWING_INTERVAL_TICKS) {
            return;
        }
        LAST_SWING_TICK.put(player, now);

        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(
            EntityLivingBase.class, player.getEntityBoundingBox().grow(RADIUS),
            target -> target != player && !target.isDead
                && isHitboxWithinRange(player, target)
                && !player.isOnSameTeam(target) && canSeeHitbox(player, target));

        float baseDamage = (float) player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        boolean critical = player.fallDistance > 0.0F && !player.onGround
            && !player.isOnLadder() && !player.isInWater()
            && !player.isPotionActive(MobEffects.BLINDNESS) && !player.isRiding();
        if (critical) {
            baseDamage *= 1.5F;
        }

        boolean hitAnything = false;
        int fireAspect = EnchantmentHelper.getFireAspectModifier(player);
        int knockback = EnchantmentHelper.getKnockbackModifier(player);
        for (EntityLivingBase target : targets) {
            float enchantmentDamage = EnchantmentHelper.getModifierForCreature(
                weapon, target.getCreatureAttribute());
            if (!target.attackEntityFrom(DamageSource.causePlayerDamage(player),
                baseDamage + enchantmentDamage)) {
                continue;
            }
            hitAnything = true;
            if (fireAspect > 0) {
                target.setFire(fireAspect * 4);
            }
            if (knockback > 0) {
                target.knockBack(player, knockback * 0.5F,
                    MathHelper.sin(player.rotationYaw * 0.017453292F),
                    -MathHelper.cos(player.rotationYaw * 0.017453292F));
            }
            if (critical) {
                player.onCriticalHit(target);
            }
            if (enchantmentDamage > 0.0F) {
                player.onEnchantmentCritical(target);
            }
            EnchantmentHelper.applyThornEnchantments(target, player);
            EnchantmentHelper.applyArthropodEnchantments(player, target);
        }

        player.resetCooldown();
        SoundEvent sound = hitAnything ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG
            : SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE;
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            sound, player.getSoundCategory(), hitAnything ? 1.0F : 0.65F, 0.82F);
        ModNetwork.CHANNEL.sendToAllAround(
            new FlailAnimationMessage(player.getEntityId()),
            new NetworkRegistry.TargetPoint(player.dimension, player.posX,
                player.posY, player.posZ, 64.0D));
        if (hitAnything) {
            weapon.damageItem(1, player);
            player.addExhaustion(0.1F);
        }
    }

    private static boolean isHitboxWithinRange(EntityPlayer player,
                                                EntityLivingBase target) {
        AxisAlignedBB playerBox = player.getEntityBoundingBox();
        AxisAlignedBB targetBox = target.getEntityBoundingBox();
        double dx = Math.max(0.0D, Math.max(playerBox.minX - targetBox.maxX,
            targetBox.minX - playerBox.maxX));
        double dy = Math.max(0.0D, Math.max(playerBox.minY - targetBox.maxY,
            targetBox.minY - playerBox.maxY));
        double dz = Math.max(0.0D, Math.max(playerBox.minZ - targetBox.maxZ,
            targetBox.minZ - playerBox.maxZ));
        return dx * dx + dy * dy + dz * dz <= RADIUS * RADIUS;
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
