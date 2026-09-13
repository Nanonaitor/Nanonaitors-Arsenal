package com.nanonaitor.arsenal.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nanonaitor.arsenal.client.AbilityPoseOverride;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs after setupAnim (including animation-mod callbacks), before body and held items draw. */
@Mixin(LivingEntityRenderer.class)
public abstract class AbilityPoseMixin {
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",shift=At.Shift.AFTER))
    private void arsenal$abilityPose(LivingEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){
        var model=((LivingEntityRenderer<?,?>)(Object)this).getModel();
        if(model instanceof HumanoidModel<?> humanoid) AbilityPoseOverride.apply(humanoid,entity,partial);
    }
}
