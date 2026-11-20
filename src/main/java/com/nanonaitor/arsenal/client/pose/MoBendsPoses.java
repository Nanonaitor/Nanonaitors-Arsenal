package com.nanonaitor.arsenal.client.pose;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Applies {@link WeaponPoses} through Mo' Bends' skeleton, after Mo' Bends has
 * animated the player for this frame, so the two never fight over the arms.
 * Mo' Bends keeps animating everything else (legs, body, its own attacks).
 *
 * Arms are oriented to exactly the vanilla angles (see {@link #applyArm}).
 * Any API mismatch disables this path and logs once.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class MoBendsPoses {
    private static final float DEG = (float) (180.0D / Math.PI);
    private static final Map<String, Method> METHODS = new HashMap<>();
    private static final Map<String, Field> FIELDS = new HashMap<>();
    private static Field databaseInstance;
    private static Method databaseGet;
    private static boolean unavailable;
    private static Boolean loaded;

    private MoBendsPoses() {}

    /** True when weapon poses are drawn through Mo' Bends (loaded, enabled and working). */
    public static boolean active() {
        if (unavailable || !ArsenalConfig.animations.moBendsPoses) return false;
        if (loaded == null) loaded = Loader.isModLoaded("mobends");
        return loaded;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterMoBendsPose(RenderPlayerEvent.Pre event) {
        if (!active()) return;
        EntityPlayer player = event.getEntityPlayer();
        float partial = event.getPartialRenderTick();
        WeaponPoses.Pose pose = WeaponPoses.compute(player, player.ticksExisted + partial, partial);
        if (pose == null) return;
        try {
            Object data = playerData(player);
            if (data == null) return;
            if (pose.rightArm() != null) {
                applyArm(data, "rightArm", "rightForeArm", pose.rightArm());
                clearItemTilt(data, "renderRightItemRotation");
            }
            if (pose.leftArm() != null) {
                applyArm(data, "leftArm", "leftForeArm", pose.leftArm());
                clearItemTilt(data, "renderLeftItemRotation");
            }
            if (pose.hasBodyTwist()) {
                call(partRotation(data, "body"), "rotateInstantY", pose.bodyTwist() * DEG);
            }
        } catch (ReflectiveOperationException | LinkageError | ClassCastException incompatible) {
            unavailable = true;
            NanonaitorsArsenal.LOGGER.warn("Arsenal weapon poses for Mo' Bends disabled: unexpected Mo' Bends API", incompatible);
        }
    }

    /**
     * Vanilla turns an arm by Z, then Y, then X (Rz * Ry * Rx). That whole
     * rotation is converted to one axis-angle and set in a single call, so
     * the result does not depend on how Mo' Bends chains separate rotations.
     * Single-axis rotations use the same signs in both (calibration captures).
     */
    private static void applyArm(Object data, String arm, String foreArm, float[] angles)
        throws ReflectiveOperationException {
        double[] q = vanillaArmQuaternion(angles[0], angles[1], angles[2]);
        double w = Math.max(-1.0D, Math.min(1.0D, q[0]));
        double half = Math.acos(w);
        double sin = Math.sin(half);
        Object rotation = partRotation(data, arm);
        if (sin < 1.0E-6D) {
            call(rotation, "orientInstantX", 0.0F);
        } else {
            method(rotation.getClass(), "orientInstant", float.class, float.class, float.class, float.class)
                .invoke(rotation, (float) (2.0D * half * DEG),
                    (float) (q[1] / sin), (float) (q[2] / sin), (float) (q[3] / sin));
        }
        straighten(data, foreArm);
    }

    /** Quaternion {w, x, y, z} of Rz(z) * Ry(y) * Rx(x). */
    static double[] vanillaArmQuaternion(double x, double y, double z) {
        double cx = Math.cos(x / 2), sx = Math.sin(x / 2);
        double cy = Math.cos(y / 2), sy = Math.sin(y / 2);
        double cz = Math.cos(z / 2), sz = Math.sin(z / 2);
        // qz * qy
        double w1 = cz * cy, x1 = -sz * sy, y1 = cz * sy, z1 = sz * cy;
        // (qz * qy) * qx
        return new double[] {
            w1 * cx - x1 * sx,
            w1 * sx + x1 * cx,
            y1 * cx + z1 * sx,
            z1 * cx - y1 * sx
        };
    }

    private static Object playerData(EntityPlayer player) throws ReflectiveOperationException {
        if (databaseGet == null) {
            Class<?> databaseClass = Class.forName("goblinbob.mobends.core.data.EntityDatabase");
            databaseInstance = databaseClass.getField("instance");
            databaseGet = databaseClass.getMethod("get", EntityLivingBase.class);
        }
        return databaseGet.invoke(databaseInstance.get(null), player);
    }

    private static Object partRotation(Object data, String part) throws ReflectiveOperationException {
        Object transform = field(data.getClass(), part).get(data);
        return method(transform.getClass(), "getRotation").invoke(transform);
    }

    private static void straighten(Object data, String foreArm) throws ReflectiveOperationException {
        call(partRotation(data, foreArm), "orientInstantX", 0.0F);
    }

    /** Mo' Bends tilts held items on its own; the weapon poses expect vanilla's grip. */
    private static void clearItemTilt(Object data, String itemRotation) throws ReflectiveOperationException {
        call(field(data.getClass(), itemRotation).get(data), "orientInstantX", 0.0F);
    }

    private static void call(Object rotation, String operation, float degrees)
        throws ReflectiveOperationException {
        method(rotation.getClass(), operation, float.class).invoke(rotation, degrees);
    }

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
}
