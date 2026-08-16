package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class BatteringRamAnimationHandler {
    private static final Map<EntityPlayer, PreviousPose> PREVIOUS = new WeakHashMap<>();

    private BatteringRamAnimationHandler() {}

    @SubscribeEvent
    public static void beforePlayerRender(RenderPlayerEvent.Pre event) {
        EntityPlayer player = event.getEntityPlayer();
        boolean ram = player.getHeldItemMainhand().getItem() instanceof ItemBatteringRam;
        boolean bulwark = player.getHeldItemMainhand().getItem() instanceof ItemSunWarBulwark;
        if ((!ram && !bulwark) || !ArsenalCompatManager.canUseTwoHanded(player)) return;
        ModelPlayer model = event.getRenderer().getMainModel();
        PREVIOUS.put(player, new PreviousPose(model));
        boolean active = ram ? BatteringRamInputHandler.isCharging(player)
            : player.isHandActive()
                && player.getActiveItemStack() == player.getHeldItemMainhand();
        if (ram) {
            model.leftArmPose = ModelBiped.ArmPose.EMPTY;
            model.rightArmPose = ModelBiped.ArmPose.EMPTY;
            poseRam(model, player, active, event.getPartialRenderTick());
        } else {
            // Raw arm rotations are recalculated by ModelBiped in 1.12.  BLOCK is
            // persistent through that pass and keeps both hands raised around the
            // Bulwark in vanilla and animation-free modpack profiles.
            model.leftArmPose = ModelBiped.ArmPose.BLOCK;
            model.rightArmPose = ModelBiped.ArmPose.BLOCK;
            if (active) model.isSneak = true;
        }
    }

    @SubscribeEvent
    public static void afterPlayerRender(RenderPlayerEvent.Post event) {
        PreviousPose previous = PREVIOUS.remove(event.getEntityPlayer());
        if (previous == null) {
            return;
        }
        ModelPlayer model = event.getRenderer().getMainModel();
        previous.restore(model);
    }

    private static void poseRam(ModelPlayer model, EntityPlayer player,
                                boolean charging, float partialTicks) {
        model.bipedRightArm.rotateAngleX = charging ? -1.20F : -1.02F;
        model.bipedRightArm.rotateAngleY = charging ? -0.16F : -0.18F;
        model.bipedRightArm.rotateAngleZ = charging ? -0.05F : -0.06F;
        model.bipedLeftArm.rotateAngleX = charging ? -1.62F : -1.52F;
        model.bipedLeftArm.rotateAngleY = charging ? 0.20F : 0.22F;
        model.bipedLeftArm.rotateAngleZ = charging ? 0.05F : 0.06F;
        if (charging) {
            float stride = MathHelper.cos((player.ticksExisted + partialTicks) * 0.85F) * 1.15F;
            model.bipedRightLeg.rotateAngleX = stride;
            model.bipedLeftLeg.rotateAngleX = -stride;
            model.bipedRightLeg.rotateAngleY = model.bipedLeftLeg.rotateAngleY = 0.0F;
            model.bipedRightLeg.rotateAngleZ = model.bipedLeftLeg.rotateAngleZ = 0.0F;
        }
    }

    private static final class PreviousPose {
        private final ModelBiped.ArmPose leftPose, rightPose;
        private final float[] angles = new float[12];
        private final boolean sneaking;

        private PreviousPose(ModelPlayer model) {
            leftPose = model.leftArmPose; rightPose = model.rightArmPose;
            angles[0]=model.bipedLeftArm.rotateAngleX; angles[1]=model.bipedLeftArm.rotateAngleY;
            angles[2]=model.bipedLeftArm.rotateAngleZ; angles[3]=model.bipedRightArm.rotateAngleX;
            angles[4]=model.bipedRightArm.rotateAngleY; angles[5]=model.bipedRightArm.rotateAngleZ;
            angles[6]=model.bipedLeftLeg.rotateAngleX; angles[7]=model.bipedLeftLeg.rotateAngleY;
            angles[8]=model.bipedLeftLeg.rotateAngleZ; angles[9]=model.bipedRightLeg.rotateAngleX;
            angles[10]=model.bipedRightLeg.rotateAngleY; angles[11]=model.bipedRightLeg.rotateAngleZ;
            sneaking = model.isSneak;
        }
        private void restore(ModelPlayer model) {
            model.leftArmPose=leftPose; model.rightArmPose=rightPose;
            model.bipedLeftArm.rotateAngleX=angles[0]; model.bipedLeftArm.rotateAngleY=angles[1];
            model.bipedLeftArm.rotateAngleZ=angles[2]; model.bipedRightArm.rotateAngleX=angles[3];
            model.bipedRightArm.rotateAngleY=angles[4]; model.bipedRightArm.rotateAngleZ=angles[5];
            model.bipedLeftLeg.rotateAngleX=angles[6]; model.bipedLeftLeg.rotateAngleY=angles[7];
            model.bipedLeftLeg.rotateAngleZ=angles[8]; model.bipedRightLeg.rotateAngleX=angles[9];
            model.bipedRightLeg.rotateAngleY=angles[10]; model.bipedRightLeg.rotateAngleZ=angles[11];
            model.isSneak=sneaking;
        }
    }
}
