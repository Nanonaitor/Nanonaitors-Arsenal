package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.WeaponTier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.UUID;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Registers Arsenal Silver weapons with RLCraft's Quicksilver Hands set. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class SilverSetBonusCompat {
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString(
        "563b78c5-187b-42ec-8698-28d8e906d70a");
    private static final double SILVER_ATTACK_SPEED_BONUS = 0.50D;
    private static boolean registeredWithSetBonus;

    private SilverSetBonusCompat() {}

    /**
     * Adds every registered Arsenal Silver weapon to SetBonus's SSetW
     * (Quicksilver Hands) mainhand pool. The SetBonus classes are accessed by
     * reflection so the mod remains optional outside RLCraft installations.
     */
    public static void registerQuicksilverHandsEquipment() {
        if (!Loader.isModLoaded("setbonus")) return;
        try {
            Class<?> dataClass = Class.forName("com.fantasticsource.setbonus.SetBonusData");
            Object serverData = dataClass.getField("SERVER_DATA").get(null);
            Collection<Object> equipment = collectionField(dataClass, serverData, "equipment");
            Collection<Object> sets = collectionField(dataClass, serverData, "sets");
            Class<?> equipClass = Class.forName(
                "com.fantasticsource.setbonus.common.bonusrequirements.setrequirement.Equip");
            Method createEquip = equipClass.getMethod("getInstance", String.class);

            Object quicksilverHands = findById(sets, "SSetW");
            if (quicksilverHands == null) {
                NanonaitorsArsenal.LOGGER.warn(
                    "SetBonus is loaded, but SSetW (Quicksilver Hands) was not found");
                return;
            }
            Field slotDataField = quicksilverHands.getClass().getField("slotData");
            Collection<?> slots = (Collection<?>) slotDataField.get(quicksilverHands);
            if (slots.isEmpty()) return;
            Object mainhandSlot = slots.iterator().next();
            Field involvedField = mainhandSlot.getClass().getField("involvedEquips");
            @SuppressWarnings("unchecked")
            Collection<Object> involvedEquips =
                (Collection<Object>) involvedField.get(mainhandSlot);

            int added = 0;
            for (ItemArsenalWeapon weapon : arsenalSilverWeapons()) {
                String path = weapon.getRegistryName().getResourcePath();
                String equipId = "ArsenalSilver_" + path;
                Object equip = findById(equipment, equipId);
                if (equip == null) {
                    equip = createEquip.invoke(null, equipId + ", "
                        + weapon.getRegistryName());
                    if (equip != null) equipment.add(equip);
                }
                if (equip != null && involvedEquips.add(equip)) added++;
            }
            registeredWithSetBonus = true;
            NanonaitorsArsenal.LOGGER.info(
                "Registered {} Arsenal Silver weapons with Quicksilver Hands", added);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            registeredWithSetBonus = false;
            NanonaitorsArsenal.LOGGER.warn(
                "Could not register Arsenal Silver weapons with Quicksilver Hands; "
                    + "using the numeric compatibility fallback", exception);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        EntityPlayer player = event.player;
        IAttributeInstance speed = player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_SPEED);
        if (speed == null) return;

        AttributeModifier old = speed.getModifier(ATTACK_SPEED_UUID);
        if (old != null) speed.removeModifier(old);
        if (!registeredWithSetBonus && Loader.isModLoaded("setbonus")
            && holdsArsenalSilverWeapon(player)
            && wearsFullSilverArmor(player)) {
            // SetBonus's RLCraft rule is generic.attackSpeed=0.5 @ 1:
            // +50% of the base attack-speed attribute.
            speed.applyModifier(new AttributeModifier(ATTACK_SPEED_UUID,
                "Arsenal Silver Set Bonus compatibility",
                SILVER_ATTACK_SPEED_BONUS, 1).setSaved(false));
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> collectionField(Class<?> owner, Object instance,
                                                       String name)
        throws ReflectiveOperationException {
        return (Collection<Object>) owner.getField(name).get(instance);
    }

    private static Object findById(Collection<?> values, String id)
        throws ReflectiveOperationException {
        for (Object value : values) {
            if (id.equals(value.getClass().getField("id").get(value))) return value;
        }
        return null;
    }

    private static Collection<ItemArsenalWeapon> arsenalSilverWeapons() {
        Collection<ItemArsenalWeapon> result = new LinkedHashSet<>();
        for (net.minecraft.item.Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            if (item instanceof ItemArsenalWeapon
                && ((ItemArsenalWeapon) item).getTier() == WeaponTier.SILVER) {
                result.add((ItemArsenalWeapon) item);
            }
        }
        return result;
    }

    private static boolean holdsArsenalSilverWeapon(EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        return held.getItem() instanceof ItemArsenalWeapon
            && ((ItemArsenalWeapon) held.getItem()).getTier() == WeaponTier.SILVER;
    }

    private static boolean wearsFullSilverArmor(EntityPlayer player) {
        return hasRegistryName(player.getItemStackFromSlot(EntityEquipmentSlot.HEAD),
                "iceandfire:armor_silver_metal_helmet")
            && hasRegistryName(player.getItemStackFromSlot(EntityEquipmentSlot.CHEST),
                "iceandfire:armor_silver_metal_chestplate")
            && hasRegistryName(player.getItemStackFromSlot(EntityEquipmentSlot.LEGS),
                "iceandfire:armor_silver_metal_leggings")
            && hasRegistryName(player.getItemStackFromSlot(EntityEquipmentSlot.FEET),
                "iceandfire:armor_silver_metal_boots");
    }

    private static boolean hasRegistryName(ItemStack stack, String expected) {
        ResourceLocation name = stack.isEmpty() ? null : stack.getItem().getRegistryName();
        return name != null && expected.equals(name.toString());
    }
}
