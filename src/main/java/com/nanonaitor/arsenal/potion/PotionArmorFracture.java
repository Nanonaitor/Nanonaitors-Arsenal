package com.nanonaitor.arsenal.potion;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class PotionArmorFracture extends Potion {
    private static final String ARMOR_MODIFIER_UUID = "c15a4b14-b9c8-4afb-9af1-13be7ac1b501";
    private static final ResourceLocation ICON = new ResourceLocation(
        NanonaitorsArsenal.MOD_ID, "textures/mob_effect/armor_fracture.png");

    public PotionArmorFracture() {
        super(true, 0x9B2D20);
        setRegistryName(NanonaitorsArsenal.MOD_ID, "armor_fracture");
        setPotionName("effect.nanonaitors_arsenal.armor_fracture");
        registerPotionAttributeModifier(SharedMonsterAttributes.ARMOR,
            ARMOR_MODIFIER_UUID, -0.20D, 2);
    }

    @Override
    public boolean hasStatusIcon() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderInventoryEffect(int x, int y, PotionEffect effect,
                                      Minecraft minecraft) {
        renderIcon(minecraft, x + 6, y + 7, 1.0F);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(int x, int y, PotionEffect effect,
                                Minecraft minecraft, float alpha) {
        renderIcon(minecraft, x + 3, y + 3, alpha);
    }

    @SideOnly(Side.CLIENT)
    private static void renderIcon(Minecraft minecraft, int x, int y, float alpha) {
        minecraft.getTextureManager().bindTexture(ICON);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F,
            18, 18, 18.0F, 18.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
