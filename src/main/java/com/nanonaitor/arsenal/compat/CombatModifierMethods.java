package com.nanonaitor.arsenal.compat;

import java.lang.reflect.Method;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/** RLCombat 2.0.x uses two parameters; newer versions add three selection flags. */
public final class CombatModifierMethods {
    public static Method[] resolve(Class<?> helpers) throws NoSuchMethodException {
        try {
            return pair(helpers,EntityLivingBase.class,ItemStack.class,boolean.class,boolean.class,boolean.class);
        } catch(NoSuchMethodException modernMissing) {
            return pair(helpers,EntityLivingBase.class,ItemStack.class);
        }
    }
    private static Method[] pair(Class<?> helpers,Class<?>... parameters) throws NoSuchMethodException {
        return new Method[]{helpers.getMethod("clearOldModifiers",parameters),helpers.getMethod("addNewModifiers",parameters)};
    }
    public static void invoke(Method method,EntityLivingBase entity,ItemStack stack) throws ReflectiveOperationException {
        if(method.getParameterCount()==2)method.invoke(null,entity,stack);
        else method.invoke(null,entity,stack,true,true,true);
    }
    private CombatModifierMethods() {}
}
