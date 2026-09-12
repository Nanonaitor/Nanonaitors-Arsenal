package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.registry.ModContent;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

/** Resolves live reach and attack-speed modifiers from equipment, potions and enchants. */
public final class ChainWeaponStats {
    private static final double MIN_REACH = 1.0D;
    private static final double MIN_ATTACK_SPEED = 0.05D;

    private ChainWeaponStats() {}

    public static double flailReach(EntityPlayer player, ItemStack stack) {
        return Math.max(MIN_REACH, com.nanonaitor.arsenal.config.ArsenalConfig.reach.flailRadius + reachBonus(player, stack));
    }

    public static double flailVerticalReach(EntityPlayer player, ItemStack stack) {
        return Math.max(MIN_REACH, com.nanonaitor.arsenal.config.ArsenalConfig.reach.flailRadius + attributeReachBonus(player));
    }

    public static double longChainBonus(EntityPlayer player, ItemStack stack) {
        return ModContent.LONG_CHAIN == null ? 0.0D
            : EnchantmentHelper.getEnchantmentLevel(ModContent.LONG_CHAIN, stack);
    }

    public static double ballWindupReach(EntityPlayer player, ItemStack stack) {
        return Math.max(MIN_REACH, com.nanonaitor.arsenal.config.ArsenalConfig.reach.ballWindupReach
            + reachBonus(player, stack));
    }

    public static double ballThrowReach(EntityPlayer player, ItemStack stack,
                                        int effectiveCharge) {
        return Math.max(MIN_REACH, effectiveCharge
            * com.nanonaitor.arsenal.config.ArsenalConfig.reach.ballThrowReachPerCharge + reachBonus(player, stack));
    }

    public static int swingIntervalTicks(EntityPlayer player, ItemStack stack) {
        return Math.max(1, (int) Math.round(20.0D / attackSpeed(player, stack)));
    }

    public static int ballReleaseAnimationTicks(EntityPlayer player, ItemStack stack) {
        double baseSpeed = stack.getItem() instanceof ItemArsenalWeapon
            ? ((ItemArsenalWeapon) stack.getItem()).getDisplayedAttackSpeed()
            : attackSpeed(player, stack);
        double speedRatio = baseSpeed / attackSpeed(player, stack);
        return Math.max(3, (int) Math.round(
            BallAndChainCombat.RELEASE_ANIMATION_TICKS * speedRatio));
    }

    public static double attackSpeed(EntityPlayer player, ItemStack stack) {
        IAttributeInstance attribute = player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_SPEED);
        double speed = attribute == null ? 0.8D : attribute.getAttributeValue();
        if (ModContent.ROTATION_FORCE != null) {
            speed += 0.2D * EnchantmentHelper.getEnchantmentLevel(
                ModContent.ROTATION_FORCE, stack);
        }
        // Haste did not alter ATTACK_SPEED in vanilla 1.12.2, so chain weapons
        // explicitly interpret it as +10% rotation speed per amplifier level.
        PotionEffect haste = player.getActivePotionEffect(MobEffects.HASTE);
        if (haste != null) {
            speed *= 1.0D + 0.10D * (haste.getAmplifier() + 1);
        }
        if (stack.getItem() instanceof com.nanonaitor.arsenal.item.ItemBallAndChain) {
            if (!player.getHeldItemOffhand().isEmpty()) speed *= 0.50D;
            if (player.getHeldItemOffhand().isEmpty()
                && player.getEntityData().getBoolean("ArsenalBallWindBoost")) speed += 0.30D;
        }
        return Math.max(MIN_ATTACK_SPEED, speed);
    }

    private static double reachBonus(EntityPlayer player, ItemStack stack) {
        return attributeReachBonus(player) + longChainBonus(player, stack);
    }

    private static double attributeReachBonus(EntityPlayer player) {
        IAttributeInstance reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE);
        return reach == null ? 0.0D
            : reach.getAttributeValue() - reach.getAttribute().getDefaultValue();
    }
}
