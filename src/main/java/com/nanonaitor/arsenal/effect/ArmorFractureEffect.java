package com.nanonaitor.arsenal.effect;

import com.nanonaitor.arsenal.ArsenalMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ArmorFractureEffect extends MobEffect {
    public ArmorFractureEffect() {
        super(MobEffectCategory.HARMFUL, 0x9B2D20);
        addAttributeModifier(Attributes.ARMOR,
            "3a695ca7-7b71-4f26-91c0-a67e225ce358",
            -0.20D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
