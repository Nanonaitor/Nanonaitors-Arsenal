package com.nanonaitor.arsenal.client;
import com.nanonaitor.arsenal.item.ArsenalShieldItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
public final class ShieldClientExtensions implements IClientItemExtensions {
    private ShieldItemRenderer renderer;
    public static void bootstrap() {}
    @Override public HumanoidModel.ArmPose getArmPose(LivingEntity entity,InteractionHand hand,ItemStack stack){
        // The original display transforms were authored against the vanilla blocking arm,
        // not the 26.1 overhead pose. Keep the model and arm coordinate spaces paired.
        return entity.isUsingItem() && entity.getUseItem()==stack ? HumanoidModel.ArmPose.BLOCK : null;
    }
    @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
        if(renderer==null)renderer=new ShieldItemRenderer();
        return renderer;
    }
}
