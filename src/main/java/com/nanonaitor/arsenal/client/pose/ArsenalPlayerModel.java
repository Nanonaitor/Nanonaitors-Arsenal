package com.nanonaitor.arsenal.client.pose;

import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Vanilla player model that applies Arsenal's weapon pose after vanilla's own
 * angle pass. 1.12 has no model-pose event and recalculates arm angles inside
 * render, so this is the only point where a pose survives to the screen.
 */
@SideOnly(Side.CLIENT)
public final class ArsenalPlayerModel extends ModelPlayer {
    public ArsenalPlayerModel(float modelSize, boolean smallArms) {
        super(modelSize, smallArms);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, float scaleFactor,
                                  Entity entity) {
        WeaponPoses.prepareSwing(this, entity, ageInTicks);
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
            scaleFactor, entity);
        if (WeaponPoses.applyTo(this, entity, ageInTicks)) {
            copyModelAngles(bipedRightArm, bipedRightArmwear);
            copyModelAngles(bipedLeftArm, bipedLeftArmwear);
            copyModelAngles(bipedBody, bipedBodyWear);
        }
    }
}
