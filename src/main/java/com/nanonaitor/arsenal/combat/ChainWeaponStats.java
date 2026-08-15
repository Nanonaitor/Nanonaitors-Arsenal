package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.enchantment.ModEnchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public final class ChainWeaponStats {
    public static double flailReach(LivingEntity owner, ItemStack stack) {
        return Math.max(1.0D, 4.0D + reachBonus(owner, stack));
    }

    public static double ballWindupReach(LivingEntity owner, ItemStack stack) {
        return Math.max(1.0D, 3.0D + reachBonus(owner, stack));
    }

    public static double ballThrowReach(LivingEntity owner, ItemStack stack, int effectiveCharge) {
        return Math.max(1.0D, effectiveCharge * 4.0D + reachBonus(owner, stack));
    }

    public static int swingIntervalTicks(LivingEntity owner, ItemStack stack) {
        double speed = owner.getAttributeValue(Attributes.ATTACK_SPEED)
            + 0.2D * ModEnchantments.level(owner, stack, ModEnchantments.ROTATION_FORCE);
        return Math.max(1, (int)Math.round(20.0D / Math.max(0.05D, speed)));
    }

    private static double reachBonus(LivingEntity owner, ItemStack stack) {
        // Vanilla survival entity interaction reach is 3 blocks. Attribute
        // modifiers from effects/equipment and Long Chain stack additively.
        return owner.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) - 3.0D
            + ModEnchantments.level(owner, stack, ModEnchantments.LONG_CHAIN);
    }

    private ChainWeaponStats() {}
}
