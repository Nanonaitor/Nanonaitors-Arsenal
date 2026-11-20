package com.nanonaitor.arsenal.client.pose;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Puts {@link ArsenalPlayerModel} into Minecraft's own two player renderers,
 * once each, the first time they draw a player.
 *
 * Compatibility rules: renderers created by other mods are left alone; a model
 * already replaced by another mod (anything that is not exactly ModelPlayer)
 * is left alone; with Mo' Bends installed nothing is replaced, because
 * Mo' Bends drives the skeleton and {@link MoBendsPoses} poses through it.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class PoseInstaller {
    private static final Set<RenderPlayer> CHECKED =
        Collections.newSetFromMap(new WeakHashMap<RenderPlayer, Boolean>());
    private static boolean failed;

    private PoseInstaller() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforePlayerRender(RenderPlayerEvent.Pre event) {
        RenderPlayer renderer = event.getRenderer();
        if (failed || !CHECKED.add(renderer) || Loader.isModLoaded("mobends")) return;
        try {
            install(renderer);
        } catch (RuntimeException | LinkageError exception) {
            failed = true;
            NanonaitorsArsenal.LOGGER.warn("Arsenal weapon poses disabled: player renderer could not be adapted", exception);
        }
    }

    private static boolean anyInstalled;

    /** True when this renderer draws players with Arsenal's posing model. */
    public static boolean isInstalled(RenderPlayer renderer) {
        return renderer.getMainModel() instanceof ArsenalPlayerModel;
    }

    /** True once at least one player renderer uses Arsenal's posing model. */
    public static boolean anyInstalled() {
        return anyInstalled;
    }

    private static void install(RenderPlayer renderer) {
        Map<String, RenderPlayer> skins = Minecraft.getMinecraft().getRenderManager().getSkinMap();
        boolean slim;
        if (renderer == skins.get("slim")) slim = true;
        else if (renderer == skins.get("default")) slim = false;
        else return;
        ModelPlayer old = renderer.getMainModel();
        if (old.getClass() != ModelPlayer.class) {
            NanonaitorsArsenal.LOGGER.info("Arsenal weapon poses: player model already replaced by {}; leaving it alone",
                old.getClass().getName());
            return;
        }
        ArsenalPlayerModel model = new ArsenalPlayerModel(0.0F, slim);
        Field mainModel = ReflectionHelper.findField(RenderLivingBase.class, "mainModel", "field_77045_g");
        Field layers = ReflectionHelper.findField(RenderLivingBase.class, "layerRenderers", "field_177097_h");
        Field leggings = ReflectionHelper.findField(LayerArmorBase.class, "modelLeggings", "field_177189_c");
        Field armor = ReflectionHelper.findField(LayerArmorBase.class, "modelArmor", "field_177186_d");
        Field headPart = ReflectionHelper.findField(LayerCustomHead.class, "modelRenderer", "field_177209_a");
        try {
            mainModel.set(renderer, model);
            for (Object layer : (List<?>) layers.get(renderer)) {
                if (layer.getClass() == LayerBipedArmor.class) {
                    // Only swap vanilla's plain armor models; custom ones stay.
                    if (leggings.get(layer).getClass() == ModelBiped.class) leggings.set(layer, new ArsenalArmorModel(0.5F));
                    if (armor.get(layer).getClass() == ModelBiped.class) armor.set(layer, new ArsenalArmorModel(1.0F));
                } else if (layer.getClass() == LayerCustomHead.class
                    && headPart.get(layer) == old.bipedHead) {
                    // Skulls and pumpkins follow the head part they were built with.
                    headPart.set(layer, (ModelRenderer) model.bipedHead);
                }
            }
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
        anyInstalled = true;
        NanonaitorsArsenal.LOGGER.info("Arsenal weapon poses enabled for the {} player model", slim ? "slim" : "default");
    }
}
