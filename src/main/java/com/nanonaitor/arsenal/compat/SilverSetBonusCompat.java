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
import net.minecraft.entity.EntityLivingBase;
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

/** Registers Arsenal material tiers with RLCraft's configured SetBonus sets. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class SilverSetBonusCompat {
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString(
        "563b78c5-187b-42ec-8698-28d8e906d70a");
    private static final double SILVER_ATTACK_SPEED_BONUS = 0.50D;
    private static boolean registeredWithSetBonus;
    private static boolean goldRegisteredWithSetBonus;

    private SilverSetBonusCompat() {}

    /**
     * Adds every registered Arsenal Silver weapon to SetBonus's SSetW
     * (Quicksilver Hands) mainhand pool. The SetBonus classes are accessed by
     * reflection so the mod remains optional outside RLCraft installations.
     */
    public static void registerRlcraftEquipmentSets() {
        if (!Loader.isModLoaded("setbonus")) return;
        try {
            Class<?> dataClass = Class.forName("com.fantasticsource.setbonus.SetBonusData");
            Object serverData = dataClass.getField("SERVER_DATA").get(null);
            Collection<Object> equipment = collectionField(dataClass, serverData, "equipment");
            Collection<Object> sets = collectionField(dataClass, serverData, "sets");
            Collection<Object> bonuses = collectionField(dataClass, serverData, "bonuses");
            Class<?> equipClass = Class.forName(
                "com.fantasticsource.setbonus.common.bonusrequirements.setrequirement.Equip");
            Method createEquip = equipClass.getMethod("getInstance", String.class);

            // These IDs and pairings come from RLCraft's custom SetBonus data.
            // Their presence is the pack detection; other packs are untouched.
            Object quicksilverHands = findById(sets, "SSetW");
            if (quicksilverHands != null && findById(bonuses, "SBonusW") != null) {
                int silverAdded = registerTier(equipment, quicksilverHands,
                    createEquip, WeaponTier.SILVER, "ArsenalSilver_");
                registeredWithSetBonus = silverAdded > 0;
                NanonaitorsArsenal.LOGGER.info(
                    "Registered {} Arsenal Silver weapons with Quicksilver Hands",
                    silverAdded);
            }

            Object magicInfusedWeapons = findById(sets, "GSetW");
            if (magicInfusedWeapons != null
                && findById(bonuses, "GBonusWeapon") != null) {
                int goldAdded = registerTier(equipment, magicInfusedWeapons,
                    createEquip, WeaponTier.GOLD, "ArsenalGold_");
                goldRegisteredWithSetBonus = goldAdded > 0;
                NanonaitorsArsenal.LOGGER.info(
                    "Registered {} Arsenal Gold weapons with Magic Infused Weapon",
                    goldAdded);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            registeredWithSetBonus = false;
            goldRegisteredWithSetBonus = false;
            NanonaitorsArsenal.LOGGER.warn(
                "Could not register Arsenal weapons with the RLCraft equipment sets; "
                    + "using the Silver numeric compatibility fallback", exception);
        }
    }

    /**
     * RLCraft-only Gold set check used by Scimitars. Requiring the detected
     * SetBonus data prevents an ordinary standalone Gold armor set from
     * granting RLCraft's Magic Infused Weapon behavior.
     */
    public static boolean isMagicInfusedGoldSetActive(EntityLivingBase wearer) {
        return Loader.isModLoaded("setbonus") && goldRegisteredWithSetBonus
            && wearsFullGoldArmor(wearer);
    }

    private static int registerTier(Collection<Object> equipment, Object weaponSet,
                                    Method createEquip, WeaponTier tier,
                                    String equipPrefix)
        throws ReflectiveOperationException {
        Field slotDataField = weaponSet.getClass().getField("slotData");
        Collection<?> slots = (Collection<?>) slotDataField.get(weaponSet);
        if (slots.isEmpty()) return 0;
        Object mainhandSlot = slots.iterator().next();
        Field involvedField = mainhandSlot.getClass().getField("involvedEquips");
        @SuppressWarnings("unchecked")
        Collection<Object> involvedEquips =
            (Collection<Object>) involvedField.get(mainhandSlot);

        int added = 0;
        for (ItemArsenalWeapon weapon : arsenalWeapons(tier)) {
            String path = weapon.getRegistryName().getResourcePath();
            String equipId = equipPrefix + path;
            Object equip = findById(equipment, equipId);
            if (equip == null) {
                equip = createEquip.invoke(null, equipId + ", "
                    + weapon.getRegistryName());
                if (equip != null) equipment.add(equip);
            }
            if (equip != null) {
                involvedEquips.add(equip);
                added++;
            }
        }
        return added;
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

    private static Collection<ItemArsenalWeapon> arsenalWeapons(WeaponTier tier) {
        Collection<ItemArsenalWeapon> result = new LinkedHashSet<>();
        for (net.minecraft.item.Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            if (item instanceof ItemArsenalWeapon
                && ((ItemArsenalWeapon) item).getTier() == tier) {
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

    private static boolean wearsFullGoldArmor(EntityLivingBase wearer) {
        return isGoldArmor(wearer.getItemStackFromSlot(EntityEquipmentSlot.HEAD))
            && isGoldArmor(wearer.getItemStackFromSlot(EntityEquipmentSlot.CHEST))
            && isGoldArmor(wearer.getItemStackFromSlot(EntityEquipmentSlot.LEGS))
            && isGoldArmor(wearer.getItemStackFromSlot(EntityEquipmentSlot.FEET));
    }

    private static boolean isGoldArmor(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.ItemArmor
            && ((net.minecraft.item.ItemArmor) stack.getItem()).getArmorMaterial()
                == net.minecraft.item.ItemArmor.ArmorMaterial.GOLD;
    }

    private static boolean hasRegistryName(ItemStack stack, String expected) {
        ResourceLocation name = stack.isEmpty() ? null : stack.getItem().getRegistryName();
        return name != null && expected.equals(name.toString());
    }
}
