package com.nanonaitor.arsenal.client.pose;

import com.nanonaitor.arsenal.client.BallAndChainAnimationHandler;
import com.nanonaitor.arsenal.client.BatteringRamInputHandler;
import com.nanonaitor.arsenal.client.FlailAnimationHandler;
import com.nanonaitor.arsenal.combat.DoubleBladedScimitarCombat;
import com.nanonaitor.arsenal.combat.ShieldCombat;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.item.ItemDoubleBladedScimitar;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Third-person arm poses for Arsenal weapons. Poses are written for a weapon
 * held by the right arm (vanilla ModelBiped radians: negative X raises an arm
 * forward, negative Y turns the right arm inward) and mirrored when the weapon
 * arm is the left one. Shared by the vanilla model and the Mo' Bends path.
 */
@SideOnly(Side.CLIENT)
public final class WeaponPoses {
    private WeaponPoses() {}

    // Held-item reference point and axis in the right arm's local frame (px):
    // LayerHeldItem's offset (1, 2, -10) plus each model's third-person display
    // translation, rotated by the layer's -90 X / 180 Y turn.
    /** Battering ram (display translation -2, 4, 0); its axis runs along the arm to the iron head. */
    private static final double[] RAM_CENTER = {1.0D, 10.0D, -6.0D};
    private static final double[] RAM_AXIS = {0.0D, 1.0D, 0.0D};
    /** Posed blade staff (display rotation 0, 0, -45): the staff lies along the arm's local X. */
    private static final double[] STAFF_CENTER = {-1.0D, 10.0D, -2.0D};
    private static final double[] STAFF_AXIS = {1.0D, 0.0D, 0.0D};

    public enum Kind { NONE, RAM, STAFF, BALL, FLAIL, BULWARK, MORNING_STAR }

    /** What the pose depends on; detected from the player, or forced by the capture tool. */
    public static final class Input {
        public Kind kind = Kind.NONE;
        /** Both hands free for a two-handed weapon. */
        public boolean twoHanded = true;
        /** Charging, guarding, reflecting or morning-star charging. */
        public boolean active;
        /** Attack-speed-timed swing progress 0..1, or -1. */
        public float attackPhase = -1.0F;
        public int attackCount;
        /** Flail orbit or ball-and-chain wind-up angle, or NaN. */
        public double spinAngle = Double.NaN;
        /** Ball-and-chain throw progress 0..1, or -1. */
        public double throwProgress = -1.0D;
        /** The weapon is held by the left arm, so the pose is mirrored. */
        public boolean weaponArmLeft;
    }

    /** Arm angles to apply; arms left null keep the vanilla pose. */
    public static final class Pose {
        float[] right, left;
        float bodyY;
        boolean body;

        Pose right(float x, float y, float z) { right = new float[] {x, y, z}; return this; }
        Pose left(float x, float y, float z) { left = new float[] {x, y, z}; return this; }
        Pose left(float[] angles) { left = angles.clone(); return this; }
        Pose bodyY(float y) { bodyY = y; body = true; return this; }

        public float[] rightArm() { return right; }
        public float[] leftArm() { return left; }
        public boolean hasBodyTwist() { return body; }
        public float bodyTwist() { return bodyY; }

        Pose mirrored() {
            Pose m = new Pose();
            m.right = left == null ? null : new float[] {left[0], -left[1], -left[2]};
            m.left = right == null ? null : new float[] {right[0], -right[1], -right[2]};
            m.body = body;
            m.bodyY = -bodyY;
            return m;
        }
    }

    /** Capture tool hook: adjusts the detected input for the local player. Null in normal play. */
    public static java.util.function.Consumer<Input> debugOverride;
    /** Capture tool hook: exact arm angles {rx, ry, rz, lx, ly, lz} for the local player. Null in normal play. */
    public static float[] debugAngles;

    public static boolean enabled() {
        return ArsenalConfig.animations.weaponPoses;
    }

    /** Item property "posed": 1 while this staff is drawn in Arsenal's two-handed grip. */
    public static final net.minecraft.item.IItemPropertyGetter STAFF_POSED = (stack, world, entity) ->
        entity instanceof EntityPlayer && entity.getHeldItemMainhand() == stack
            && holdsStaffCrosswise((EntityPlayer) entity) ? 1.0F : 0.0F;

    /**
     * True when this player's Blade Staff should use the crosswise held model:
     * a staff pose is drawn (vanilla model installed, or Mo' Bends) and not reflecting.
     */
    public static boolean holdsStaffCrosswise(EntityPlayer player) {
        if (!enabled() || !(player.getHeldItemMainhand().getItem() instanceof ItemDoubleBladedScimitar)) return false;
        return (PoseInstaller.anyInstalled() || MoBendsPoses.active()) && detect(player, 0.0F).kind == Kind.STAFF;
    }

    /** True when Arsenal poses this player's arms for its held weapon right now. */
    public static boolean posesArms(EntityPlayer player) {
        return enabled() && detect(player, 0.0F).kind != Kind.NONE;
    }

    public static Input detect(EntityPlayer player, float partialTicks) {
        Input in = new Input();
        Item main = player.getHeldItemMainhand().getItem();
        Item off = player.getHeldItemOffhand().getItem();
        boolean mainIsLeft = player.getPrimaryHand() == EnumHandSide.LEFT;
        if (main instanceof ItemSunWarBulwark || off instanceof ItemSunWarBulwark) {
            boolean inMain = main instanceof ItemSunWarBulwark;
            in.kind = Kind.BULWARK;
            in.twoHanded = ShieldCombat.isBulwarkReady(player);
            in.active = in.twoHanded && ShieldCombat.isGuarding(player, ItemSunWarBulwark.class);
            in.weaponArmLeft = inMain ? mainIsLeft : !mainIsLeft;
        } else if (main instanceof ItemBatteringRam) {
            in.kind = Kind.RAM;
            in.twoHanded = player.getHeldItemOffhand().isEmpty();
            in.active = in.twoHanded && BatteringRamInputHandler.isCharging(player);
            in.weaponArmLeft = mainIsLeft;
        } else if (main instanceof ItemDoubleBladedScimitar) {
            in.kind = Kind.STAFF;
            in.twoHanded = player.getHeldItemOffhand().isEmpty();
            in.active = DoubleBladedScimitarCombat.isReflecting(player);
            in.weaponArmLeft = mainIsLeft;
        } else if (main instanceof ItemBallAndChain) {
            in.kind = Kind.BALL;
            in.twoHanded = player.getHeldItemOffhand().isEmpty();
            in.spinAngle = BallAndChainAnimationHandler.windupAngle(player, partialTicks);
            in.throwProgress = BallAndChainAnimationHandler.releaseProgress(player, partialTicks);
            in.active = BallAndChainAnimationHandler.isGuarding(player);
            in.weaponArmLeft = mainIsLeft;
        } else if (main instanceof ItemFlail) {
            in.kind = Kind.FLAIL;
            in.spinAngle = FlailAnimationHandler.orbitAngle(player, partialTicks);
            in.weaponArmLeft = mainIsLeft;
        } else if (main instanceof ItemMorningStar) {
            in.kind = Kind.MORNING_STAR;
            in.active = com.nanonaitor.arsenal.data.TransientData.of(player)
                .getBoolean("ArsenalMorningCharging");
            in.weaponArmLeft = mainIsLeft;
        }
        if (in.kind != Kind.NONE) {
            in.attackPhase = AttackTiming.phase(player, partialTicks);
            in.attackCount = AttackTiming.count(player);
        }
        if (debugOverride != null && player == net.minecraft.client.Minecraft.getMinecraft().player) {
            debugOverride.accept(in);
        }
        return in;
    }

    // One player's model and both armor layers all ask for the same pose in a
    // frame (the body model twice); compute it once per player per frame.
    private static java.lang.ref.WeakReference<EntityPlayer> cachedPlayer = new java.lang.ref.WeakReference<>(null);
    private static float cachedAge = Float.NaN;
    private static Pose cachedPose;

    /** The pose for this player, or null to keep vanilla arms. */
    public static Pose compute(EntityPlayer player, float ageInTicks, float partialTicks) {
        if (!enabled()) return null;
        if (debugAngles != null && player == net.minecraft.client.Minecraft.getMinecraft().player) {
            float[] a = debugAngles;
            return new Pose().right(a[0], a[1], a[2]).left(a[3], a[4], a[5]);
        }
        if (cachedPlayer.get() == player && cachedAge == ageInTicks && debugOverride == null) return cachedPose;
        Input in = detect(player, partialTicks);
        Pose pose = pose(in, ageInTicks);
        if (pose != null && in.weaponArmLeft) pose = pose.mirrored();
        cachedPlayer = new java.lang.ref.WeakReference<>(player);
        cachedAge = ageInTicks;
        cachedPose = pose;
        return pose;
    }

    /**
     * Where the weapon hand is drawn this frame, as a world offset {x, y, z} in
     * blocks from the player's interpolated feet position, or null when Arsenal
     * is not posing that arm (poses off, another mod's model, first person).
     * Chain effects start here so the chain leaves the hand that swings it.
     */
    public static double[] weaponHandOffset(EntityPlayer player, float partialTicks) {
        if (!enabled() || !(PoseInstaller.anyInstalled() || MoBendsPoses.active())) return null;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (player == mc.getRenderViewEntity() && mc.gameSettings.thirdPersonView == 0) return null;
        Pose pose = compute(player, player.ticksExisted + partialTicks, partialTicks);
        if (pose == null) return null;
        boolean left = player.getPrimaryHand() == EnumHandSide.LEFT;
        float[] arm = left ? pose.left : pose.right;
        if (arm == null) return null;
        float twist = pose.body ? pose.bodyY : 0.0F;
        boolean sneak = player.isSneaking();
        float side = left ? 1.0F : -1.0F;
        // Fist centre in the arm's frame, rotated like ModelRenderer (Z, Y, X),
        // then moved to the (possibly twisted) shoulder pivot. Model pixels.
        double[] hand = GripMath.rotate(arm[0] + (sneak ? 0.4F : 0.0F), arm[1] + twist, arm[2],
            new double[] {side, 10.5D, 0.0D});
        double px = hand[0] + side * 5.0D * Math.cos(twist);
        double py = hand[1] + 2.0D;
        double pz = hand[2] - side * 5.0D * Math.sin(twist);
        // RenderLivingBase: translate(0, -1.501), scale 0.9375 (players), flip X and Y,
        // then turn by 180 - body yaw.
        double scale = 0.9375D;
        double lx = -(px / 16.0D) * scale;
        double ly = -(py / 16.0D - 1.501D) * scale - (sneak ? 0.125D : 0.0D);
        double lz = (pz / 16.0D) * scale;
        float bodyYaw = player.prevRenderYawOffset
            + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        double turn = Math.toRadians(180.0F - bodyYaw);
        double c = Math.cos(turn), s = Math.sin(turn);
        return new double[] {lx * c + lz * s, ly, -lx * s + lz * c};
    }

    private static float tremble(float age) {
        return (float) (Math.sin(age * 1.9D) * 0.035D + Math.sin(age * 3.3D + 1.0D) * 0.02D);
    }

    private static float lerp(float from, float to, float t) {
        float k = t < 0.0F ? 0.0F : t > 1.0F ? 1.0F : t;
        return from + (to - from) * k;
    }

    static Pose pose(Input in, float age) {
        float shake = tremble(age);
        switch (in.kind) {
            case RAM:
                if (!in.twoHanded) {
                    // Dragging a two-handed log with one arm.
                    return new Pose().right(-0.28F + shake, 0.15F, 0.22F + shake * 0.5F);
                }
            {
                // The log runs along the right arm; the left hand takes it just behind the right.
                float pump = in.active ? (float) Math.sin(age * 0.9D) * 0.07F : 0.0F;
                float[] right = in.active ? new float[] {-1.45F + pump, 0.0F, 0.0F}
                    : new float[] {-1.00F, 0.0F, 0.0F};
                return new Pose().right(right[0], right[1], right[2])
                    .left(GripMath.leftGrip(right, RAM_CENTER, RAM_AXIS, 3.0D, 10.0D, true));
            }

            case STAFF: {
                if (in.active) {
                    return new Pose().right(-1.35F, -0.45F, 0.0F).left(-1.35F, 0.45F, 0.0F);
                }
                float stroke = in.attackPhase < 0.0F ? 0.0F
                    : (float) Math.sin(in.attackPhase * Math.PI);
                float side = (in.attackCount & 1) == 0 ? 1.0F : -1.0F;
                // The staff lies crosswise in the fist (see the posed staff model),
                // so rolling the right arm tips its ends: positive roll lowers the
                // left end. Each attack paddles on alternate sides.
                // Rolling the far end down turns the staff's face from the light,
                // so the grip rests on a slight diagonal (right end low) and the
                // left-side stroke tips less far than the right.
                if (!in.twoHanded) {
                    return new Pose().right(-0.55F + shake, -0.15F, -0.35F - 0.30F * stroke * side)
                        .bodyY(-0.18F * stroke * side);
                }
                float roll = side > 0.0F ? -0.30F - 0.60F * stroke : -0.30F + 0.65F * stroke;
                float[] right = {-0.95F + 0.25F * stroke, -0.30F, roll};
                return new Pose().right(right[0], right[1], right[2])
                    .left(GripMath.leftGrip(right, STAFF_CENTER, STAFF_AXIS, 5.0D, 13.0D, false))
                    .bodyY(-0.35F * stroke * side);
            }

            case BALL: {
                if (in.throwProgress >= 0.0D) {
                    float p = (float) in.throwProgress;
                    float rightX = p < 0.15F ? lerp(-2.70F, -1.55F, p / 0.15F)
                        : p < 0.5F ? -1.55F : lerp(-1.55F, -0.90F, (p - 0.5F) / 0.5F);
                    Pose pose = new Pose().right(rightX, -0.10F, 0.0F);
                    if (in.twoHanded) {
                        // The free hand feeds the chain out, then hauls it back in.
                        pose.left(p < 0.5F ? -0.90F : lerp(-1.20F, -0.35F, (p - 0.5F) / 0.5F),
                            p < 0.5F ? 0.45F : 0.30F, 0.0F);
                    }
                    return pose;
                }
                if (!Double.isNaN(in.spinAngle)) {
                    float s = (float) Math.sin(in.spinAngle), c = (float) Math.cos(in.spinAngle);
                    if (!in.twoHanded) {
                        return new Pose().right(-1.20F - 0.50F * s + shake, -0.10F, 0.12F * c + 0.10F);
                    }
                    // The spinning arm follows the ball's vertical circle; the
                    // other hand steadies the chain in front of the chest.
                    return new Pose().right(-1.35F - 0.75F * s, -0.15F, 0.15F * c)
                        .left(-0.95F + 0.08F * s, 0.50F, 0.0F);
                }
                if (in.active && in.twoHanded) {
                    return new Pose().right(-1.05F, -0.50F, 0.0F).left(-1.05F, 0.50F, 0.0F);
                }
                return null;
            }

            case FLAIL: {
                if (Double.isNaN(in.spinAngle)) return null;
                float s = (float) Math.sin(in.spinAngle), c = (float) Math.cos(in.spinAngle);
                // Raised arm whirling the flail in tight circles overhead.
                return new Pose().right(-2.50F + 0.35F * s, 0.0F, 0.25F + 0.35F * c);
            }

            case BULWARK:
                if (!in.twoHanded) {
                    // Hauling the heavy shield one-handed.
                    return new Pose().right(-0.20F + shake, 0.0F, 0.20F + shake * 0.5F);
                }
                if (in.active) {
                    // Vanilla's shield-block angles, which the Bulwark's blocking
                    // model is built for, on both arms so both hands brace it.
                    return new Pose().right(-0.94F, -0.52F, 0.0F).left(-0.94F, 0.52F, 0.0F);
                }
                // Carried at the side; the free hand reaches over to steady its near edge.
                return new Pose().right(-0.30F, -0.10F, 0.0F).left(-0.80F, 1.15F, 0.0F);

            case MORNING_STAR:
                if (in.active) {
                    float sway = (float) Math.sin(age * 0.5D) * 0.05F;
                    return new Pose().right(-2.85F + sway, 0.0F, 0.15F);
                }
                if (in.attackPhase >= 0.0F && ArsenalConfig.animations.attackSpeedSwings) {
                    float a = in.attackPhase;
                    float x = a < 0.35F ? lerp(-2.85F, -0.35F, a / 0.35F) : lerp(-0.35F, 0.0F, (a - 0.35F) / 0.65F);
                    return new Pose().right(x, 0.0F, 0.10F);
                }
                return null;

            default:
                return null;
        }
    }

    /**
     * Before vanilla's angle pass: stretch the swing of ordinary Arsenal weapons
     * (claws, scimitars, morning star) to the weapon's attack cooldown.
     */
    public static void prepareSwing(ModelBiped model, Entity entity, float ageInTicks) {
        if (!enabled() || !ArsenalConfig.animations.attackSpeedSwings
            || !(entity instanceof EntityPlayer) || ageInTicks == 0.0F) return;
        EntityPlayer player = (EntityPlayer) entity;
        EnumHand hand = AttackTiming.hand(player);
        if (!(player.getHeldItem(hand).getItem() instanceof ItemArsenalWeapon)) return;
        Item item = player.getHeldItem(hand).getItem();
        if (item instanceof ItemBallAndChain || item instanceof ItemFlail
            || item instanceof ItemBatteringRam || item instanceof ItemDoubleBladedScimitar) return;
        float phase = AttackTiming.phase(player, ageInTicks - (int) ageInTicks);
        if (phase >= 0.0F) model.swingProgress = phase;
    }

    /**
     * After vanilla's angle pass: apply the weapon pose. Returns true when arms
     * changed, so the caller can copy them to overlay layers. A zero
     * ageInTicks is the first-person arm render, which is left alone.
     */
    public static boolean applyTo(ModelBiped model, Entity entity, float ageInTicks) {
        if (!(entity instanceof EntityPlayer) || ageInTicks == 0.0F) return false;
        Pose pose = compute((EntityPlayer) entity, ageInTicks, ageInTicks - (int) ageInTicks);
        if (pose == null) return false;
        float sneak = model.isSneak ? 0.4F : 0.0F;
        if (pose.body) {
            float twist = pose.bodyY;
            model.bipedBody.rotateAngleY = twist;
            model.bipedRightArm.rotationPointZ = (float) Math.sin(twist) * 5.0F;
            model.bipedRightArm.rotationPointX = (float) -Math.cos(twist) * 5.0F;
            model.bipedLeftArm.rotationPointZ = (float) -Math.sin(twist) * 5.0F;
            model.bipedLeftArm.rotationPointX = (float) Math.cos(twist) * 5.0F;
        }
        float twist = pose.body ? pose.bodyY : 0.0F;
        if (pose.right != null) set(model.bipedRightArm, pose.right, sneak, twist);
        if (pose.left != null) set(model.bipedLeftArm, pose.left, sneak, twist);
        return true;
    }

    private static void set(ModelRenderer arm, float[] angles, float sneak, float twist) {
        arm.rotateAngleX = angles[0] + sneak;
        arm.rotateAngleY = angles[1] + twist;
        arm.rotateAngleZ = angles[2];
    }
}
