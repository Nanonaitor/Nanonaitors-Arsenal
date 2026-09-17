package com.nanonaitor.arsenal.client;
import com.nanonaitor.arsenal.item.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;

/** Only active Arsenal abilities own the final pose; ordinary movement is untouched. */
public final class AbilityPoseOverride {
    public static void apply(HumanoidModel<?> model,LivingEntity entity,float partial){
        if(!(entity.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon))return;
        boolean local=entity==Minecraft.getInstance().player;
        boolean using=entity.isUsingItem() && entity.getUseItem()==entity.getMainHandItem();
        boolean active=false;
        HumanoidArm arm=entity.getMainArm();
        switch(weapon.kind()){
            case MORNING_STAR -> {
                boolean charge=local?ClientControls.morningStarCharging():using;
                if(charge){MorningStarClientExtensions.poseRaised(model,entity,arm);active=true;}
                else if(local && ClientControls.morningStarSwing(entity.level().getGameTime())){
                    MorningStarClientExtensions.poseHorizontal(model,entity,arm);active=true;
                }
            }
            case BATTERING_RAM -> {
                if((local?ClientControls.ramActive():using) && entity.getOffhandItem().isEmpty()){
                    BatteringRamClientExtensions.poseBracedCharge(model,entity,arm);active=true;
                }
            }
            case SCIMITAR -> {
                if(entity instanceof net.minecraft.world.entity.player.Player p
                    && com.nanonaitor.arsenal.combat.ParityRules.disabled(p)) break;
                if(entity.isUsingItem() && entity.getOffhandItem().getItem() instanceof ArsenalWeaponItem off
                    && off.kind()==WeaponKind.SCIMITAR){
                    ScimitarClientExtensions.poseCrossed(model,entity,arm);active=true;
                }
            }
            case BLADE_STAFF -> {
                if(AbilityVisualState.staffReflecting(entity)){BladeStaffClientExtensions.pose(model,entity,arm);active=true;}
            }
            default -> {}
        }
        if(active && model instanceof PlayerModel<?> player){
            player.rightSleeve.copyFrom(model.rightArm);player.leftSleeve.copyFrom(model.leftArm);
            player.rightPants.copyFrom(model.rightLeg);player.leftPants.copyFrom(model.leftLeg);
            player.jacket.copyFrom(model.body);
        }
    }
}
