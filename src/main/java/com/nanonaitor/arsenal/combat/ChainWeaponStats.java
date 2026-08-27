package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.enchantment.ModEnchantments;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

public final class ChainWeaponStats {
    public static double flailReach(LivingEntity owner, ItemStack stack) {
        return Math.max(1.0D, 4.0D + attributeReachBonus(owner) + longChainBonus(owner, stack));
    }

    /** Long Chain expands the horizontal orbit only, never the vertical hitbox. */
    public static double flailVerticalReach(LivingEntity owner) {
        return Math.max(1.0D, 4.0D + attributeReachBonus(owner));
    }

    public static double ballWindupReach(LivingEntity owner, ItemStack stack) {
        return Math.max(1.0D, 3.0D + attributeReachBonus(owner) + longChainBonus(owner, stack));
    }

    public static double ballThrowReach(LivingEntity owner, ItemStack stack, int effectiveCharge) {
        return Math.max(1.0D, effectiveCharge * 4.0D + attributeReachBonus(owner)
            + longChainBonus(owner, stack));
    }

    public static int swingIntervalTicks(LivingEntity owner, ItemStack stack) {
        return swingIntervalTicks(owner, stack, false);
    }

    public static int swingIntervalTicks(LivingEntity owner, ItemStack stack, boolean windBoost) {
        return Math.max(1, (int)Math.round(20.0D / attackSpeed(owner, stack, windBoost)));
    }

    /** Scales the complete outward-and-return trip with the same live speed as rotations. */
    public static int ballReleaseAnimationTicks(LivingEntity owner, ItemStack stack) {
        double baseSpeed = stack.getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.BALL_AND_CHAIN
            ? 4.0D + WeaponKind.BALL_AND_CHAIN.speedModifier
            : attackSpeed(owner, stack);
        return Math.max(3, Math.min(200,
            (int)Math.round(16.0D * baseSpeed / attackSpeed(owner, stack))));
    }

    public static double attackSpeed(LivingEntity owner, ItemStack stack) {
        return attackSpeed(owner, stack, false);
    }

    public static double attackSpeed(LivingEntity owner, ItemStack stack, boolean windBoost) {
        double speed = owner.getAttributeValue(Attributes.ATTACK_SPEED)
            + 0.2D * ModEnchantments.level(owner, stack, ModEnchantments.ROTATION_FORCE);
        if (windBoost && stack.getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.BALL_AND_CHAIN) speed += 0.30D;
        // Haste still primarily modifies mining speed, so chain rotations explicitly
        // treat every Haste level as ten percent additional attack/animation speed.
        var haste = owner.getEffect(MobEffects.HASTE);
        if (haste != null) speed *= 1.0D + 0.10D * (haste.getAmplifier() + 1);
        // An occupied offhand slows only Ball & Chain rotations/releases. It is
        // not a general attack-speed debuff and attempting an unavailable guard
        // must not slow unrelated attacks.
        if (stack.getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.BALL_AND_CHAIN
            && !owner.getOffhandItem().isEmpty()) speed *= 0.50D;
        return Math.max(0.05D, speed);
    }

    public static double longChainBonus(LivingEntity owner, ItemStack stack) {
        return ModEnchantments.level(owner, stack, ModEnchantments.LONG_CHAIN);
    }

    private static double attributeReachBonus(LivingEntity owner) {
        // Vanilla survival entity interaction reach is 3 blocks. Attribute
        // modifiers from effects/equipment remain three-dimensional.
        return owner.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) - 3.0D;
    }

    private ChainWeaponStats() {}
}
