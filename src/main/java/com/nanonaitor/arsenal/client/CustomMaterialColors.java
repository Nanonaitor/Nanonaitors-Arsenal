package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Tints custom-material weapons. Their shared *_custom models put the metal
 * on tint index 1 (handles, logs and wood stay untinted on index 0).
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class CustomMaterialColors {
    /** Tint index of the metal parts in the *_custom models. */
    public static final int METAL = 1;

    private CustomMaterialColors() {}

    @SubscribeEvent
    public static void registerColors(ColorHandlerEvent.Item event) {
        List<Item> items = new ArrayList<>();
        for (WeaponTier tier : WeaponTier.values()) {
            if (!tier.isCustom() || !tier.isActive()) continue;
            for (java.util.Map<WeaponTier, ? extends Item> map : java.util.Arrays.asList(
                ModContent.MORNING_STARS, ModContent.SCIMITARS, ModContent.CLAWS, ModContent.FLAILS,
                ModContent.BATTERING_RAMS, ModContent.BALLS_AND_CHAINS, ModContent.LINKED_CLAWS,
                ModContent.DOUBLE_BLADED_SCIMITARS)) {
                Item item = map.get(tier);
                if (item != null) items.add(item);
            }
        }
        if (items.isEmpty()) return;
        event.getItemColors().registerItemColorHandler((stack, tintIndex) -> {
            if (tintIndex != METAL || !(stack.getItem() instanceof ItemArsenalWeapon)) return -1;
            return ((ItemArsenalWeapon) stack.getItem()).getTier().getColor();
        }, items.toArray(new Item[0]));
    }
}
