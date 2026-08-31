package com.nanonaitor.arsenal.potion;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.registry.ModContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Temporarily suspends mob AI and restores its previous state on expiry. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class PotionStunned extends Potion {
    private static final String MANAGED = "ArsenalStunManaged";
    private static final String PREVIOUS = "ArsenalStunPreviousNoAi";
    private static final ResourceLocation ICON = new ResourceLocation(
        NanonaitorsArsenal.MOD_ID, "textures/mob_effect/stunned.png");

    public PotionStunned() {
        super(true, 0xF6C945);
        setRegistryName(NanonaitorsArsenal.MOD_ID, "stunned");
        setPotionName("effect.nanonaitors_arsenal.stunned");
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase living = event.getEntityLiving();
        if (living.world.isRemote || !(living instanceof EntityLiving)) return;
        EntityLiving mob = (EntityLiving) living;
        NBTTagCompound data = mob.getEntityData();
        boolean stunned = ModContent.STUNNED != null && mob.isPotionActive(ModContent.STUNNED);
        if (stunned) {
            if (!data.getBoolean(MANAGED)) {
                data.setBoolean(PREVIOUS, mob.isAIDisabled());
                data.setBoolean(MANAGED, true);
            }
            mob.setNoAI(true);
        } else if (data.getBoolean(MANAGED)) {
            mob.setNoAI(data.getBoolean(PREVIOUS));
            data.removeTag(MANAGED);
            data.removeTag(PREVIOUS);
        }
    }

    @Override public boolean hasStatusIcon() { return false; }

    @SideOnly(Side.CLIENT)
    @Override public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        draw(mc, x + 6, y + 7, 1.0F);
    }

    @SideOnly(Side.CLIENT)
    @Override public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        draw(mc, x + 3, y + 3, alpha);
    }

    @SideOnly(Side.CLIENT)
    private static void draw(Minecraft mc, int x, int y, float alpha) {
        mc.getTextureManager().bindTexture(ICON);
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 18, 18, 18, 18);
        GlStateManager.color(1, 1, 1, 1);
    }
}
