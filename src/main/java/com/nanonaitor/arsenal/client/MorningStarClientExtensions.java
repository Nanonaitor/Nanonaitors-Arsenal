package com.nanonaitor.arsenal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Raised charge and broad release poses for the Morning Star. */
public final class MorningStarClientExtensions implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose RAISED = HumanoidModel.ArmPose.create(
        "NANONAITORS_MORNING_STAR_RAISED", false, MorningStarClientExtensions::poseRaised);
    private static final HumanoidModel.ArmPose HORIZONTAL = HumanoidModel.ArmPose.create(
        "NANONAITORS_MORNING_STAR_HORIZONTAL", false, MorningStarClientExtensions::poseHorizontal);

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (hand != InteractionHand.MAIN_HAND || entity.getMainHandItem() != stack
            || !(stack.getItem() instanceof com.nanonaitor.arsenal.item.ArsenalWeaponItem weapon)
            || weapon.kind() != com.nanonaitor.arsenal.item.WeaponKind.MORNING_STAR) return null;
        boolean charging = entity == Minecraft.getInstance().player
            ? ClientControls.morningStarCharging()
            : entity.isUsingItem()
                && entity.getUseItem().getItem() instanceof com.nanonaitor.arsenal.item.ArsenalWeaponItem used
                && used.kind() == com.nanonaitor.arsenal.item.WeaponKind.MORNING_STAR;
        if (charging) return RAISED;
        if (entity.swinging && entity.swingingArm == InteractionHand.MAIN_HAND) return HORIZONTAL;
        return null;
    }

    static void poseRaised(HumanoidModel<? extends LivingEntity> model,
            LivingEntity state, HumanoidArm arm) {
        var active = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        var passive = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        active.xRot = -2.75F;
        active.yRot = arm == HumanoidArm.RIGHT ? -0.30F : 0.30F;
        active.zRot = arm == HumanoidArm.RIGHT ? 0.28F : -0.28F;
        passive.xRot = 0.0F;
        passive.yRot = 0.0F;
        passive.zRot = 0.0F;
    }

    static void poseHorizontal(HumanoidModel<? extends LivingEntity> model,
            LivingEntity state, HumanoidArm arm) {
        var active = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        var passive = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        active.xRot = -1.30F;
        active.yRot = arm == HumanoidArm.RIGHT ? -1.22F : 1.22F;
        active.zRot = arm == HumanoidArm.RIGHT ? 0.18F : -0.18F;
        passive.xRot = 0.0F;
        passive.yRot = 0.0F;
        passive.zRot = 0.0F;
        model.body.yRot = arm == HumanoidArm.RIGHT ? -0.30F : 0.30F;
    }
}
