package com.nanonaitor.arsenal.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Third-person two-ended defensive spin used during the one-second reflect window. */
public final class BladeStaffClientExtensions implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose SPIN = HumanoidModel.ArmPose.create(
        "NANONAITORS_BLADE_STAFF_SPIN", false, true, BladeStaffClientExtensions::pose);

    @Override public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return hand == InteractionHand.MAIN_HAND && entity.isUsingItem() && entity.getUseItem() == stack ? SPIN : null;
    }

    private static void pose(HumanoidModel<? extends HumanoidRenderState> model,
            HumanoidRenderState state, HumanoidArm arm) {
        var active = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        // Brace the arm at the center. The item renderer rotates the staff on
        // its own center axis; rotating the arm made the whole weapon orbit the
        // player's shoulder in a wide, incorrect circle.
        active.xRot = -1.15F;
        active.yRot = arm == HumanoidArm.RIGHT ? -0.25F : 0.25F;
        active.zRot = arm == HumanoidArm.RIGHT ? 0.08F : -0.08F;
    }
}
