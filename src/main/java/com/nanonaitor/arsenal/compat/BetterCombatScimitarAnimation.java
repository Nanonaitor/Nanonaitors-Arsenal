package com.nanonaitor.arsenal.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.Loader;

/** Drives RLCombat's independent first-person hand controllers for automated scimitar swings. */
public final class BetterCombatScimitarAnimation {
    private static Class<?> handlers;
    private static Field mainHand, offHand;
    private static Method checkStacks, resetMain, resetOff;
    private static boolean initialized, available;

    private BetterCombatScimitarAnimation() {}

    private static void init() {
        if(initialized)return;
        initialized=true;
        if(!Loader.isModLoaded("bettercombatmod"))return;
        try {
            handlers=Class.forName("bettercombat.mod.client.handler.EventHandlersClient");
            mainHand=handlers.getField("betterCombatMainhand");
            offHand=handlers.getField("betterCombatOffhand");
            checkStacks=handlers.getMethod("checkItemstacksChanged");
            resetMain=handlers.getMethod("resetMainhandAnimationCooldown",float.class);
            resetOff=handlers.getMethod("resetOffhandAnimationCooldown",float.class);
            available=true;
        } catch(ReflectiveOperationException ignored) { available=false; }
    }

    public static void play(EntityPlayerSP player,boolean offhand) {
        init();
        if(!available)return;
        try {
            checkStacks.invoke(null);
            float cooldown=(float)ScimitarAttackBridge.cooldown(player,offhand);
            (offhand?resetOff:resetMain).invoke(null,cooldown);
            Object hand=(offhand?offHand:mainHand).get(null);
            hand.getClass().getMethod("initiateAnimation").invoke(hand);
        } catch(ReflectiveOperationException ex) { available=false; }
    }

    /** RLCombat observes canceled mouse events, so remove its false main-hand sweep on offhand turns. */
    public static void cancelNativeMainhandSweep() {
        init();
        if(!available)return;
        try {
            Object hand=mainHand.get(null);
            set(hand,"isSwingInProgress",false);
            set(hand,"swingProgressInt",0);
            set(hand,"swingProgressFloat",0.0F);
            set(hand,"prevSwingProgressFloat",0.0F);
        } catch(ReflectiveOperationException ex) { available=false; }
    }

    private static void set(Object target,String name,Object value) throws ReflectiveOperationException {
        Field field=target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target,value);
    }
}
