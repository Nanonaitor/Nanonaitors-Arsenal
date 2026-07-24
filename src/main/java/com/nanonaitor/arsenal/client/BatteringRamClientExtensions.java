package com.nanonaitor.arsenal.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Supplies a low, two-handed carry instead of the vanilla one-arm block pose. */
public final class BatteringRamClientExtensions implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose LOW_CARRY = HumanoidModel.ArmPose.create(
        "NANONAITORS_BATTERING_RAM_LOW_CARRY", true, true,
        BatteringRamClientExtensions::poseLowCarry);
    private static final HumanoidModel.ArmPose BRACED_CHARGE = HumanoidModel.ArmPose.create(
        "NANONAITORS_BATTERING_RAM_BRACED_CHARGE", true, true,
        BatteringRamClientExtensions::poseBracedCharge);

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (!entity.getOffhandItem().isEmpty()) return null;
        return entity.isUsingItem() && entity.getUseItem() == stack ? BRACED_CHARGE : LOW_CARRY;
    }

    private static void poseLowCarry(HumanoidModel<? extends HumanoidRenderState> model,
            HumanoidRenderState state, HumanoidArm arm) {
        // The right hand stays on the rear grip while the left reaches the forward grip.
        model.rightArm.xRot = -1.02F;
        model.rightArm.yRot = -0.18F;
        model.rightArm.zRot = -0.06F;
        model.leftArm.xRot = -1.52F;
        model.leftArm.yRot = 0.22F;
        model.leftArm.zRot = 0.06F;
    }

    private static void poseBracedCharge(HumanoidModel<? extends HumanoidRenderState> model,
            HumanoidRenderState state, HumanoidArm arm) {
        // Pull the elbows inward and level the ram as the player commits to the charge.
        model.rightArm.xRot = -1.20F;
        model.rightArm.yRot = -0.16F;
        model.rightArm.zRot = -0.05F;
        model.leftArm.xRot = -1.62F;
        model.leftArm.yRot = 0.20F;
        model.leftArm.zRot = 0.05F;

        // Keep a committed running stride even when collision briefly slows the player.
        // This makes the pointed end appear to drive through the custom break path.
        float stride = Mth.cos(state.ageInTicks * 0.85F) * 1.15F;
        model.rightLeg.xRot = stride;
        model.leftLeg.xRot = -stride;
        model.rightLeg.yRot = 0.0F;
        model.leftLeg.yRot = 0.0F;
        model.rightLeg.zRot = 0.0F;
        model.leftLeg.zRot = 0.0F;
    }
}
