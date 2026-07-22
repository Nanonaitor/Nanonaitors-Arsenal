package com.nanonaitor.arsenal.potion;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;

public final class PotionArmorFracture extends Potion {
    private static final String ARMOR_MODIFIER_UUID = "c15a4b14-b9c8-4afb-9af1-13be7ac1b501";

    public PotionArmorFracture() {
        super(true, 0x9B2D20);
        setRegistryName(NanonaitorsArsenal.MOD_ID, "armor_fracture");
        setPotionName("effect.nanonaitors_arsenal.armor_fracture");
        setIconIndex(2, 1);
        registerPotionAttributeModifier(SharedMonsterAttributes.ARMOR,
            ARMOR_MODIFIER_UUID, -0.20D, 2);
    }
}
