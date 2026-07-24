package com.nanonaitor.arsenal.registry;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.*;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
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
    public static final RegistryObject<Item> CHAIN_LINK_FLAT = visualItem("chain_link_flat");
    public static final RegistryObject<Item> CHAIN_LINK_UPRIGHT = visualItem("chain_link_upright");

    public static final RegistryObject<Item> SUN_WAR = registerShield("sun_war_bulwark", ArsenalShieldItem.Type.SUN_WAR,
        new Item.Properties().setId(ITEMS.key("sun_war_bulwark"))
            .durability(4096)
            .enchantable(15)
            .attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 0.0D,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.75D,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build())
            .component(DataComponents.USE_EFFECTS, new UseEffects(false, true, 1.0F)));

    static {
        for (WeaponTier tier : WeaponTier.values()) {
            BALL_VISUALS.put(tier, visualItem("ball_visual_" + tier.id));
        }
        for (WeaponKind kind : WeaponKind.values()) {
            Map<WeaponTier, RegistryObject<Item>> tiers = new EnumMap<>(WeaponTier.class);
            WEAPONS.put(kind, tiers);
            for (WeaponTier tier : WeaponTier.values()) {
                String id = kind.id + "_" + tier.id;
                RegistryObject<Item> object = ITEMS.register(id, () -> {
                    float damage = kind == WeaponKind.CLAWS || kind == WeaponKind.LINKED_CLAWS
                        ? tier.clawDamage() - 1.0F - tier.material.attackDamageBonus()
                        : kind.damageBaseline;
                    float speed = kind == WeaponKind.BATTERING_RAM && tier == WeaponTier.GOLD ? -3.0F : kind.speedModifier;
                    Item.Properties properties = new Item.Properties().setId(ITEMS.key(id))
                        .sword(tier.material, damage, speed)
                        .enchantable(tier.material.enchantmentValue());
                    if (tier == WeaponTier.NETHERITE) properties.fireResistant();
                    return new ArsenalWeaponItem(tier, kind, properties);
                });
                tiers.put(tier, object);
                if (kind != WeaponKind.LINKED_CLAWS) VISIBLE.put(id, object);
            }
        }
    }

    private static RegistryObject<Item> registerShield(String id, ArsenalShieldItem.Type type, Item.Properties properties) {
        RegistryObject<Item> object = ITEMS.register(id, () -> new ArsenalShieldItem(type, properties));
        VISIBLE.put(id, object);
        return object;
    }
    private static RegistryObject<Item> visualItem(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().setId(ITEMS.key(id))));
    }
    public static RegistryObject<Item> get(WeaponKind kind, WeaponTier tier) { return WEAPONS.get(kind).get(tier); }
    private ModItems() {}
}
