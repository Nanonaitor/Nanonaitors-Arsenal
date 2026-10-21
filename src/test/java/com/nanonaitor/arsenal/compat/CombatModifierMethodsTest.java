package com.nanonaitor.arsenal.compat;

import java.lang.reflect.Method;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public final class CombatModifierMethodsTest {
    private static int calls,checks;
    private static void check(boolean value) { checks++;if(!value)throw new AssertionError("API check "+checks); }
    private static void near(double expected,double actual) {
        check(Math.abs(expected-actual)<0.00001D);
    }
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
        // One 4.5 main-hand hit plus RLCombat's 50%-efficient 2.25 off-hand
        // hit, while both target-specific enchantment bonuses remain unscaled.
        near(10.75D,ScimitarAttackBridge.combineBashDamage(4.5D,1.5F,2.25D,2.5F));
        check(ScimitarAttackBridge.resolveMainCooldownTicksField().getDeclaringClass()
            == EntityLivingBase.class);
        System.out.println("Passed "+checks+" legacy/modern RLCombat API checks");
    }
}
