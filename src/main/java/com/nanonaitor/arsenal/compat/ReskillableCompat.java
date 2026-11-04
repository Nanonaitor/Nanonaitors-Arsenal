package com.nanonaitor.arsenal.compat;

import java.lang.reflect.Method;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

/** Respects externally configured Reskillable locks for custom weapon actions. */
public final class ReskillableCompat {
    private static boolean useCheckResolved;
    private static Method canPlayerUseItem;

    private ReskillableCompat() {}

    public static boolean canUse(EntityPlayer player, ItemStack stack) {
        if (com.nanonaitor.arsenal.combat.AbilityUseRules.cooling(player, stack)) return false;
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
}
