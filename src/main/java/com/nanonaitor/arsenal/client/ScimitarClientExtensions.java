package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Crossed dual-scimitar guard. */
public final class ScimitarClientExtensions implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose CROSSED = HumanoidModel.ArmPose.create(
        "NANONAITORS_SCIMITAR_CROSSED", true, true, ScimitarClientExtensions::poseCrossed);

    @Override public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        if (!entity.isUsingItem()
            || !(entity.getUseItem().getItem() instanceof ArsenalWeaponItem used)
            || used.kind() != WeaponKind.SCIMITAR || !dualScimitars(entity)) return null;
        return CROSSED;
    }

    private static boolean dualScimitars(LivingEntity entity) {
        return entity.getMainHandItem().getItem() instanceof ArsenalWeaponItem main
            && entity.getOffhandItem().getItem() instanceof ArsenalWeaponItem off
            && main.kind() == WeaponKind.SCIMITAR && off.kind() == WeaponKind.SCIMITAR;
    }

    private static void poseCrossed(HumanoidModel<? extends HumanoidRenderState> model,
            HumanoidRenderState state, HumanoidArm arm) {
        model.rightArm.xRot = -1.42F;
        model.rightArm.yRot = 0.52F;
        model.rightArm.zRot = 0.30F;
        model.leftArm.xRot = -1.42F;
        model.leftArm.yRot = -0.52F;
        model.leftArm.zRot = -0.30F;
    }
}
