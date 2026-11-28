package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.client.pose.WeaponPoses;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Optional Mo' Bends support. Mo' Bends draws players with its own animated skeleton,
 * which replaced every Arsenal arm pose (the crossed scimitar guard, bashes, the ram
 * carry...). Mo' Bends already knows how to take a pose from the vanilla model for other
 * animation mods (ModCompatManager.adoptPlayerPoseIf, called from BipedMutator's
 * adoptExternalArmPose). {@code MoBendsPoseMixin} adds Arsenal to that list: while Arsenal
 * poses a player's arms, Mo' Bends copies the vanilla model, which holds Arsenal's pose.
 * Everything is looked up by reflection, so Arsenal needs no Mo' Bends classes.
 */
public final class MoBendsCompat {
    private static Method adopt, currentEntity, currentModel;
    private static boolean failed;

    /** True while Arsenal poses this player's arms for its held weapon. */
    public static boolean posing(LivingEntity entity){
        return entity instanceof Player player && WeaponPoses.posesArms(player,Minecraft.getInstance().getFrameTime());
    }

    /** Called by MoBendsPoseMixin with Mo' Bends' BipedMutator while it draws a model. */
    public static void adopt(Object mutator){
        if(failed)return;
        try {
            if(adopt==null){
                var context=Class.forName("goblinbob.mobends.core.client.MoBendsRenderContext");
                currentEntity=context.getMethod("getCurrentEntity");
                currentModel=context.getMethod("getCurrentVanillaModel");
                for(Method m:Class.forName("goblinbob.mobends.compat.ModCompatManager").getMethods())
                    if(m.getName().equals("adoptPlayerPoseIf"))adopt=m;
                if(adopt==null)throw new NoSuchMethodException("ModCompatManager.adoptPlayerPoseIf");
            }
            adopt.invoke(null,currentEntity.invoke(null),mutator,currentModel.invoke(null),(Predicate<LivingEntity>)MoBendsCompat::posing);
        } catch(ReflectiveOperationException | LinkageError | ClassCastException error){
            // Another Mo' Bends version: keep rendering, only without Arsenal's poses.
            failed=true;
            com.mojang.logging.LogUtils.getLogger().warn("Arsenal cannot hand its weapon poses to this Mo' Bends version; Mo' Bends animations will replace them",error);
        }
    }
    private MoBendsCompat(){}
}
