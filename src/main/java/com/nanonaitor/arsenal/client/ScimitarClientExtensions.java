package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraft.client.Minecraft;

/** Crossed dual-scimitar guard. */
public final class ScimitarClientExtensions implements IClientItemExtensions {
    private static final HumanoidModel.ArmPose CROSSED = HumanoidModel.ArmPose.create(
        "NANONAITORS_SCIMITAR_CROSSED", true, ScimitarClientExtensions::poseCrossed);

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

    /** Force both arms into the crossed guard before the humanoid model is posed. */
    public static void beforeRender(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity=event.getEntity();
        if(entity.isUsingItem() && dualScimitars(entity) && event.getRenderer().getModel() instanceof HumanoidModel<?> model) {
            model.rightArmPose=CROSSED; model.leftArmPose=CROSSED;
        }
    }

    static void poseCrossed(HumanoidModel<? extends LivingEntity> model,
            LivingEntity state, HumanoidArm arm) {
        model.rightArm.xRot = -1.42F;
        // Turn both forearms inward so the blades form an X in front of the
        // torso. The previous signs opened the arms outward in a raised V.
        model.rightArm.yRot = -0.62F;
        model.rightArm.zRot = -0.24F;
        model.leftArm.xRot = -1.42F;
        model.leftArm.yRot = 0.62F;
        model.leftArm.zRot = 0.24F;
    }
}
