package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

/** A second item meter, offset above vanilla durability instead of replacing it. */
@Mod.EventBusSubscriber(modid=NanonaitorsArsenal.MOD_ID,value=Side.CLIENT)
public final class BulwarkStrainOverlay {
    private BulwarkStrainOverlay() {}
    private static void bar(ItemStack stack,int x,int y) {
        if (!(stack.getItem() instanceof ItemSunWarBulwark)) return;
        int value=ItemSunWarBulwark.strain(stack);
        if(value<=0)return;
        int width=(int)Math.ceil(13.0D*Math.min(25,value)/25.0D);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        Gui.drawRect(x+2,y+10,x+15,y+12,0xFF302600);
        Gui.drawRect(x+2,y+10,x+2+width,y+11,0xFFFFD42A);
        if (depth) GlStateManager.enableDepth();
        if (lighting) GlStateManager.enableLighting();
    }
    @SubscribeEvent public static void hotbar(RenderGameOverlayEvent.Post event) {
        if(event.getType()!=RenderGameOverlayEvent.ElementType.HOTBAR)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.player==null || mc.player.isSpectator())return;
        int center=event.getResolution().getScaledWidth()/2,y=event.getResolution().getScaledHeight()-19;
        for(int i=0;i<9;i++)bar(mc.player.inventory.mainInventory.get(i),center-90+i*20+2,y);
        bar(mc.player.getHeldItemOffhand(),center+(mc.player.getPrimaryHand()==EnumHandSide.RIGHT?-117:101),y);
    }
    @SubscribeEvent public static void inventory(GuiScreenEvent.DrawScreenEvent.Post event) {
        if(!(event.getGui() instanceof GuiContainer))return;
        GuiContainer gui=(GuiContainer)event.getGui();
        for(Slot slot:gui.inventorySlots.inventorySlots)
            if(slot.isEnabled())bar(slot.getStack(),gui.getGuiLeft()+slot.xPos,gui.getGuiTop()+slot.yPos);
    }
}
