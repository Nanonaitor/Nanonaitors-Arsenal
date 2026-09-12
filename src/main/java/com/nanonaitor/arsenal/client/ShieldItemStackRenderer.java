package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.client.model.ModelSunWarBulwark;
import com.nanonaitor.arsenal.client.model.ModelTartsyShield;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import com.nanonaitor.arsenal.item.ItemTartsyShield;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Renders the supplied Blockbench entity models without lossy item-JSON conversion. */
@SideOnly(Side.CLIENT)
public final class ShieldItemStackRenderer extends TileEntityItemStackRenderer {
    private static final ResourceLocation TARTSY_TEXTURE = new ResourceLocation(
        NanonaitorsArsenal.MOD_ID, "textures/items/tartsy_shield.png");
    private static final ResourceLocation SUN_WAR_TEXTURE = new ResourceLocation(
        NanonaitorsArsenal.MOD_ID, "textures/items/sun_war_bulwark.png");
    private static final float PIXEL = 1.0F / 16.0F;

    private final ModelTartsyShield tartsy = new ModelTartsyShield();
    private final ModelSunWarBulwark sunWar = new ModelSunWarBulwark();

    @Override
    public void renderByItem(ItemStack stack) {
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (stack.getItem() instanceof ItemTartsyShield) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(TARTSY_TEXTURE);
            // Center the supplied entity-model coordinates around item space.
            GlStateManager.translate(0.0F, -15.5F * PIXEL, 1.5F * PIXEL);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            tartsy.render(PIXEL);
        } else if (stack.getItem() instanceof ItemSunWarBulwark) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(SUN_WAR_TEXTURE);
            GlStateManager.translate(0.0F, -15.0F * PIXEL, -1.5F * PIXEL);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            // The Bulwark is intentionally oversized to read as a true
            // two-handed fortress shield in every render context.
            GlStateManager.scale(1.2F, 1.2F, 1.2F);
            sunWar.render(PIXEL);
        }
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }
}
