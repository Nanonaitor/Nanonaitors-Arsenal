package com.nanonaitor.arsenal.compat;

import java.lang.reflect.Method;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public final class CombatModifierMethodsTest {
    private static int calls,checks;
    private static void check(boolean value) { checks++;if(!value)throw new AssertionError("API check "+checks); }
    public static class Legacy {
        public static void clearOldModifiers(EntityLivingBase p,ItemStack s) { calls++; }
        public static void addNewModifiers(EntityLivingBase p,ItemStack s) { calls++; }
    }
    public static class Modern {
        public static void clearOldModifiers(EntityLivingBase p,ItemStack s,boolean damage,boolean speed,boolean reach) {
            check(damage && speed && reach);calls++;
        }
        public static void addNewModifiers(EntityLivingBase p,ItemStack s,boolean damage,boolean speed,boolean reach) {
            check(damage && speed && reach);calls++;
        }
    }
    public static class Incomplete {
        public static void clearOldModifiers(EntityLivingBase p,ItemStack s,boolean d,boolean a,boolean r) {}
        public static void addNewModifiers(EntityLivingBase p,ItemStack s) {}
    }
    public static void main(String[] args) throws Exception {
        for(Class<?> api:new Class<?>[]{Legacy.class,Modern.class}) {
            Method[] methods=CombatModifierMethods.resolve(api);
            int arity=api==Legacy.class?2:5;
            check(methods[0].getParameterCount()==arity);check(methods[1].getParameterCount()==arity);
            int before=calls;
            CombatModifierMethods.invoke(methods[0],null,null);
            CombatModifierMethods.invoke(methods[1],null,null);
            check(calls==before+2);
        }
        boolean rejected=false;
        try {CombatModifierMethods.resolve(Incomplete.class);}catch(NoSuchMethodException expected){rejected=true;}
        check(rejected);
        System.out.println("Passed "+checks+" legacy/modern RLCombat API checks");
    }
}
