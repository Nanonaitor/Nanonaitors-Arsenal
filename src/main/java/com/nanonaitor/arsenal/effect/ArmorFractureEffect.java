package com.nanonaitor.arsenal.effect;

import com.nanonaitor.arsenal.ArsenalMod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ArmorFractureEffect extends MobEffect {
    public ArmorFractureEffect() {
        super(MobEffectCategory.HARMFUL, 0x9B2D20);
        addAttributeModifier(Attributes.ARMOR,
            Identifier.fromNamespaceAndPath(ArsenalMod.MOD_ID, "armor_fracture"),
            -0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
