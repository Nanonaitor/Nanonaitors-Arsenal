package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Optional Reskillable integration based on equivalent Spartan weapons. */
public final class ReskillableCompat {
    private static final Logger LOGGER = LogManager.getLogger("Arsenal/Reskillable");
    private static boolean useCheckResolved;
    private static Method canPlayerUseItem;

    private ReskillableCompat() {}

    public static void registerWeaponRequirements() {
        if (!Loader.isModLoaded("reskillable")) return;

        try {
            Class<?> holderClass = Class.forName(
                "codersafterdark.reskillable.api.data.RequirementHolder");
            Class<?> handlerClass = Class.forName(
                "codersafterdark.reskillable.base.LevelLockHandler");
            Method fromString = holderClass.getMethod("fromString", String.class);
            Method isRealLock = holderClass.getMethod("isRealLock");
            Method getSkillLock = handlerClass.getMethod("getSkillLock", ItemStack.class);
            Method addLock = handlerClass.getMethod("addLock", ItemStack.class,
                holderClass);

            int registered = 0;
            for (WeaponTier tier : WeaponTier.values()) {
                if (!hasRequirement(tier)
                    || !ArsenalCompatManager.isTierAvailable(tier)) continue;

                Object holder = copyReferenceLock(tier, getSkillLock, isRealLock);
                if (holder == null) {
                    holder = fromString.invoke(null, fallbackRequirements(tier));
                }
                registered += registerFamily(ModContent.MORNING_STARS, tier,
                    holder, addLock);
                registered += registerFamily(ModContent.SCIMITARS, tier,
                    holder, addLock);
                registered += registerFamily(ModContent.CLAWS, tier,
                    holder, addLock);
                registered += registerFamily(ModContent.FLAILS, tier,
                    holder, addLock);
                registered += registerFamily(ModContent.BATTERING_RAMS, tier,
                    holder, addLock);
                registered += registerFamily(ModContent.BALLS_AND_CHAINS, tier,
                    holder, addLock);
            }
            LOGGER.info("Registered Reskillable requirements for {} Arsenal weapons.",
                registered);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("Could not register optional Reskillable requirements.",
                exception);
        }
    }

    /** Enforces locks for Arsenal attacks that bypass vanilla interaction events. */
    public static boolean canUse(EntityPlayer player, ItemStack stack) {
        if (!Loader.isModLoaded("reskillable")) return true;
        resolveUseCheck();
        if (canPlayerUseItem == null) return true;
        try {
            return Boolean.TRUE.equals(canPlayerUseItem.invoke(null, player, stack));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return true;
        }
    }

    private static synchronized void resolveUseCheck() {
        if (useCheckResolved) return;
        useCheckResolved = true;
        try {
            Class<?> handlerClass = Class.forName(
                "codersafterdark.reskillable.base.LevelLockHandler");
            canPlayerUseItem = handlerClass.getMethod("canPlayerUseItem",
                EntityPlayer.class, ItemStack.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            canPlayerUseItem = null;
        }
    }

    private static Object copyReferenceLock(WeaponTier tier, Method getSkillLock,
                                            Method isRealLock)
        throws ReflectiveOperationException {
        Item reference = ForgeRegistries.ITEMS.getValue(
            new ResourceLocation(referenceWeapon(tier)));
        if (reference == null || reference == Items.AIR) return null;
        Object holder = getSkillLock.invoke(null, new ItemStack(reference));
        return holder != null && Boolean.TRUE.equals(isRealLock.invoke(holder))
            ? holder : null;
    }

    private static int registerFamily(
        Map<WeaponTier, ? extends ItemArsenalWeapon> family, WeaponTier tier,
        Object holder, Method addLock) throws ReflectiveOperationException {
        ItemArsenalWeapon weapon = family.get(tier);
        if (weapon == null) return 0;
        addLock.invoke(null, new ItemStack(weapon), holder);
        return 1;
    }

    private static boolean hasRequirement(WeaponTier tier) {
        return tier != WeaponTier.WOOD && tier != WeaponTier.STONE
            && tier != WeaponTier.LIVING && tier != WeaponTier.SENTIENT;
    }

    private static String referenceWeapon(WeaponTier tier) {
        switch (tier) {
            case UMBRIUM: return "spartandefiled:mace_umbrium";
            case DRAGONBONE: return "spartanfire:mace_dragonbone";
            case FLAMED_DRAGONBONE: return "spartanfire:mace_fire_dragonbone";
            case ICED_DRAGONBONE: return "spartanfire:mace_ice_dragonbone";
            case ELECTRIC_DRAGONBONE:
                return "spartanfire:mace_lightning_dragonbone";
            case DESERT_MYRMEX: return "spartanfire:mace_desert";
            case JUNGLE_MYRMEX: return "spartanfire:mace_jungle";
            case DESERT_VENOM: return "spartanfire:mace_desert_venom";
            case JUNGLE_VENOM: return "spartanfire:mace_jungle_venom";
            default: return "spartanweaponry:mace_" + tier.getId();
        }
    }

    private static String fallbackRequirements(WeaponTier tier) {
        switch (tier) {
            case GOLD:
                return "reskillable:attack|2,reskillable:magic|4";
            case SILVER:
                return "reskillable:attack|4,reskillable:magic|2";
            case BRONZE:
            case UMBRIUM:
                return "reskillable:attack|4";
            case IRON:
                return "reskillable:attack|8";
            case STEEL:
            case DESERT_MYRMEX:
            case JUNGLE_MYRMEX:
            case DESERT_VENOM:
            case JUNGLE_VENOM:
                return "reskillable:attack|12";
            case DIAMOND:
                return "reskillable:attack|16";
            case DRAGONBONE:
            case FLAMED_DRAGONBONE:
            case ICED_DRAGONBONE:
            case ELECTRIC_DRAGONBONE:
                return "reskillable:attack|24";
            default:
                return "reskillable:attack|1";
        }
    }
}
