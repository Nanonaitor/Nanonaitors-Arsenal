package com.nanonaitor.arsenal.registry;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.*;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.Item;



import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ArsenalMod.MOD_ID);
    public static final Map<WeaponKind, Map<WeaponTier, RegistryObject<Item>>> WEAPONS = new EnumMap<>(WeaponKind.class);
    public static final Map<String, RegistryObject<Item>> VISIBLE = new LinkedHashMap<>();
    public static final Map<WeaponTier, RegistryObject<Item>> BALL_VISUALS = new EnumMap<>(WeaponTier.class);
    public static final Map<WeaponTier, RegistryObject<Item>> FLAIL_SPIKE_VISUALS = new EnumMap<>(WeaponTier.class);
    public static final Map<WeaponTier, RegistryObject<Item>> FLAIL_SPIKE_TRAIL_NEAR = new EnumMap<>(WeaponTier.class);
    public static final Map<WeaponTier, RegistryObject<Item>> FLAIL_SPIKE_TRAIL_FAR = new EnumMap<>(WeaponTier.class);
    public static final RegistryObject<Item> CHAIN_LINK_UPRIGHT = visualItem("chain_link_upright");

    public static final RegistryObject<Item> SUN_WAR = registerShield("sun_war_bulwark", ArsenalShieldItem.Type.SUN_WAR, new Item.Properties().durability(4096));
    public static final RegistryObject<Item> TARTSY_SHIELD = registerShield("tartsy_shield", ArsenalShieldItem.Type.TARTSY, new Item.Properties().durability(768));
    static {
        for (WeaponTier tier : WeaponTier.values()) {
            BALL_VISUALS.put(tier, visualItem("ball_visual_" + tier.id));
            FLAIL_SPIKE_VISUALS.put(tier, visualItem("flail_spikeball_visual_" + tier.id));
            FLAIL_SPIKE_TRAIL_NEAR.put(tier, visualItem("flail_spikeball_visual_" + tier.id + "_trail_near"));
            FLAIL_SPIKE_TRAIL_FAR.put(tier, visualItem("flail_spikeball_visual_" + tier.id + "_trail_far"));
        }
        for (WeaponKind kind : WeaponKind.values()) {
            Map<WeaponTier, RegistryObject<Item>> tiers = new EnumMap<>(WeaponTier.class);
            WEAPONS.put(kind, tiers);
            for (WeaponTier tier : WeaponTier.values()) {
                String id = kind.id + "_" + tier.id;
                RegistryObject<Item> object = ITEMS.register(id, () -> {
                    float damage = kind == WeaponKind.CLAWS || kind == WeaponKind.LINKED_CLAWS
                        ? tier.clawDamage() - 1.0F - tier.material.getAttackDamageBonus()
                        : kind == WeaponKind.SCIMITAR
                            ? roundedScimitarDamage(tier) - 1.0F - tier.material.getAttackDamageBonus()
                        : kind.damageBaseline;
                    float speed = kind == WeaponKind.BATTERING_RAM && tier == WeaponTier.GOLD ? -3.0F : kind.speedModifier;
                    Item.Properties properties = new Item.Properties().durability(tier.material.getUses());
                    if (tier == WeaponTier.NETHERITE) properties.fireResistant();
                    return new ArsenalWeaponItem(tier, kind, properties);
                });
                tiers.put(tier, object);
                if (kind != WeaponKind.LINKED_CLAWS) VISIBLE.put(id, object);
            }
        }
    }

    /** Ten percent below the old final value, rounded to the nearest half point. */
    public static float roundedScimitarDamage(WeaponTier tier) {
        float oldFinalDamage = 1.0F + tier.material.getAttackDamageBonus() + WeaponKind.SCIMITAR.damageBaseline;
        return Math.round(oldFinalDamage * 0.90F * 2.0F) / 2.0F;
    }

    private static RegistryObject<Item> registerShield(String id, ArsenalShieldItem.Type type, Item.Properties properties) {
        RegistryObject<Item> object = ITEMS.register(id, () -> new ArsenalShieldItem(type, properties));
        VISIBLE.put(id, object);
        return object;
    }
    private static RegistryObject<Item> visualItem(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }
    public static RegistryObject<Item> get(WeaponKind kind, WeaponTier tier) { return WEAPONS.get(kind).get(tier); }
    private ModItems() {}
}
