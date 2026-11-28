package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

/**
 * Custom materials on the client. Their items have no model files of their own:
 * each uses the shared *_custom model of its weapon, whose metal sits on a
 * tinted layer (tint index 1 on weapons) coloured with the material's colour.
 */
public final class CustomMaterialClient {
    /** Tint index of the metal parts in the *_custom weapon models. */
    public static final int METAL = 1;

    private static ResourceLocation model(String name) {
        return new ResourceLocation(ArsenalMod.MOD_ID, "item/" + name);
    }

    /** {item id, shared model} for every custom item. */
    private static List<String[]> pairs() {
        List<String[]> pairs = new ArrayList<>();
        for (WeaponTier tier : WeaponTier.activeValues()) {
            if (!tier.custom()) continue;
            for (WeaponKind kind : WeaponKind.values()) {
                String shared = (kind == WeaponKind.LINKED_CLAWS ? "claws" : kind.id) + "_custom";
                pairs.add(new String[] {kind.id + "_" + tier.id, shared});
            }
            pairs.add(new String[] {"ball_visual_" + tier.id, "ball_visual_custom"});
            pairs.add(new String[] {"flail_spikeball_visual_" + tier.id, "flail_spikeball_visual_custom"});
            pairs.add(new String[] {"flail_spikeball_visual_" + tier.id + "_trail_near", "flail_spikeball_visual_custom_trail_near"});
            pairs.add(new String[] {"flail_spikeball_visual_" + tier.id + "_trail_far", "flail_spikeball_visual_custom_trail_far"});
        }
        return pairs;
    }

    static void registerModels(ModelEvent.RegisterAdditional event) {
        if (!com.nanonaitor.arsenal.config.CustomMaterials.any()) return;
        pairs().stream().map(p -> p[1]).distinct().forEach(name -> event.register(model(name)));
    }

    static void useSharedModels(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        for (String[] pair : pairs()) {
            var shared = models.get(model(pair[1]));
            if (shared != null) models.put(new ModelResourceLocation(new ResourceLocation(ArsenalMod.MOD_ID, pair[0]), "inventory"), shared);
        }
    }

    static void registerColors(RegisterColorHandlersEvent.Item event) {
        for (WeaponTier tier : WeaponTier.activeValues()) {
            if (!tier.custom()) continue;
            int argb = 0xFF000000 | tier.color();
            List<Item> weapons = new ArrayList<>();
            for (WeaponKind kind : WeaponKind.values()) weapons.add(ModItems.get(kind, tier).get());
            event.register((stack, tint) -> tint == METAL ? argb : -1, weapons.toArray(new Item[0]));
            // Ball heads tint their metal faces (index 1); spike-ball sprites their single layer (index 0).
            event.register((stack, tint) -> tint >= 0 ? argb : -1, ModItems.BALL_VISUALS.get(tier).get(),
                ModItems.FLAIL_SPIKE_VISUALS.get(tier).get(), ModItems.FLAIL_SPIKE_TRAIL_NEAR.get(tier).get(),
                ModItems.FLAIL_SPIKE_TRAIL_FAR.get(tier).get());
        }
    }

    private CustomMaterialClient() {}
}
