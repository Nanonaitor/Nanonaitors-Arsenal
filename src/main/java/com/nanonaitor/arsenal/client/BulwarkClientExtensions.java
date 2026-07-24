package com.nanonaitor.arsenal.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class BulwarkClientExtensions implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose TWO_HANDED_CARRY = HumanoidModel.ArmPose.create(
        "NANONAITORS_BULWARK_CARRY", true, true, BulwarkClientExtensions::poseCarry);
    private static final HumanoidModel.ArmPose OVERHEAD_GUARD = HumanoidModel.ArmPose.create(
        "NANONAITORS_BULWARK_OVERHEAD_GUARD", true, true, BulwarkClientExtensions::poseGuard);

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (!entity.getOffhandItem().isEmpty()) return null;
        return entity.isUsingItem() && entity.getUseItem() == stack ? OVERHEAD_GUARD : TWO_HANDED_CARRY;
    }

    private static void poseCarry(HumanoidModel<? extends HumanoidRenderState> model,
            HumanoidRenderState state, HumanoidArm arm) {
        model.rightArm.xRot = -1.12F;
        model.rightArm.yRot = -0.42F;
        model.rightArm.zRot = -0.10F;
        model.leftArm.xRot = -1.12F;
        model.leftArm.yRot = 0.42F;
        model.leftArm.zRot = 0.10F;
    }

    private static void poseGuard(HumanoidModel<? extends HumanoidRenderState> model,
            HumanoidRenderState state, HumanoidArm arm) {
        model.rightArm.xRot = -2.35F;
        model.rightArm.yRot = -0.52F;
        model.rightArm.zRot = -0.20F;
        model.leftArm.xRot = -2.35F;
        model.leftArm.yRot = 0.52F;
        model.leftArm.zRot = 0.20F;
        state.isCrouching = true;
    }
}
