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

    private static Object rotation(ModelRenderer arm) throws ReflectiveOperationException {
        Method getRotation=arm.getClass().getMethod("getRotation");
        return getRotation.invoke(arm);
    }

    private static void poseRightArm(ModelRenderer arm) throws ReflectiveOperationException {
        Object rotation=rotation(arm);
        rotation.getClass().getMethod("orientInstantX",float.class).invoke(rotation,-82.0F);
        rotation.getClass().getMethod("rotateInstantZ",float.class).invoke(rotation,35.0F);
    }

    private static void poseLeftArm(ModelRenderer arm) throws ReflectiveOperationException {
        Object rotation=rotation(arm);
        // Mo' Bends mirrors its left arm by reversing the operation order.
        // Using the right-arm order here makes the offhand blade visibly diverge.
        rotation.getClass().getMethod("orientInstantZ",float.class).invoke(rotation,-35.0F);
        rotation.getClass().getMethod("rotateInstantX",float.class).invoke(rotation,-82.0F);
    }

    private static void poseItems(EntityPlayer player) throws ReflectiveOperationException {
        Class<?> databaseClass=Class.forName("goblinbob.mobends.core.data.EntityDatabase");
        Object database=databaseClass.getField("instance").get(null);
        Object data=databaseClass.getMethod("get",EntityLivingBase.class).invoke(database,player);
        if(data==null)return;
        poseItem(data,"renderRightItemRotation",-28.0F);
        poseItem(data,"renderLeftItemRotation",28.0F);
    }

    private static void poseItem(Object data,String fieldName,float z)
        throws ReflectiveOperationException {
        Field field=data.getClass().getField(fieldName);
        Object rotation=field.get(data);
        rotation.getClass().getMethod("orientInstantZ",float.class).invoke(rotation,z);
    }
}
