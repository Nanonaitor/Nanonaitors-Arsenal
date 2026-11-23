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
    /** Hip-held ram (display rotation 75 X): the log points forward along the arm's -Z, toward the head. */
    private static final double[] RAM_HIP_AXIS = {0.0D, 0.259D, -0.966D};
    /** Two-handed hip model: the line through both plank handles (the rear one sits in the right fist). */
    private static final double[] RAM_HANDLES = {1.0D, 10.44D, -7.64D};
    /** Posed blade staff (display rotation 0, 0, -45): the staff lies along the arm's local X. */
    private static final double[] STAFF_CENTER = {-1.0D, 10.0D, -2.0D};
    private static final double[] STAFF_AXIS = {1.0D, 0.0D, 0.0D};

    public enum Kind { NONE, RAM, STAFF, BALL, FLAIL, BULWARK, MORNING_STAR, SCIMITARS, CLAWS }

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
        /** Ticks since the current motion began (a charge starting, for example). */
        public float segmentAge;
        /** Morning Star: ticks to a full charge. */
        public float chargeTicks = 40.0F;
        /** Paired claws: the off-hand's own swing, which can play alongside the main hand's. */
        public float offAttackPhase = -1.0F;
        public int offAttackCount;
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
        } else if (main instanceof com.nanonaitor.arsenal.item.ItemScimitar
            && off instanceof com.nanonaitor.arsenal.item.ItemScimitar
            && (com.nanonaitor.arsenal.compat.ScimitarShieldCompat.isGuarding(player)
                || ScimitarBash.phase(player, partialTicks) >= 0.0F)) {
            // Paired scimitars held crossed as a guard; a bash slashes both outward.
            in.kind = Kind.SCIMITARS;
            in.active = true;
            in.weaponArmLeft = mainIsLeft;
        } else if (main instanceof com.nanonaitor.arsenal.item.ItemClaws
            && (off instanceof com.nanonaitor.arsenal.item.ItemLinkedClaw
                || off instanceof com.nanonaitor.arsenal.item.ItemClaws)) {
            // A pair of claws: a low ready stance, raking with whichever hand strikes.
            in.kind = Kind.CLAWS;
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
            in.chargeTicks = com.nanonaitor.arsenal.combat.ModernBackportCombat.chargeTicks(player.getHeldItemMainhand());
            in.weaponArmLeft = mainIsLeft;
        }
        if (in.kind == Kind.CLAWS) {
            in.attackPhase = AttackTiming.phase(player, partialTicks, EnumHand.MAIN_HAND);
            in.attackCount = AttackTiming.count(player, EnumHand.MAIN_HAND);
            in.offAttackPhase = AttackTiming.phase(player, partialTicks, EnumHand.OFF_HAND);
            in.offAttackCount = AttackTiming.count(player, EnumHand.OFF_HAND);
        } else if (in.kind == Kind.SCIMITARS) {
            in.attackPhase = ScimitarBash.phase(player, partialTicks);
            in.attackCount = AttackTiming.count(player);
        } else if (in.kind != Kind.NONE) {
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
    private static String cachedSegment = "";

    /** Names the kind of motion, so a change of motion can be eased (see {@link #applyTo}). */
    private static String segment(Input in) {
        return in.kind + "|" + in.twoHanded + "|" + in.active
            + (Double.isNaN(in.spinAngle) ? "" : "|spin")
            + (in.throwProgress < 0.0D ? "" : "|throw")
            + (in.attackPhase < 0.0F ? "" : "|attack" + in.attackCount)
            + (in.offAttackPhase < 0.0F ? "" : "|off" + in.offAttackCount)
            + (in.weaponArmLeft ? "|left" : "");
    }

    /** The pose for this player, or null to keep vanilla arms. */
    public static Pose compute(EntityPlayer player, float ageInTicks, float partialTicks) {
        if (!enabled()) return null;
        if (debugAngles != null && player == net.minecraft.client.Minecraft.getMinecraft().player) {
            float[] a = debugAngles;
            cachedSegment = "debug";
            return new Pose().right(a[0], a[1], a[2]).left(a[3], a[4], a[5]);
        }
        if (cachedPlayer.get() == player && cachedAge == ageInTicks && debugOverride == null) return cachedPose;
        Input in = detect(player, partialTicks);
        String segment = segment(in);
        Object[] started = SEGMENT_STARTS.get(player);
        if (started == null || !segment.equals(started[0])) {
            started = new Object[] {segment, ageInTicks};
            SEGMENT_STARTS.put(player, started);
        }
        in.segmentAge = ageInTicks - (Float) started[1];
        Pose pose = pose(in, ageInTicks);
        if (pose != null && in.weaponArmLeft) pose = pose.mirrored();
        cachedPlayer = new java.lang.ref.WeakReference<>(player);
        cachedAge = ageInTicks;
        cachedPose = pose;
        cachedSegment = segment;
        cachedInput = in;
        return pose;
    }

    private static final java.util.Map<EntityPlayer, Object[]> SEGMENT_STARTS = new java.util.WeakHashMap<>();
    private static Input cachedInput;

    /**
     * Claw slash trails: the tip of the striking claw over the last part of the
     * rake, newest first, as world offsets from the feet. Null when no rake is
     * playing (or poses are not drawn for this player).
     */
    public static double[][] clawTrail(EntityPlayer player, float partialTicks, boolean offHand) {
        if (!enabled() || !(PoseInstaller.anyInstalled() || MoBendsPoses.active())) return null;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (player == mc.getRenderViewEntity() && mc.gameSettings.thirdPersonView == 0) return null;
        float age = player.ticksExisted + partialTicks;
        if (compute(player, age, partialTicks) == null || cachedInput == null
            || cachedInput.kind != Kind.CLAWS) return null;
        Input in = cachedInput;
        float a = offHand ? in.offAttackPhase : in.attackPhase;
        if (a < SWEEP_START || a > SWEEP_END + 0.15F) return null;
        int count = offHand ? in.offAttackCount : in.attackCount;
        // Right-arm rake samples; the off-hand's rake is the mirror image.
        boolean leftArm = in.weaponArmLeft != offHand;
        double[][] points = new double[7][];
        for (int i = 0; i < points.length; i++) {
            float phase = Math.max(SWEEP_START, a - i * 0.04F);
            Pose pose = new Pose();
            pose.right = clawRake(phase, count);
            if (leftArm) pose = pose.mirrored();
            // Sampled past poses: always the vanilla arm math (a provider knows only the current frame).
            points[i] = vanillaArmPoint(player, partialTicks, pose, leftArm, 16.0D);
        }
        return points;
    }

    /**
     * Blade points (model px down the arm, forward of it): tip, then mid-blade.
     * In third person the scimitar's blade carries on along the arm past the fist.
     */
    private static final double[][] SCIMITAR_BLADE = {{23.0D, -1.5D}, {17.0D, -0.5D}};

    /**
     * Air-slash streaks behind an ordinary scimitar swing: the blade's tip and
     * middle at the last few sampled moments of vanilla's swing, as world
     * offsets from the feet (newest first), or null when no streak shows.
     */
    public static double[][][] scimitarTrail(EntityPlayer player, float partialTicks) {
        if (!enabled()) return null;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (player == mc.getRenderViewEntity() && mc.gameSettings.thirdPersonView == 0) return null;
        float age = player.ticksExisted + partialTicks;
        // A full weapon pose (guard, bash, claws...) owns the arms.
        if (compute(player, age, partialTicks) != null) return null;
        EnumHand hand = AttackTiming.hand(player);
        if (!(player.getHeldItem(hand).getItem() instanceof com.nanonaitor.arsenal.item.ItemScimitar)) return null;
        float a = ArsenalConfig.animations.attackSpeedSwings ? AttackTiming.phase(player, partialTicks)
            : player.isSwingInProgress ? player.getSwingProgress(partialTicks) : -1.0F;
        if (a < 0.04F || a > 0.75F) return null;
        EnumHandSide primary = player.getPrimaryHand();
        boolean left = (hand == EnumHand.MAIN_HAND) == (primary == EnumHandSide.LEFT);
        float pitch = (player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks)
            * (float) (Math.PI / 180.0D);
        double[][][] trails = new double[SCIMITAR_BLADE.length][7][];
        for (int i = 0; i < 7; i++) {
            Pose pose = vanillaSwingPose(Math.max(0.0F, a - i * 0.05F), pitch, left);
            for (int b = 0; b < SCIMITAR_BLADE.length; b++) {
                trails[b][i] = vanillaArmPoint(player, partialTicks, pose, left,
                    SCIMITAR_BLADE[b][0], SCIMITAR_BLADE[b][1]);
            }
        }
        return trails;
    }

    /** ModelBiped's swing for a held item at progress p (the swinging arm only). */
    private static Pose vanillaSwingPose(float p, float headPitch, boolean left) {
        float bodyY = (float) Math.sin(Math.sqrt(p) * Math.PI * 2.0D) * 0.2F;
        if (left) bodyY = -bodyY;
        float f = 1.0F - p;
        f = 1.0F - f * f * f * f;
        float lift = (float) Math.sin(f * Math.PI) * 1.2F
            + (float) Math.sin(p * Math.PI) * -(headPitch - 0.7F) * 0.75F;
        // Held-item arm rests at -pi/10; the swinging arm turns by 3x the body
        // twist in all (vanillaArmPoint adds the body's own share).
        float[] arm = {-0.31415927F - lift, bodyY * 2.0F, (float) Math.sin(p * Math.PI) * -0.4F};
        Pose pose = new Pose().bodyY(bodyY);
        if (left) pose.left = arm; else pose.right = arm;
        return pose;
    }

    // ---- Easing between motions -------------------------------------------
    // Poses change instantly when the motion changes (wind-up to throw, throw
    // to the next wind-up, guard to bash...). Each change eases from the arms
    // last drawn to the new motion over a few ticks so nothing snaps.
    private static final float BLEND_TICKS = 3.0F;
    private static final java.util.Map<EntityPlayer, Blend> BLENDS = new java.util.WeakHashMap<>();
    private static final java.util.Map<EntityPlayer, Blend> MO_BENDS_BLENDS = new java.util.WeakHashMap<>();

    private static final class Blend {
        String segment;
        float start = -1000.0F;
        float[] fromRight, fromLeft;
        float fromBody;
        float[] lastRight, lastLeft;
        float lastBody;
    }

    /** Starts a new ease when the motion changes; returns the ease progress 0..1 (1 = done). */
    private static float track(java.util.Map<EntityPlayer, Blend> map, EntityPlayer player, float age) {
        Blend blend = map.get(player);
        if (blend == null) {
            blend = new Blend();
            map.put(player, blend);
        }
        if (!cachedSegment.equals(blend.segment)) {
            if (blend.segment != null && blend.lastRight != null) {
                blend.fromRight = blend.lastRight.clone();
                blend.fromLeft = blend.lastLeft.clone();
                blend.fromBody = blend.lastBody;
                blend.start = age;
            }
            blend.segment = cachedSegment;
        }
        float t = (age - blend.start) / BLEND_TICKS;
        if (t < 0.0F || t >= 1.0F || blend.fromRight == null) return 1.0F;
        return t * t * (3.0F - 2.0F * t);
    }

    private static float[] mix(float[] from, float[] to, float k) {
        return new float[] {from[0] + (to[0] - from[0]) * k, from[1] + (to[1] - from[1]) * k,
            from[2] + (to[2] - from[2]) * k};
    }

    /**
     * The pose for the Mo' Bends path, eased between motions. Mo' Bends eases
     * its own arms back when Arsenal stops posing, so only posed motions ease here.
     */
    public static Pose computeForMoBends(EntityPlayer player, float ageInTicks, float partialTicks) {
        Pose pose = compute(player, ageInTicks, partialTicks);
        float k = track(MO_BENDS_BLENDS, player, ageInTicks);
        Blend blend = MO_BENDS_BLENDS.get(player);
        if (pose == null) {
            blend.lastRight = null;
            return null;
        }
        Pose out = pose;
        if (k < 1.0F && blend.fromRight != null) {
            out = new Pose();
            out.right = pose.right == null ? null : mix(blend.fromRight, pose.right, k);
            out.left = pose.left == null ? null : mix(blend.fromLeft, pose.left, k);
            out.body = pose.body || blend.fromBody != 0.0F;
            out.bodyY = blend.fromBody + ((pose.body ? pose.bodyY : 0.0F) - blend.fromBody) * k;
        }
        blend.lastRight = out.right == null ? new float[3] : out.right.clone();
        blend.lastLeft = out.left == null ? new float[3] : out.left.clone();
        blend.lastBody = out.body ? out.bodyY : 0.0F;
        return out;
    }

    /**
     * Where the weapon hand is drawn this frame, as a world offset {x, y, z} in
     * blocks from the player's interpolated feet position, or null when Arsenal
     * is not posing that arm (poses off, another mod's model, first person).
     * Chain effects start here so the chain leaves the hand that swings it.
     */
    public static double[] weaponHandOffset(EntityPlayer player, float partialTicks) {
        return handOffset(player, partialTicks, true);
    }

    /**
     * Like {@link #weaponHandOffset}, for the weapon hand or the other hand
     * (which, for example, holds the slack of a Ball and Chain's chain).
     */
    public static double[] handOffset(EntityPlayer player, float partialTicks, boolean weaponHand) {
        if (!enabled() || !(PoseInstaller.anyInstalled() || MoBendsPoses.active())) return null;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (player == mc.getRenderViewEntity() && mc.gameSettings.thirdPersonView == 0) return null;
        Pose pose = compute(player, player.ticksExisted + partialTicks, partialTicks);
        if (pose == null) return null;
        boolean left = (player.getPrimaryHand() == EnumHandSide.LEFT) == weaponHand;
        return armPoint(player, partialTicks, pose, left, 10.5D);
    }

    /**
     * A point along the weapon arm (reach in model pixels from the shoulder; past
     * 10.5 is beyond the fist, along whatever it holds), as a world offset from
     * the feet, or null when the arm is not posed (or in first person).
     */
    public static double[] weaponArmPoint(EntityPlayer player, float partialTicks, double reach) {
        if (!enabled() || !(PoseInstaller.anyInstalled() || MoBendsPoses.active())) return null;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (player == mc.getRenderViewEntity() && mc.gameSettings.thirdPersonView == 0) return null;
        Pose pose = compute(player, player.ticksExisted + partialTicks, partialTicks);
        if (pose == null) return null;
        return armPoint(player, partialTicks, pose, player.getPrimaryHand() == EnumHandSide.LEFT, reach);
    }

    /**
     * A point down the arm (reach in model pixels from the shoulder: 10.5 is the
     * fist, more is along a held blade) as a world offset from the feet.
     */
    /**
     * Hook for mods that draw their own player model (custom proportions): given
     * a point down an arm (reach in vanilla model pixels from the shoulder, 10.5 =
     * the fist), return where it really is as a world offset {x, y, z} from the
     * player's interpolated feet, or null to use Arsenal's vanilla-arm math.
     * Arsenal's chains, the flail handle, claw streaks and the guard ball follow it.
     * Set it from the other mod's client setup; it is read on the render thread.
     */
    public interface ArmPointProvider {
        double[] armPoint(EntityPlayer player, float partialTicks, boolean leftArm, double reach);
    }

    public static volatile ArmPointProvider armPointProvider;

    private static double[] armPoint(EntityPlayer player, float partialTicks, Pose pose, boolean left, double reach) {
        ArmPointProvider provider = armPointProvider;
        if (provider != null) {
            try {
                double[] point = provider.armPoint(player, partialTicks, left, reach);
                if (point != null && point.length == 3) return point;
            } catch (RuntimeException ignored) {
                // A broken provider must not break rendering; fall back to vanilla math.
            }
        }
        return vanillaArmPoint(player, partialTicks, pose, left, reach);
    }

    /** {@link #armPoint} on the vanilla player model, for any pose (also past or future ones). */
    private static double[] vanillaArmPoint(EntityPlayer player, float partialTicks, Pose pose, boolean left, double reach) {
        return vanillaArmPoint(player, partialTicks, pose, left, reach, 0.0D);
    }

    /** As above, plus forward model pixels in the arm's frame (where a held blade points). */
    private static double[] vanillaArmPoint(EntityPlayer player, float partialTicks, Pose pose, boolean left,
        double reach, double forward) {
        float[] arm = left ? pose.left : pose.right;
        if (arm == null) return null;
        float twist = pose.body ? pose.bodyY : 0.0F;
        boolean sneak = player.isSneaking();
        float side = left ? 1.0F : -1.0F;
        // Fist centre in the arm's frame, rotated like ModelRenderer (Z, Y, X),
        // then moved to the (possibly twisted) shoulder pivot. Model pixels.
        double[] hand = GripMath.rotate(arm[0] + (sneak ? 0.4F : 0.0F), arm[1] + twist, arm[2],
            new double[] {side, reach, -forward});
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

    /** Claw rake (right arm): from the side, sweeping in to the front of the body. */
    private static final float SWEEP_START = 0.15F;
    private static final float SWEEP_END = 0.55F;

    static float[] clawRake(float a, int count) {
        // Ready: low at the side, claws angled forward and out.
        float[] ready = {-0.35F, 0.05F, 0.12F};
        if (a < 0.0F) return ready;
        boolean lunge = count % 3 == 0;
        boolean high = count % 2 == 0;
        // Out to the side at shoulder height (or high, for the diagonal strikes),
        // sweeping in across the front of the body.
        float[] side = high ? new float[] {-2.25F, 0.95F, 0.15F} : new float[] {-1.50F, 1.20F, 0.15F};
        float[] front = high ? new float[] {lunge ? -1.05F : -0.90F, -0.50F, 0.0F}
            : new float[] {lunge ? -1.65F : -1.50F, -0.55F, 0.0F};
        if (a < SWEEP_START) return mixArm(ready, side, a / SWEEP_START);
        if (a < SWEEP_END) {
            float t = (a - SWEEP_START) / (SWEEP_END - SWEEP_START);
            return mixArm(side, front, t * t * (3.0F - 2.0F * t));
        }
        return mixArm(front, ready, (a - SWEEP_END) / (1.0F - SWEEP_END));
    }

    /** The torso turns into each strike; every third strike of a hand lunges further. */
    private static float clawTwist(float a, int count) {
        if (a < 0.0F) return 0.0F;
        return (count % 3 == 0 ? 0.50F : 0.32F) * (float) Math.sin(a * Math.PI);
    }

    /** Battering Ram attack: a quick thrust forward in the first part of the swing, then back. */
    private static float ramLunge(float a) {
        if (a < 0.0F || !ArsenalConfig.animations.attackSpeedSwings) return 0.0F;
        return a < 0.25F ? 0.55F * a / 0.25F : 0.55F * Math.max(0.0F, 1.0F - (a - 0.25F) / 0.45F);
    }

    private static float[] mixArm(float[] from, float[] to, float t) {
        float k = t < 0.0F ? 0.0F : t > 1.0F ? 1.0F : t;
        return new float[] {from[0] + (to[0] - from[0]) * k, from[1] + (to[1] - from[1]) * k,
            from[2] + (to[2] - from[2]) * k};
    }

    /**
     * Item property "hip" for the Battering Ram while Arsenal draws the arms: the
     * held model points the log forward from the hip. 1 = one-handed; 2 =
     * two-handed, rolled over so both plank handles face the hands.
     */
    public static final net.minecraft.item.IItemPropertyGetter RAM_AT_HIP = (stack, world, entity) ->
        entity instanceof EntityPlayer && entity.getHeldItemMainhand() == stack && enabled()
            && (PoseInstaller.anyInstalled() || MoBendsPoses.active())
            ? (entity.getHeldItemOffhand().isEmpty() ? 2.0F : 1.0F) : 0.0F;

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
            case RAM: {
                // Carried at the hip, the log pointing forward like a heavy gun (the
                // "hip" held model). Two-handed, the other hand grips the forward
                // handle underneath. A normal attack is a short, quick lunge.
                float x = -0.30F - ramLunge(in.attackPhase) * 0.5F;
                if (in.active) {
                    // Charge: pull back for two ticks, drive forward, then bob with the stride.
                    float t = in.segmentAge;
                    x = t < 2.0F ? lerp(-0.30F, -0.12F, t / 2.0F)
                        : t < 5.0F ? lerp(-0.12F, -0.42F, (t - 2.0F) / 3.0F)
                        : -0.36F + (float) Math.sin(age * 0.9D) * 0.06F;
                    // Impact (the server swings the arm on a hit): the arms kick back.
                    if (in.attackPhase >= 0.0F) x += 0.22F * (float) Math.sin(in.attackPhase * Math.PI);
                }
                float[] right = {x, 0.0F, 0.05F};
                Pose pose = new Pose().right(right[0], right[1], right[2]);
                if (in.twoHanded) {
                    // The left hand takes the front handle.
                    pose.left(GripMath.leftGrip(right, RAM_HANDLES, RAM_HIP_AXIS, 2.8D, 3.8D, false));
                }
                return pose;
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
                    // One-handed: gripped at the middle like a sword. The strike
                    // raises the staff and brings it down across the body, rolling
                    // the arm so the leading blade tips towards the target.
                    if (in.attackPhase < 0.0F) return new Pose().right(-0.45F, -0.10F, -0.55F);
                    float a = in.attackPhase;
                    float x = a < 0.30F ? lerp(-0.45F, -2.25F, a / 0.30F)
                        : a < 0.70F ? lerp(-2.25F, -0.55F, (a - 0.30F) / 0.40F) : lerp(-0.55F, -0.45F, (a - 0.70F) / 0.30F);
                    float roll = a < 0.30F ? lerp(-0.55F, -0.95F, a / 0.30F)
                        : a < 0.70F ? lerp(-0.95F, 0.15F, (a - 0.30F) / 0.40F) : lerp(0.15F, -0.55F, (a - 0.70F) / 0.30F);
                    float inward = -0.10F - 0.40F * stroke;
                    return new Pose().right(x, inward, roll).bodyY(-0.25F * stroke);
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
                    // Out: the throw. Back: the throwing hand yanks the chain in toward
                    // the body (the ball is being pulled), then settles to catch it.
                    float rightX = p < 0.15F ? lerp(-2.70F, -1.55F, p / 0.15F)
                        : p < 0.5F ? -1.55F
                        : p < 0.62F ? lerp(-1.55F, -0.15F, (p - 0.5F) / 0.12F)
                        : lerp(-0.15F, -0.90F, (p - 0.62F) / 0.38F);
                    float yank = p < 0.5F ? 0.0F : (float) Math.sin(Math.min(1.0F, (p - 0.5F) / 0.24F) * Math.PI);
                    Pose pose = new Pose().right(rightX, -0.10F, 0.0F);
                    if (yank > 0.0F) pose.bodyY(0.22F * yank);
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
                // Raised high and out to the side, whirling in circles above the head
                // so the chain sweeps over it rather than through it.
                return new Pose().right(-2.80F + 0.30F * s, 0.0F, 0.45F + 0.30F * c);
            }

            case BULWARK:
                if (!in.twoHanded) {
                    // Carried one-handed at the side, steady (no tremble: it read as a stutter).
                    return new Pose().right(-0.20F, 0.0F, 0.20F);
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
                    // Raised gradually as the charge builds, reaching the highest
                    // point exactly at full charge, then held there with a slight sway.
                    float k = Math.min(1.0F, in.segmentAge / Math.max(1.0F, in.chargeTicks));
                    float eased = k * k * (3.0F - 2.0F * k);
                    float sway = k >= 1.0F ? (float) Math.sin(age * 0.5D) * 0.05F : 0.0F;
                    return new Pose().right(lerp(-0.45F, -3.0F, eased) + sway, 0.0F, 0.05F + 0.12F * eased);
                }
                if (in.attackPhase >= 0.0F && ArsenalConfig.animations.attackSpeedSwings) {
                    float a = in.attackPhase;
                    float x = a < 0.35F ? lerp(-2.85F, -0.35F, a / 0.35F) : lerp(-0.35F, 0.0F, (a - 0.35F) / 0.65F);
                    return new Pose().right(x, 0.0F, 0.10F);
                }
                return null;

            case CLAWS: {
                // Each hand rakes on its own swing, so both can play at once.
                float[] right = clawRake(in.attackPhase, in.attackCount);
                float[] left = clawRake(in.offAttackPhase, in.offAttackCount);
                Pose pose = new Pose().right(right[0], right[1], right[2]).left(left[0], -left[1], -left[2]);
                float twist = clawTwist(in.attackPhase, in.attackCount) - clawTwist(in.offAttackPhase, in.offAttackCount);
                if (twist != 0.0F) pose.bodyY(twist);
                return pose;
            }

            case SCIMITARS: {
                // Guard: both blades crossed in front. Bash: a short wind-in, then
                // both arms slash outward and return to the guard.
                if (in.attackPhase < 0.0F) {
                    return new Pose().right(-1.25F, -0.55F, 0.0F).left(-1.25F, 0.55F, 0.0F);
                }
                float a = in.attackPhase;
                float y = a < 0.20F ? lerp(-0.55F, -0.85F, a / 0.20F)
                    : a < 0.65F ? lerp(-0.85F, 0.80F, (a - 0.20F) / 0.45F) : lerp(0.80F, -0.55F, (a - 0.65F) / 0.35F);
                float x = a < 0.20F ? lerp(-1.25F, -1.40F, a / 0.20F)
                    : a < 0.65F ? lerp(-1.40F, -1.05F, (a - 0.20F) / 0.45F) : lerp(-1.05F, -1.25F, (a - 0.65F) / 0.35F);
                float z = a < 0.20F ? 0.0F
                    : a < 0.65F ? lerp(0.0F, 0.45F, (a - 0.20F) / 0.45F) : lerp(0.45F, 0.0F, (a - 0.65F) / 0.35F);
                return new Pose().right(x, y, z).left(x, -y, -z);
            }

            default:
                return null;
        }
    }

    /**
     * Before vanilla's angle pass: stretch the swing of ordinary Arsenal weapons
     * (claws, scimitars, morning star) to the weapon's attack cooldown.
     */
    public static void prepareSwing(ModelBiped model, Entity entity, float ageInTicks) {
        if (!enabled() || !(entity instanceof EntityPlayer) || ageInTicks == 0.0F) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (compute(player, ageInTicks, ageInTicks - (int) ageInTicks) != null) {
            // A full weapon pose owns the arms: vanilla's swing would only twist
            // the body and shift the shoulders under it, a visible twitch.
            model.swingProgress = 0.0F;
            return;
        }
        if (ScimitarBash.ownsSwing(player)) {
            // The bash's swing finished as the double slash: never replay it as a hit.
            model.swingProgress = 0.0F;
            return;
        }
        if (!ArsenalConfig.animations.attackSpeedSwings) return;
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
        EntityPlayer player = (EntityPlayer) entity;
        Pose pose = compute(player, ageInTicks, ageInTicks - (int) ageInTicks);
        float k = track(BLENDS, player, ageInTicks);
        boolean changed = false;
        if (pose != null) {
            applyPose(model, pose);
            changed = true;
        }
        Blend blend = BLENDS.get(player);
        if (k < 1.0F) {
            // Ease from the arms last drawn to this motion (posed or vanilla).
            float[] right = mix(blend.fromRight, angles(model.bipedRightArm), k);
            float[] left = mix(blend.fromLeft, angles(model.bipedLeftArm), k);
            float body = blend.fromBody + (model.bipedBody.rotateAngleY - blend.fromBody) * k;
            model.bipedBody.rotateAngleY = body;
            model.bipedRightArm.rotationPointZ = (float) Math.sin(body) * 5.0F;
            model.bipedRightArm.rotationPointX = (float) -Math.cos(body) * 5.0F;
            model.bipedLeftArm.rotationPointZ = (float) -Math.sin(body) * 5.0F;
            model.bipedLeftArm.rotationPointX = (float) Math.cos(body) * 5.0F;
            setAngles(model.bipedRightArm, right);
            setAngles(model.bipedLeftArm, left);
            changed = true;
        }
        blend.lastRight = angles(model.bipedRightArm);
        blend.lastLeft = angles(model.bipedLeftArm);
        blend.lastBody = model.bipedBody.rotateAngleY;
        return changed;
    }

    private static float[] angles(ModelRenderer arm) {
        return new float[] {arm.rotateAngleX, arm.rotateAngleY, arm.rotateAngleZ};
    }

    private static void setAngles(ModelRenderer arm, float[] angles) {
        arm.rotateAngleX = angles[0];
        arm.rotateAngleY = angles[1];
        arm.rotateAngleZ = angles[2];
    }

    private static void applyPose(ModelBiped model, Pose pose) {
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
    }

    private static void set(ModelRenderer arm, float[] angles, float sneak, float twist) {
        arm.rotateAngleX = angles[0] + sneak;
        arm.rotateAngleY = angles[1] + twist;
        arm.rotateAngleZ = angles[2];
    }
}
