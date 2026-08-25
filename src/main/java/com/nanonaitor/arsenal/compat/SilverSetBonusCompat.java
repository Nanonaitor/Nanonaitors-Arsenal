package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.WeaponTier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Integrates Arsenal's Gold and Silver weapons with RLCraft's SetBonus rules. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class SilverSetBonusCompat {
    private static final UUID SILVER_ATTACK_SPEED_UUID = UUID.fromString(
        "563b78c5-187b-42ec-8698-28d8e906d70a");
    private static final UUID GOLD_ATTACK_DAMAGE_UUID = UUID.fromString(
        "91a8e8e7-d6e5-4df3-a121-bf5bb94a6438");
    private static final double SILVER_ATTACK_SPEED_BONUS = 0.50D;
    private static final double GOLD_ATTACK_DAMAGE_BONUS = 0.50D;
    private static final String SILVER_EQUIP_PREFIX = "ArsenalSilver_";
    private static final String GOLD_EQUIP_PREFIX = "ArsenalGold_";

    private static boolean silverBonusDetected;
    private static boolean goldBonusDetected;
    private static boolean silverNativeRegistration;
    private static boolean goldNativeRegistration;
    private static Object silverArmorSet;
    private static Object goldArmorSet;

    private SilverSetBonusCompat() {}

    /**
     * Extends SetBonus's in-memory configuration before SetBonus compiles it
     * during FMLServerStartingEvent. This makes Arsenal weapons genuine members
     * of GSetW/SSetW, so SetBonus supplies both mechanics and tooltips itself.
     * The user's on-disk SetBonus config is not modified.
     */
    public static void prepareRlcraftEquipmentSets() {
        if (!Loader.isModLoaded("setbonus")) return;
        try {
            Class<?> configClass = Class.forName(
                "com.fantasticsource.setbonus.config.SetBonusConfig");
            Object serverSettings = configClass.getField("serverSettings").get(null);
            if (serverSettings == null) return;

            Field equipmentField = serverSettings.getClass().getField("equipment");
            Field setsField = serverSettings.getClass().getField("sets");
            List<String> equipment = new ArrayList<>(Arrays.asList(
                (String[]) equipmentField.get(serverSettings)));
            List<String> sets = new ArrayList<>(Arrays.asList(
                (String[]) setsField.get(serverSettings)));

            int silverAdded = addTierToConfig(equipment, sets, "SSetW",
                WeaponTier.SILVER, SILVER_EQUIP_PREFIX);
            int goldAdded = addTierToConfig(equipment, sets, "GSetW",
                WeaponTier.GOLD, GOLD_EQUIP_PREFIX);

            equipmentField.set(serverSettings, equipment.toArray(new String[0]));
            setsField.set(serverSettings, sets.toArray(new String[0]));
            NanonaitorsArsenal.LOGGER.info(
                "Prepared native SetBonus entries for {} Silver and {} Gold Arsenal weapons",
                silverAdded, goldAdded);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            NanonaitorsArsenal.LOGGER.warn(
                "Could not prepare native RLCraft Gold/Silver weapon-set entries; "
                    + "the numeric compatibility fallback will be used", exception);
        }
    }

    /** Detects the fully parsed sets after SetBonus has built its server data. */
    public static void registerRlcraftEquipmentSets() {
        if (!Loader.isModLoaded("setbonus")) return;
        try {
            Class<?> dataClass = Class.forName("com.fantasticsource.setbonus.SetBonusData");
            Object serverData = dataClass.getField("SERVER_DATA").get(null);
            Collection<Object> sets = collectionField(dataClass, serverData, "sets");
            Collection<Object> bonuses = collectionField(dataClass, serverData, "bonuses");

            silverArmorSet = findById(sets, "SSet");
            goldArmorSet = findById(sets, "GSet");
            Object silverWeaponSet = findById(sets, "SSetW");
            Object goldWeaponSet = findById(sets, "GSetW");
            silverBonusDetected = silverArmorSet != null && silverWeaponSet != null
                && findById(bonuses, "SBonusW") != null;
            goldBonusDetected = goldArmorSet != null && goldWeaponSet != null
                && findById(bonuses, "GBonusWeapon") != null;
            silverNativeRegistration = containsEquipPrefix(
                silverWeaponSet, SILVER_EQUIP_PREFIX);
            goldNativeRegistration = containsEquipPrefix(
                goldWeaponSet, GOLD_EQUIP_PREFIX);

            NanonaitorsArsenal.LOGGER.info(
                "RLCraft SetBonus integration: Magic Infused Weapon native={}, "
                    + "Quicksilver Hands native={}",
                goldNativeRegistration, silverNativeRegistration);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            silverBonusDetected = false;
            goldBonusDetected = false;
            silverNativeRegistration = false;
            goldNativeRegistration = false;
            silverArmorSet = null;
            goldArmorSet = null;
            NanonaitorsArsenal.LOGGER.warn(
                "Could not detect RLCraft's configured Gold/Silver equipment bonuses",
                exception);
        }
    }

    public static boolean isMagicInfusedGoldSetActive(EntityLivingBase wearer) {
        return Loader.isModLoaded("setbonus") && goldBonusDetected
            && isSetComplete(goldArmorSet, wearer);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        WeaponTier heldTier = heldArsenalTier(player);
        updateModifier(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED),
            SILVER_ATTACK_SPEED_UUID, "Quicksilver Hands (Arsenal)",
            SILVER_ATTACK_SPEED_BONUS,
            !silverNativeRegistration && silverBonusDetected
                && heldTier == WeaponTier.SILVER
                && isSetComplete(silverArmorSet, player));
        updateModifier(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE),
            GOLD_ATTACK_DAMAGE_UUID, "Magic Infused Weapon (Arsenal)",
            GOLD_ATTACK_DAMAGE_BONUS,
            !goldNativeRegistration && goldBonusDetected
                && heldTier == WeaponTier.GOLD
                && isSetComplete(goldArmorSet, player));
    }

    private static int addTierToConfig(List<String> equipment, List<String> sets,
                                       String setId, WeaponTier tier,
                                       String equipPrefix) {
        int setIndex = findConfigEntry(sets, setId);
        if (setIndex < 0) return 0;
        String setLine = sets.get(setIndex);
        int added = 0;
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            if (!(item instanceof ItemArsenalWeapon)
                || ((ItemArsenalWeapon) item).getTier() != tier
                || item.getRegistryName() == null) continue;

            String equipId = equipPrefix + item.getRegistryName().getResourcePath();
            if (findConfigEntry(equipment, equipId) < 0) {
                equipment.add(equipId + ", " + item.getRegistryName());
            }
            if (!containsSetMember(setLine, equipId)) {
                setLine += " | " + equipId;
            }
            added++;
        }
        sets.set(setIndex, setLine);
        return added;
    }

    private static int findConfigEntry(List<String> entries, String id) {
        for (int i = 0; i < entries.size(); i++) {
            String value = entries.get(i);
            if (value != null && value.trim().startsWith(id + ",")) return i;
        }
        return -1;
    }

    private static boolean containsSetMember(String setLine, String equipId) {
        int equals = setLine.indexOf('=');
        if (equals < 0) return false;
        String[] members = setLine.substring(equals + 1).split("[|]");
        for (String member : members) {
            if (equipId.equals(member.trim())) return true;
        }
        return false;
    }

    private static boolean containsEquipPrefix(Object weaponSet, String prefix)
        throws ReflectiveOperationException {
        if (weaponSet == null) return false;
        Collection<?> slots = (Collection<?>) weaponSet.getClass()
            .getField("slotData").get(weaponSet);
        for (Object slot : slots) {
            Collection<?> equips = (Collection<?>) slot.getClass()
                .getField("involvedEquips").get(slot);
            for (Object equip : equips) {
                Object id = equip.getClass().getField("id").get(equip);
                if (id instanceof String && ((String) id).startsWith(prefix)) return true;
            }
        }
        return false;
    }

    private static void updateModifier(IAttributeInstance attribute, UUID id,
                                       String name, double amount, boolean active) {
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(id);
        if (active && current == null) {
            attribute.applyModifier(new AttributeModifier(id, name, amount, 1)
                .setSaved(false));
        } else if (!active && current != null) {
            attribute.removeModifier(current);
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

    private static WeaponTier heldArsenalTier(EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        return held.getItem() instanceof ItemArsenalWeapon
            ? ((ItemArsenalWeapon) held.getItem()).getTier() : null;
    }

    private static boolean isSetComplete(Object set, EntityLivingBase wearer) {
        if (set == null || !(wearer instanceof EntityPlayer)) return false;
        try {
            Method equipped = set.getClass().getMethod("getNumberEquipped",
                EntityPlayer.class);
            Method maximum = set.getClass().getMethod("getMaxNumber");
            int equippedCount = ((Number) equipped.invoke(set, wearer)).intValue();
            int requiredCount = ((Number) maximum.invoke(set)).intValue();
            return requiredCount > 0 && equippedCount >= requiredCount;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }
}
