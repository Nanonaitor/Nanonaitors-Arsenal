package com.nanonaitor.arsenal.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nanonaitor.arsenal.item.ArsenalShieldItem;
import com.nanonaitor.arsenal.client.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Original 1.12 shield geometry, texture layout, and renderer-space transforms. */
public final class ShieldItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final ModelPart bulwark=new ModelSunWarBulwark().bake();
    private final ModelPart tartsy=new ModelTartsyShield().bake();
    public ShieldItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(!(stack.getItem() instanceof ArsenalShieldItem shield))return;
        boolean sun=shield.shieldType()==ArsenalShieldItem.Type.SUN_WAR;
        pose.pushPose();
        // Both 1.12 and 1.20 ItemRenderer already translate by -0.5 before this hook.
        pose.translate(0,sun?-15.0/16:-15.5/16,sun?-1.5/16:1.5/16);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        if(sun){pose.mulPose(Axis.ZP.rotationDegrees(180));pose.scale(1.2F,1.2F,1.2F);}
        var texture=new ResourceLocation("nanonaitors_arsenal","textures/item/"+(sun?"sun_war_bulwark":"tartsy_shield")+".png");
        var vertex=ItemRenderer.getFoilBufferDirect(buffers,RenderType.entityCutoutNoCull(texture),false,stack.hasFoil());
        (sun?bulwark:tartsy).render(pose,vertex,light,overlay);
        pose.popPose();
    }
}
