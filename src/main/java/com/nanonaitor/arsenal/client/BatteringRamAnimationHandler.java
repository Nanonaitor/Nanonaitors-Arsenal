package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.player.EntityPlayer;
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
        if (!(player.getHeldItemMainhand().getItem() instanceof ItemBatteringRam)
            || !player.isHandActive()) {
            return;
        }
        ModelPlayer model = event.getRenderer().getMainModel();
        PREVIOUS.put(player, new PreviousPose(model.leftArmPose, model.rightArmPose));
        model.leftArmPose = ModelBiped.ArmPose.BLOCK;
        model.rightArmPose = ModelBiped.ArmPose.BLOCK;
    }

    @SubscribeEvent
    public static void afterPlayerRender(RenderPlayerEvent.Post event) {
        PreviousPose previous = PREVIOUS.remove(event.getEntityPlayer());
        if (previous == null) {
            return;
        }
        ModelPlayer model = event.getRenderer().getMainModel();
        model.leftArmPose = previous.left;
        model.rightArmPose = previous.right;
    }

    private static final class PreviousPose {
        private final ModelBiped.ArmPose left;
        private final ModelBiped.ArmPose right;

        private PreviousPose(ModelBiped.ArmPose left, ModelBiped.ArmPose right) {
            this.left = left;
            this.right = right;
        }
    }
}
