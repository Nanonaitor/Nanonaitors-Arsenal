package com.nanonaitor.arsenal.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Two-handed brace used when the idle Ball & Chain is guarding. */
public final class BallChainClientExtensions implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose GUARD = HumanoidModel.ArmPose.create(
        "NANONAITORS_BALL_CHAIN_GUARD", true, true, BallChainClientExtensions::poseGuard);

    @Override public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (!entity.isUsingItem() || entity.getUseItem() != stack || !entity.getOffhandItem().isEmpty()
            || attackVisualActive(stack)) return null;
        return GUARD;
    }

    private static boolean attackVisualActive(ItemStack stack) {
        CustomModelData data = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        return !data.flags().isEmpty() && data.flags().get(0);
    }

    private static void poseGuard(HumanoidModel<? extends HumanoidRenderState> model,
            HumanoidRenderState state, HumanoidArm arm) {
        model.rightArm.xRot = -1.35F;
        model.rightArm.yRot = -0.30F;
        model.rightArm.zRot = -0.20F;
        model.leftArm.xRot = -1.35F;
        model.leftArm.yRot = 0.30F;
        model.leftArm.zRot = 0.20F;
    }
}
