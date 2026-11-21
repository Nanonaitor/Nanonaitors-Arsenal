package com.nanonaitor.arsenal.client.pose;

import com.nanonaitor.arsenal.client.AbilityVisualState;
import com.nanonaitor.arsenal.client.ClientControls;
import com.nanonaitor.arsenal.client.WeaponVisualState;
import com.nanonaitor.arsenal.combat.ParityRules;
import com.nanonaitor.arsenal.config.ArsenalClientConfig;
import com.nanonaitor.arsenal.item.ArsenalShieldItem;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Third-person arm poses for Arsenal weapons. Poses are written for a weapon
 * held by the right arm (HumanoidModel radians: negative X raises an arm
 * forward, negative Y turns the right arm inward) and mirrored when the weapon
 * arm is the left one. Applied after the model's own animation pass.
 */
public final class WeaponPoses {
    // Held-item reference point and axis in the right arm's local frame (px):
    // ItemInHandLayer's offset (1, 2, -10) plus each model's third-person display
    // translation, rotated by the layer's -90 X / 180 Y turn.
    /** Hip-held ram (display rotation 75 X): the log points forward along the arm's -Z. */
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

    /** Arm angles to apply; arms left null keep the model's own pose. */
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

        Pose mirrored() {
            Pose m = new Pose();
            m.right = left == null ? null : new float[] {left[0], -left[1], -left[2]};
            m.left = right == null ? null : new float[] {right[0], -right[1], -right[2]};
            m.body = body;
            m.bodyY = -bodyY;
            return m;
        }
    }

    /** Capture tool hooks for the local player; null/false in normal play. */
    public static java.util.function.Consumer<Input> debugOverride;
    public static boolean debugBallGuard;
    /** Capture tool only: a fixed time into the current motion (-1 = off). */
    public static float debugSegmentAge = -1.0F;

    public static boolean enabled() {
        return ArsenalClientConfig.get(ArsenalClientConfig.WEAPON_POSES);
    }

    private static boolean kind(ItemStack stack, WeaponKind kind) {
        return stack.getItem() instanceof ArsenalWeaponItem weapon && weapon.kind() == kind;
    }

    private static boolean bulwark(ItemStack stack) {
        return stack.getItem() instanceof ArsenalShieldItem shield && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR;
    }

    private static boolean local(Player player) { return player == Minecraft.getInstance().player; }

    /** A scimitar shield bash is playing: a swing that came straight out of the cross-guard. */
    public static boolean scimitarBash(Player player, float partial) {
        return kind(player.getMainHandItem(), WeaponKind.SCIMITAR) && kind(player.getOffhandItem(), WeaponKind.SCIMITAR)
            && (local(player) ? ClientControls.scimitarBashPhase(partial) >= 0.0F
                : AttackTiming.phase(player, partial) >= 0.0F && AttackTiming.fromGuard(player));
    }

    public static Input detect(Player player, float partial) {
        Input in = new Input();
        ItemStack main = player.getMainHandItem(), off = player.getOffhandItem();
        boolean mainIsLeft = player.getMainArm() == HumanoidArm.LEFT;
        in.weaponArmLeft = mainIsLeft;
        if (bulwark(main) || bulwark(off)) {
            boolean inMain = bulwark(main);
            ItemStack shield = inMain ? main : off;
            in.kind = Kind.BULWARK;
            in.twoHanded = (inMain ? off : main).isEmpty();
            in.active = in.twoHanded && player.isUsingItem() && player.getUseItem() == shield;
            in.weaponArmLeft = inMain ? mainIsLeft : !mainIsLeft;
        } else if (kind(main, WeaponKind.BATTERING_RAM)) {
            in.kind = Kind.RAM;
            in.twoHanded = off.isEmpty();
            in.active = in.twoHanded && (local(player) ? ClientControls.ramActive()
                : player.isUsingItem() && player.getUseItem() == main);
        } else if (kind(main, WeaponKind.BLADE_STAFF)) {
            in.kind = Kind.STAFF;
            in.twoHanded = off.isEmpty();
            in.active = AbilityVisualState.staffReflecting(player);
        } else if (kind(main, WeaponKind.SCIMITAR) && kind(off, WeaponKind.SCIMITAR)
            && (ParityRules.guarding(player) || scimitarBash(player, partial))) {
            // Paired scimitars held crossed as a guard; a bash slashes both outward.
            in.kind = Kind.SCIMITARS;
            in.active = true;
        } else if (kind(main, WeaponKind.CLAWS) && (kind(off, WeaponKind.LINKED_CLAWS) || kind(off, WeaponKind.CLAWS))) {
            // A pair of claws: a low ready stance, raking with whichever hand strikes.
            in.kind = Kind.CLAWS;
        } else if (kind(main, WeaponKind.BALL_AND_CHAIN)) {
            in.kind = Kind.BALL;
            in.twoHanded = off.isEmpty();
            in.spinAngle = WeaponVisualState.ballWindupAngle(player, partial);
            in.throwProgress = WeaponVisualState.ballThrowProgress(player, partial);
            in.active = WeaponVisualState.ballGuarding(player);
        } else if (kind(main, WeaponKind.FLAIL)) {
            in.kind = Kind.FLAIL;
            in.spinAngle = WeaponVisualState.flailAngle(player, partial);
        } else if (kind(main, WeaponKind.MORNING_STAR)) {
            in.kind = Kind.MORNING_STAR;
            in.active = local(player) ? ClientControls.morningStarCharging()
                : player.isUsingItem() && kind(player.getUseItem(), WeaponKind.MORNING_STAR);
            in.chargeTicks = com.nanonaitor.arsenal.combat.CombatEvents.morningStarChargeTicks(main, player);
        }
        if (in.kind == Kind.CLAWS) {
            in.attackPhase = AttackTiming.phase(player, partial, InteractionHand.MAIN_HAND);
            in.attackCount = AttackTiming.count(player, InteractionHand.MAIN_HAND);
            in.offAttackPhase = AttackTiming.phase(player, partial, InteractionHand.OFF_HAND);
            in.offAttackCount = AttackTiming.count(player, InteractionHand.OFF_HAND);
        } else if (in.kind == Kind.SCIMITARS && local(player)) {
            in.attackPhase = ClientControls.scimitarBashPhase(partial);
        } else if (in.kind != Kind.NONE) {
            in.attackPhase = AttackTiming.phase(player, partial);
            in.attackCount = AttackTiming.count(player);
        }
        if (debugOverride != null && local(player)) debugOverride.accept(in);
        return in;
    }

    /** Names the kind of motion, so a change of motion can be eased. */
    private static String segment(Input in) {
        return in.kind + "|" + in.twoHanded + "|" + in.active
            + (Double.isNaN(in.spinAngle) ? "" : "|spin")
            + (in.throwProgress < 0.0D ? "" : "|throw")
            + (in.attackPhase < 0.0F ? "" : "|attack" + in.attackCount)
            + (in.offAttackPhase < 0.0F ? "" : "|off" + in.offAttackCount)
            + (in.weaponArmLeft ? "|left" : "");
    }

    private static final class State {
        String segment = "";
        float segmentStart;
        float cachedAge = Float.NaN;
        Pose cachedPose;
        Input cachedInput;
        // Easing from the arms last drawn to a new motion.
        float blendStart = -1000.0F;
        String blendSegment;
        float[] fromRight, fromLeft;
        float fromBody;
        float[] lastRight, lastLeft;
        float lastBody;
        /** Angles actually drawn this frame (after easing), and that frame's age. */
        float drawnAge = Float.NaN;
        float[] drawnRight, drawnLeft;
        float drawnBody;
        boolean drawnCrouch;
    }

    private static final Map<Player, State> STATES = new WeakHashMap<>();
    private static final float BLEND_TICKS = 3.0F;

    private static State state(Player player) { return STATES.computeIfAbsent(player, ignored -> new State()); }

    /** The pose for this player, or null to keep the model's own arms. */
    public static Pose compute(Player player, float ageInTicks, float partial) {
        if (!enabled()) return null;
        State st = state(player);
        if (st.cachedAge == ageInTicks && debugOverride == null) return st.cachedPose;
        Input in = detect(player, partial);
        String segment = segment(in);
        if (!segment.equals(st.segment)) { st.segment = segment; st.segmentStart = ageInTicks; }
        in.segmentAge = debugSegmentAge >= 0.0F && local(player) ? debugSegmentAge : ageInTicks - st.segmentStart;
        Pose pose = pose(in, ageInTicks);
        if (pose != null && in.weaponArmLeft) pose = pose.mirrored();
        st.cachedAge = ageInTicks;
        st.cachedPose = pose;
        st.cachedInput = in;
        return pose;
    }

    /** True when Arsenal poses this player's arms for its held weapon right now. */
    public static boolean posesArms(Player player, float partial) {
        return enabled() && compute(player, player.tickCount + partial, partial) != null;
    }

    /** Item property "posed": 1 while this staff is drawn crosswise in the two-handed grip. */
    public static boolean holdsStaffCrosswise(Player player) {
        if (!enabled() || !kind(player.getMainHandItem(), WeaponKind.BLADE_STAFF)) return false;
        Input in = detect(player, 0.0F);
        return in.kind == Kind.STAFF && in.twoHanded && !in.active;
    }

    /** Item property "hip": 1 carried one-handed at the hip, 2 two-handed with both hands on the handles. */
    public static float ramAtHip(Player player) {
        if (!enabled() || !kind(player.getMainHandItem(), WeaponKind.BATTERING_RAM)) return 0.0F;
        return player.getOffhandItem().isEmpty() ? 2.0F : 1.0F;
    }

    private static float ease(State st, float age) {
        if (!st.segment.equals(st.blendSegment)) {
            if (st.blendSegment != null && st.lastRight != null) {
                st.fromRight = st.lastRight.clone();
                st.fromLeft = st.lastLeft.clone();
                st.fromBody = st.lastBody;
                st.blendStart = age;
            }
            st.blendSegment = st.segment;
        }
        float t = (age - st.blendStart) / BLEND_TICKS;
        if (t < 0.0F || t >= 1.0F || st.fromRight == null) return 1.0F;
        return t * t * (3.0F - 2.0F * t);
    }

    private static float[] mix(float[] from, float[] to, float k) {
        return new float[] {from[0] + (to[0] - from[0]) * k, from[1] + (to[1] - from[1]) * k, from[2] + (to[2] - from[2]) * k};
    }

    private static float[] angles(ModelPart part) { return new float[] {part.xRot, part.yRot, part.zRot}; }

    private static void setAngles(ModelPart part, float[] a) { part.xRot = a[0]; part.yRot = a[1]; part.zRot = a[2]; }

    /**
     * Before the model's own animation: a full weapon pose owns the arms, so the
     * vanilla swing (which would twist the body under it) is removed; otherwise
     * ordinary Arsenal swings are stretched to the weapon's attack cooldown.
     */
    public static void prepareSwing(HumanoidModel<?> model, Player player, float partial) {
        if (!enabled()) return;
        if (compute(player, player.tickCount + partial, partial) != null) {
            model.attackTime = 0.0F;
            return;
        }
        if (!ArsenalClientConfig.get(ArsenalClientConfig.ATTACK_SPEED_SWINGS)) return;
        ItemStack held = player.getItemInHand(AttackTiming.hand(player));
        if (!(held.getItem() instanceof ArsenalWeaponItem weapon)) return;
        WeaponKind k = weapon.kind();
        if (k == WeaponKind.BALL_AND_CHAIN || k == WeaponKind.FLAIL || k == WeaponKind.BATTERING_RAM
            || k == WeaponKind.BLADE_STAFF) return;
        float phase = AttackTiming.phase(player, partial);
        if (phase >= 0.0F) model.attackTime = phase;
    }

    /** After the model's own animation: apply the weapon pose, easing between motions. */
    public static boolean applyTo(HumanoidModel<?> model, Player player, float partial) {
        if (!enabled()) return false;
        float age = player.tickCount + partial;
        Pose pose = compute(player, age, partial);
        State st = state(player);
        float k = ease(st, age);
        boolean changed = false;
        if (pose != null) { applyPose(model, pose); changed = true; }
        if (k < 1.0F) {
            float body = st.fromBody + (model.body.yRot - st.fromBody) * k;
            setTwist(model, body);
            setAngles(model.rightArm, mix(st.fromRight, angles(model.rightArm), k));
            setAngles(model.leftArm, mix(st.fromLeft, angles(model.leftArm), k));
            changed = true;
        }
        st.lastRight = angles(model.rightArm);
        st.lastLeft = angles(model.leftArm);
        st.lastBody = model.body.yRot;
        st.drawnAge = changed ? age : Float.NaN;
        st.drawnRight = st.lastRight;
        st.drawnLeft = st.lastLeft;
        st.drawnBody = st.lastBody;
        st.drawnCrouch = model.crouching;
        return changed;
    }

    private static void setTwist(HumanoidModel<?> model, float twist) {
        model.body.yRot = twist;
        model.rightArm.z = Mth.sin(twist) * 5.0F;
        model.rightArm.x = -Mth.cos(twist) * 5.0F;
        model.leftArm.z = -Mth.sin(twist) * 5.0F;
        model.leftArm.x = Mth.cos(twist) * 5.0F;
    }

    private static void applyPose(HumanoidModel<?> model, Pose pose) {
        float sneak = model.crouching ? 0.4F : 0.0F;
        if (pose.body) setTwist(model, pose.bodyY);
        float twist = pose.body ? pose.bodyY : 0.0F;
        if (pose.right != null) set(model.rightArm, pose.right, sneak, twist);
        if (pose.left != null) set(model.leftArm, pose.left, sneak, twist);
    }

    private static void set(ModelPart arm, float[] a, float sneak, float twist) {
        arm.xRot = a[0] + sneak;
        arm.yRot = a[1] + twist;
        arm.zRot = a[2];
    }

    // ---- Points on the drawn arms -------------------------------------------

    private static boolean firstPerson(Player player) {
        Minecraft mc = Minecraft.getInstance();
        return player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
    }

    /**
     * Where a hand is drawn this frame, as a world offset {x, y, z} in blocks from
     * the player's interpolated feet, or null when Arsenal is not posing that arm
     * (poses off, first person). Chain effects start here so the chain leaves the
     * hand that swings it.
     */
    public static double[] handOffset(Player player, float partial, boolean weaponHand) {
        boolean left = (player.getMainArm() == HumanoidArm.LEFT) == weaponHand;
        return armPoint(player, partial, left, 10.5D);
    }

    /**
     * A point down an arm (reach in model pixels from the shoulder: 10.5 is the
     * fist, more is along what it holds) as a world offset from the feet, or null.
     */
    public static double[] armPoint(Player player, float partial, boolean leftArm, double reach) {
        if (!enabled() || firstPerson(player)) return null;
        float age = player.tickCount + partial;
        State st = STATES.get(player);
        float[] arm; float twist; boolean crouch = player.isCrouching(); boolean modelAngles;
        if (st != null && st.drawnAge == age) {
            arm = leftArm ? st.drawnLeft : st.drawnRight; twist = st.drawnBody; crouch = st.drawnCrouch; modelAngles = true;
        } else {
            Pose pose = compute(player, age, partial);
            if (pose == null) return null;
            arm = leftArm ? pose.left : pose.right; twist = pose.body ? pose.bodyY : 0.0F; modelAngles = false;
            if (arm != null) arm = new float[] {arm[0] + (crouch ? 0.4F : 0.0F), arm[1] + twist, arm[2]};
        }
        if (arm == null) return null;
        return modelPoint(player, partial, arm, twist, crouch, leftArm, reach);
    }

    /** A point on an arm with these (model) angles, as a world offset from the feet. */
    private static double[] modelPoint(Player player, float partial, float[] arm, float twist, boolean crouch,
                                       boolean left, double reach) {
        float side = left ? 1.0F : -1.0F;
        double[] hand = GripMath.rotate(arm[0], arm[1], arm[2], new double[] {side, reach, 0.0D});
        double px = hand[0] + side * 5.0D * Math.cos(twist);
        double py = hand[1] + (crouch ? 5.2D : 2.0D);
        double pz = hand[2] - side * 5.0D * Math.sin(twist);
        // LivingEntityRenderer: turn by 180 - body yaw, flip X and Y, scale 0.9375
        // (players), translate -1.501; crouching players are drawn 0.125 lower.
        double scale = 0.9375D;
        double lx = -(px / 16.0D) * scale;
        double ly = -(py / 16.0D - 1.501D) * scale - (crouch ? 0.125D : 0.0D);
        double lz = (pz / 16.0D) * scale;
        double turn = Math.toRadians(180.0F - Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot));
        double c = Math.cos(turn), s = Math.sin(turn);
        return new double[] {lx * c + lz * s, ly, -lx * s + lz * c};
    }

    /**
     * Claw slash trails: the tip of the striking claw over the last part of the
     * rake, newest first, as world offsets from the feet; null when no rake plays.
     */
    public static double[][] clawTrail(Player player, float partial, boolean offHand) {
        if (!enabled() || firstPerson(player)) return null;
        float age = player.tickCount + partial;
        if (compute(player, age, partial) == null) return null;
        Input in = state(player).cachedInput;
        if (in == null || in.kind != Kind.CLAWS) return null;
        float a = offHand ? in.offAttackPhase : in.attackPhase;
        if (a < SWEEP_START || a > SWEEP_END + 0.15F) return null;
        int count = offHand ? in.offAttackCount : in.attackCount;
        boolean leftArm = in.weaponArmLeft != offHand;
        boolean crouch = player.isCrouching();
        double[][] points = new double[7][];
        for (int i = 0; i < points.length; i++) {
            float phase = Math.max(SWEEP_START, a - i * 0.04F);
            float[] r = clawRake(phase, count);
            float[] arm = leftArm ? new float[] {r[0], -r[1], -r[2]} : r;
            arm = new float[] {arm[0] + (crouch ? 0.4F : 0.0F), arm[1], arm[2]};
            points[i] = modelPoint(player, partial, arm, 0.0F, crouch, leftArm, 16.0D);
        }
        return points;
    }

    // ---- Motions ---------------------------------------------------------------

    /** Claw rake (right arm): from the side, sweeping in to the front of the body. */
    private static final float SWEEP_START = 0.15F;
    private static final float SWEEP_END = 0.55F;

    static float[] clawRake(float a, int count) {
        // Ready: low at the side, claws angled forward and out.
        float[] ready = {-0.35F, 0.05F, 0.12F};
        if (a < 0.0F) return ready;
        boolean lunge = count % 3 == 0;
        boolean high = count % 2 == 0;
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
        return (count % 3 == 0 ? 0.50F : 0.32F) * (float)Math.sin(a * Math.PI);
    }

    /** Battering Ram attack: a quick thrust forward in the first part of the swing, then back. */
    private static float ramLunge(float a) {
        if (a < 0.0F || !ArsenalClientConfig.get(ArsenalClientConfig.ATTACK_SPEED_SWINGS)) return 0.0F;
        return a < 0.25F ? 0.55F * a / 0.25F : 0.55F * Math.max(0.0F, 1.0F - (a - 0.25F) / 0.45F);
    }

    private static float[] mixArm(float[] from, float[] to, float t) {
        float k = t < 0.0F ? 0.0F : t > 1.0F ? 1.0F : t;
        return new float[] {from[0] + (to[0] - from[0]) * k, from[1] + (to[1] - from[1]) * k, from[2] + (to[2] - from[2]) * k};
    }

    private static float tremble(float age) {
        return (float)(Math.sin(age * 1.9D) * 0.035D + Math.sin(age * 3.3D + 1.0D) * 0.02D);
    }

    private static float lerp(float from, float to, float t) {
        float k = t < 0.0F ? 0.0F : t > 1.0F ? 1.0F : t;
        return from + (to - from) * k;
    }

    /** Scimitar guard forearm roll that leans both blades across into an X. */
    private static final float GUARD_X = -1.5F, GUARD_Y = -0.3F, GUARD_ROLL = -0.4F;
    /** Capture tool only: guard {x, y, roll} to try (null = the default). */
    public static float[] debugGuard;

    static Pose pose(Input in, float age) {
        float shake = tremble(age);
        switch (in.kind) {
            case RAM: {
                // Carried at the hip, the log pointing forward like a heavy gun (the
                // "hip" held model). Two-handed, the other hand grips the forward
                // handle. A normal attack is a short, quick lunge.
                float x = -0.30F - ramLunge(in.attackPhase) * 0.5F;
                if (in.active) {
                    // Charge: pull back for two ticks, drive forward, then bob with the stride.
                    float t = in.segmentAge;
                    x = t < 2.0F ? lerp(-0.30F, -0.12F, t / 2.0F)
                        : t < 5.0F ? lerp(-0.12F, -0.42F, (t - 2.0F) / 3.0F)
                        : -0.36F + (float)Math.sin(age * 0.9D) * 0.06F;
                    // Impact (the server swings the arm on a hit or wall): the arms kick back.
                    if (in.attackPhase >= 0.0F) x += 0.22F * (float)Math.sin(in.attackPhase * Math.PI);
                }
                // Two-handed, the log is turned a little across the body so the free
                // hand can reach its forward handle.
                float[] right = {x, in.twoHanded ? -0.45F : 0.0F, 0.05F};
                Pose pose = new Pose().right(right[0], right[1], right[2]);
                if (in.twoHanded) pose.left(GripMath.leftGrip(right, RAM_HANDLES, RAM_HIP_AXIS, -4.0D, 8.0D));
                return pose;
            }
            case STAFF: {
                if (in.active) return new Pose().right(-1.35F, -0.45F, 0.0F).left(-1.35F, 0.45F, 0.0F);
                float stroke = in.attackPhase < 0.0F ? 0.0F : (float)Math.sin(in.attackPhase * Math.PI);
                float side = (in.attackCount & 1) == 0 ? 1.0F : -1.0F;
                if (!in.twoHanded) {
                    // One-handed: gripped at the middle like a sword. The strike raises
                    // the staff and brings it down across the body.
                    if (in.attackPhase < 0.0F) return new Pose().right(-0.45F, -0.10F, -0.55F);
                    float a = in.attackPhase;
                    float x = a < 0.30F ? lerp(-0.45F, -2.25F, a / 0.30F)
                        : a < 0.70F ? lerp(-2.25F, -0.55F, (a - 0.30F) / 0.40F) : lerp(-0.55F, -0.45F, (a - 0.70F) / 0.30F);
                    float roll = a < 0.30F ? lerp(-0.55F, -0.95F, a / 0.30F)
                        : a < 0.70F ? lerp(-0.95F, 0.15F, (a - 0.30F) / 0.40F) : lerp(0.15F, -0.55F, (a - 0.70F) / 0.30F);
                    return new Pose().right(x, -0.10F - 0.40F * stroke, roll).bodyY(-0.25F * stroke);
                }
                // Held crosswise in both hands (the posed staff model) and paddled
                // side to side: rolling the right arm tips the staff's ends.
                float roll = side > 0.0F ? -0.30F - 0.60F * stroke : -0.30F + 0.65F * stroke;
                float[] right = {-0.95F + 0.25F * stroke, -0.30F, roll};
                return new Pose().right(right[0], right[1], right[2])
                    .left(GripMath.leftGrip(right, STAFF_CENTER, STAFF_AXIS, 5.0D, 13.0D))
                    .bodyY(-0.35F * stroke * side);
            }
            case BALL: {
                if (in.throwProgress >= 0.0D) {
                    float p = (float)in.throwProgress;
                    // Out: the throw. Back: the throwing hand yanks the chain in toward
                    // the body (the ball is being pulled), then settles to catch it.
                    float rightX = p < 0.15F ? lerp(-2.70F, -1.55F, p / 0.15F)
                        : p < 0.5F ? -1.55F
                        : p < 0.62F ? lerp(-1.55F, -0.15F, (p - 0.5F) / 0.12F)
                        : lerp(-0.15F, -0.90F, (p - 0.62F) / 0.38F);
                    float yank = p < 0.5F ? 0.0F : (float)Math.sin(Math.min(1.0F, (p - 0.5F) / 0.24F) * Math.PI);
                    Pose pose = new Pose().right(rightX, -0.10F, 0.0F);
                    if (yank > 0.0F) pose.bodyY(0.22F * yank);
                    // The free hand feeds the chain out, then hauls it back in.
                    if (in.twoHanded) pose.left(p < 0.5F ? -0.90F : lerp(-1.20F, -0.35F, (p - 0.5F) / 0.5F),
                        p < 0.5F ? 0.45F : 0.30F, 0.0F);
                    return pose;
                }
                if (!Double.isNaN(in.spinAngle)) {
                    float s = (float)Math.sin(in.spinAngle), c = (float)Math.cos(in.spinAngle);
                    if (!in.twoHanded) return new Pose().right(-1.20F - 0.50F * s + shake, -0.10F, 0.12F * c + 0.10F);
                    // The spinning arm follows the ball's vertical circle; the other
                    // hand holds the slack of the chain in front of the chest.
                    return new Pose().right(-1.35F - 0.75F * s, -0.15F, 0.15F * c).left(-0.95F + 0.08F * s, 0.50F, 0.0F);
                }
                if (in.active && in.twoHanded) return new Pose().right(-1.05F, -0.50F, 0.0F).left(-1.05F, 0.50F, 0.0F);
                return null;
            }
            case FLAIL: {
                if (Double.isNaN(in.spinAngle)) return null;
                float s = (float)Math.sin(in.spinAngle), c = (float)Math.cos(in.spinAngle);
                // Raised high, the fist whirling above the head (never in front of
                // the face) so the chain sweeps over it rather than through it.
                return new Pose().right(-3.0F + 0.15F * s, 0.0F, 0.12F + 0.25F * c);
            }
            case BULWARK:
                // Carried one-handed at the side, steady (no tremble: it read as a stutter).
                if (!in.twoHanded) return new Pose().right(-0.20F, 0.0F, 0.20F);
                // Braced: vanilla's shield-block angles on both arms so both hands hold it.
                if (in.active) return new Pose().right(-0.94F, -0.52F, 0.0F).left(-0.94F, 0.52F, 0.0F);
                // Carried at the side; the free hand reaches over to steady its near edge.
                return new Pose().right(-0.30F, -0.10F, 0.0F).left(-0.80F, 1.15F, 0.0F);
            case MORNING_STAR:
                if (in.active) {
                    // Raised gradually as the charge builds, highest exactly at full
                    // charge, then held there with a slight sway.
                    float k = Math.min(1.0F, in.segmentAge / Math.max(1.0F, in.chargeTicks));
                    float eased = k * k * (3.0F - 2.0F * k);
                    float sway = k >= 1.0F ? (float)Math.sin(age * 0.5D) * 0.05F : 0.0F;
                    return new Pose().right(lerp(-0.45F, -3.0F, eased) + sway, 0.0F, 0.05F + 0.12F * eased);
                }
                if (in.attackPhase >= 0.0F && ArsenalClientConfig.get(ArsenalClientConfig.ATTACK_SPEED_SWINGS)) {
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
                // The forearms roll so the blades lean across and cross in an X.
                float roll = debugGuard != null ? debugGuard[2] : GUARD_ROLL;
                if (in.attackPhase < 0.0F) {
                    float[] r = GripMath.roll(debugGuard != null ? new float[] {debugGuard[0], debugGuard[1], 0.0F}
                        : new float[] {GUARD_X, GUARD_Y, 0.0F}, roll);
                    return new Pose().right(r[0], r[1], r[2]).left(r[0], -r[1], -r[2]);
                }
                float a = in.attackPhase;
                float y = a < 0.20F ? lerp(GUARD_Y, -0.85F, a / 0.20F)
                    : a < 0.65F ? lerp(-0.85F, 0.80F, (a - 0.20F) / 0.45F) : lerp(0.80F, GUARD_Y, (a - 0.65F) / 0.35F);
                float x = a < 0.20F ? lerp(GUARD_X, -1.40F, a / 0.20F)
                    : a < 0.65F ? lerp(-1.40F, -1.05F, (a - 0.20F) / 0.45F) : lerp(-1.05F, GUARD_X, (a - 0.65F) / 0.35F);
                float z = a < 0.20F ? 0.0F
                    : a < 0.65F ? lerp(0.0F, 0.45F, (a - 0.20F) / 0.45F) : lerp(0.45F, 0.0F, (a - 0.65F) / 0.35F);
                // The guard's forearm roll unwinds through the outward slash and returns.
                float unroll = a < 0.20F ? 1.0F : a < 0.65F ? lerp(1.0F, 0.0F, (a - 0.20F) / 0.45F) : lerp(0.0F, 1.0F, (a - 0.65F) / 0.35F);
                float[] r = GripMath.roll(new float[] {x, y, z}, roll * unroll);
                return new Pose().right(r[0], r[1], r[2]).left(r[0], -r[1], -r[2]);
            }
            default:
                return null;
        }
    }

    private WeaponPoses() {}
}
