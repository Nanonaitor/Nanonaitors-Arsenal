package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Keeps Arsenal's crossed scimitar guard after Mo' Bends replaces vanilla arm poses. */
@Mod.EventBusSubscriber(modid=NanonaitorsArsenal.MOD_ID,value=Side.CLIENT)
public final class MoBendsScimitarCompat {
    private static boolean unavailable;
    private MoBendsScimitarCompat() {}

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void afterMoBendsPose(RenderPlayerEvent.Pre event) {
        EntityPlayer player=event.getEntityPlayer();
        if(unavailable || !Loader.isModLoaded("mobends")
            || !ScimitarShieldCompat.isGuarding(player))return;
        try {
            ModelPlayer model=event.getRenderer().getMainModel();
            poseRightArm(model.bipedRightArm);
            poseLeftArm(model.bipedLeftArm);
            poseItems(player);
        } catch(ReflectiveOperationException | LinkageError incompatibleVersion) {
            // Mo' Bends is optional. A different API must never break rendering.
            unavailable=true;
        }
    }

    // Runs every render frame while guarding: resolve Mo' Bends members once.
    // Rendering is single-threaded, so plain maps are enough.
    private static final java.util.Map<String, Method> METHODS = new java.util.HashMap<>();
    private static final java.util.Map<String, Field> FIELDS = new java.util.HashMap<>();
    private static Field databaseInstance;
    private static Method databaseGet;

    private static Method method(Class<?> owner, String name, Class<?>... params)
        throws NoSuchMethodException {
        String key = owner.getName() + '#' + name;
        Method found = METHODS.get(key);
        if (found == null) {
            found = owner.getMethod(name, params);
            METHODS.put(key, found);
        }
        return found;
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        String key = owner.getName() + '#' + name;
        Field found = FIELDS.get(key);
        if (found == null) {
            found = owner.getField(name);
            FIELDS.put(key, found);
        }
        return found;
    }

    private static void turn(Object rotation, String operation, float degrees)
        throws ReflectiveOperationException {
        method(rotation.getClass(), operation, float.class).invoke(rotation, degrees);
    }

    private static Object rotation(ModelRenderer arm) throws ReflectiveOperationException {
        return method(arm.getClass(), "getRotation").invoke(arm);
    }

    private static void poseRightArm(ModelRenderer arm) throws ReflectiveOperationException {
        Object rotation=rotation(arm);
        turn(rotation, "orientInstantX", -82.0F);
        turn(rotation, "rotateInstantZ", 35.0F);
    }

    private static void poseLeftArm(ModelRenderer arm) throws ReflectiveOperationException {
        Object rotation=rotation(arm);
        // Mo' Bends mirrors its left arm by reversing the operation order.
        // Using the right-arm order here makes the offhand blade visibly diverge.
        turn(rotation, "orientInstantZ", -35.0F);
        turn(rotation, "rotateInstantX", -82.0F);
    }

    private static void poseItems(EntityPlayer player) throws ReflectiveOperationException {
        if (databaseGet == null) {
            Class<?> databaseClass=Class.forName("goblinbob.mobends.core.data.EntityDatabase");
            databaseInstance=databaseClass.getField("instance");
            databaseGet=databaseClass.getMethod("get",EntityLivingBase.class);
        }
        Object data=databaseGet.invoke(databaseInstance.get(null),player);
        if(data==null)return;
        poseItem(data,"renderRightItemRotation",-28.0F);
        poseItem(data,"renderLeftItemRotation",28.0F);
    }

    private static void poseItem(Object data,String fieldName,float z)
        throws ReflectiveOperationException {
        turn(field(data.getClass(), fieldName).get(data), "orientInstantZ", z);
    }
}
