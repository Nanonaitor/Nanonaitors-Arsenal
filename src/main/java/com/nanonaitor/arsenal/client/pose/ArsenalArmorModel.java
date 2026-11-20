package com.nanonaitor.arsenal.client.pose;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Vanilla armor model whose sleeves and chestplate follow the weapon pose. */
@SideOnly(Side.CLIENT)
public final class ArsenalArmorModel extends ModelBiped {
    public ArsenalArmorModel(float modelSize) {
        super(modelSize);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scaleFactor,
                                  Entity entity) {
        WeaponPoses.prepareSwing(this, entity, ageInTicks);
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
            scaleFactor, entity);
        WeaponPoses.applyTo(this, entity, ageInTicks);
    }
}
