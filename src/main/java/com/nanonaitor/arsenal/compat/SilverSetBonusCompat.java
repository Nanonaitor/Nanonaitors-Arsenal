package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.WeaponTier;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Integrates Arsenal's Gold and Silver weapons with RLCraft's SetBonus rules. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class SilverSetBonusCompat {
    private static final UUID SILVER_ATTACK_SPEED_UUID = UUID.fromString(
        "563b78c5-187b-42ec-8698-28d8e906d70a");
    private static final UUID GOLD_ATTACK_DAMAGE_UUID = UUID.fromString(
        "91a8e8e7-d6e5-4df3-a121-bf5bb94a6438");
    private static final double SILVER_ATTACK_SPEED_BONUS = 0.50D;
    private static final double GOLD_ATTACK_DAMAGE_BONUS = 0.50D;
    /** RLCraft's Defiled Lands golden scale set doubles gold weapon damage. */
    private static final double SCALE_GOLD_ATTACK_DAMAGE_BONUS = 1.00D;
    private static final String SILVER_EQUIP_PREFIX = "ArsenalSilver_";
    private static final String GOLD_EQUIP_PREFIX = "ArsenalGold_";

    private static boolean silverBonusDetected;
    private static boolean goldBonusDetected;
    private static boolean scaleGoldBonusDetected;
    private static boolean silverNativeRegistration;
    private static boolean goldNativeRegistration;
    private static Object silverArmorSet;
    private static Object goldArmorSet;
    private static Object scaleGoldArmorSet;

    private SilverSetBonusCompat() {}

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
            // Golden scale armor (Defiled Lands) pairs with the same gold weapon set.
            scaleGoldArmorSet = findById(sets, "SGSet");
            scaleGoldBonusDetected = scaleGoldArmorSet != null && goldWeaponSet != null
                && findById(bonuses, "SGBonusWeapon") != null;
            silverNativeRegistration = containsEquipPrefix(
                silverWeaponSet, SILVER_EQUIP_PREFIX);
            goldNativeRegistration = containsEquipPrefix(
                goldWeaponSet, GOLD_EQUIP_PREFIX);

            NanonaitorsArsenal.LOGGER.info(
                "RLCraft SetBonus integration: Magic Infused Weapon native={}, "
                    + "Quicksilver Hands native={}, golden scale set={}",
                goldNativeRegistration, silverNativeRegistration, scaleGoldBonusDetected);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            silverBonusDetected = false;
            goldBonusDetected = false;
            scaleGoldBonusDetected = false;
            silverNativeRegistration = false;
            goldNativeRegistration = false;
            silverArmorSet = null;
            goldArmorSet = null;
            scaleGoldArmorSet = null;
            NanonaitorsArsenal.LOGGER.warn(
                "Could not detect RLCraft's configured Gold/Silver equipment bonuses",
                exception);
        }
    }

    /** A full gold armor set or a full golden scale set: either unlocks Arsenal's gold perks. */
    public static boolean isMagicInfusedGoldSetActive(EntityLivingBase wearer) {
        return Loader.isModLoaded("setbonus")
            && (isGoldArmorSetActive(wearer) || isScaleGoldSetActive(wearer));
    }

    private static boolean isGoldArmorSetActive(EntityLivingBase wearer) {
        return goldBonusDetected && isSetComplete(goldArmorSet, wearer);
    }

    private static boolean isScaleGoldSetActive(EntityLivingBase wearer) {
        return scaleGoldBonusDetected && isSetComplete(scaleGoldArmorSet, wearer);
    }

    public static boolean isQuicksilverSetActive(EntityLivingBase wearer) {
        return Loader.isModLoaded("setbonus") && silverBonusDetected
            && isSetComplete(silverArmorSet, wearer);
    }

    public static boolean hasMatchingMetalSet(EntityLivingBase wearer, WeaponTier tier) {
        return tier == WeaponTier.GOLD && isMagicInfusedGoldSetActive(wearer)
            || tier == WeaponTier.SILVER && isQuicksilverSetActive(wearer);
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
        // Fallback only, for packs whose SetBonus config does not list Arsenal's
        // gold weapons itself. Matches the configured bonus of the worn set.
        boolean goldFallback = !goldNativeRegistration && heldTier == WeaponTier.GOLD;
        double goldBonus = goldFallback && isScaleGoldSetActive(player) ? SCALE_GOLD_ATTACK_DAMAGE_BONUS
            : goldFallback && isGoldArmorSetActive(player) ? GOLD_ATTACK_DAMAGE_BONUS : 0.0D;
        updateModifier(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE),
            GOLD_ATTACK_DAMAGE_UUID, "Magic Infused Weapon (Arsenal)",
            goldBonus, goldBonus > 0.0D);
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
        if (current != null && (!active || Math.abs(current.getAmount() - amount) > 1.0E-6D)) {
            attribute.removeModifier(current);
            current = null;
        }
        if (active && current == null) {
            attribute.applyModifier(new AttributeModifier(id, name, amount, 1)
                .setSaved(false));
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
