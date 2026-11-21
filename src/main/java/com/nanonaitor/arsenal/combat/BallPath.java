package com.nanonaitor.arsenal.combat;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared Ball and Chain throw geometry, used by the server (hits and crashes)
 * and the client (what is drawn), so both agree on where the ball goes.
 */
public final class BallPath {
    /** The swung and thrown head is drawn a size up from the original. */
    public static final double BALL_SIZE = 1.3D;

    /** Model scale of the drawn head for a throw of this charge (16 px = this many blocks). */
    public static float throwModelScale(int charge) {
        return (float)((0.48D + Math.max(1, Math.min(3, charge)) * 0.05D) * BALL_SIZE);
    }

    /** Wind-up head scale. */
    public static float windupModelScale() {
        return (float)(0.528D * BALL_SIZE);
    }

    /** Radius of the ball as drawn: the head is 10 px across. */
    public static double visualBallRadius(int charge) {
        return throwModelScale(charge) * 5.0D / 16.0D;
    }

    /** Damage hits use a radius 30% larger than the drawn ball. */
    public static double hitRadius(int charge) {
        return visualBallRadius(charge) * 1.3D;
    }

    /** Blocks stop the ball a little inside its drawn size, so near misses fly on. */
    public static double crashRadius(int charge) {
        return visualBallRadius(charge) * 0.8D;
    }

    /**
     * How far the ball's centre travels before its surface meets a solid block,
     * testing the ball's outline (centre, top, bottom and sides) at the crash radius.
     */
    public static double stopDistance(Level level, Vec3 start, Vec3 direction, double distance, double radius) {
        Vec3 normalized = direction.normalize();
        Vec3 side = new Vec3(-normalized.z, 0.0D, normalized.x);
        side = side.lengthSqr() > 0.0001D ? side.normalize().scale(radius) : new Vec3(radius, 0.0D, 0.0D);
        Vec3[] offsets = {Vec3.ZERO, new Vec3(0.0D, -radius, 0.0D), new Vec3(0.0D, radius, 0.0D), side, side.scale(-1.0D)};
        double closest = distance;
        for (Vec3 offset : offsets) closest = Math.min(closest, blockingDistance(level, start.add(offset), normalized, distance));
        // Stop with the ball's surface at the block, not its centre inside it.
        return closest < distance ? Math.max(0.0D, closest - radius) : distance;
    }

    /** Where the throw's centre line starts: the hand, at the player's middle. */
    public static Vec3 origin(net.minecraft.world.entity.player.Player player) {
        return player.position().add(0.0D, player.isCrouching() ? 1.05D : 1.25D, 0.0D);
    }

    /** True when a block cut the throw short. */
    public static boolean cutShort(double actual, double requested) {
        return actual + 0.05D < requested;
    }

    /** A throw cut short by a block keeps its speed: a shorter trip takes less time. */
    public static int throwDuration(int baseTicks, double actual, double requested) {
        if (requested <= 0.01D || !cutShort(actual, requested)) return baseTicks;
        return Math.max(4, (int)Math.round(baseTicks * actual / requested));
    }

    /** Distance out along the throw at this progress (0..1): out until 0.5, back after. */
    public static double travel(double distance, double progress) {
        return distance * Math.sin(Math.max(0.0D, Math.min(1.0D, progress)) * Math.PI);
    }

    /**
     * First meaningful obstruction. Grass, flowers and other replaceable or
     * non-colliding plants are passed through; re-clipping just beyond an ignored
     * shape keeps a real wall right behind it solid.
     */
    private static double blockingDistance(Level level, Vec3 start, Vec3 direction, double distance) {
        Vec3 end = start.add(direction.scale(distance));
        Vec3 cursor = start;
        for (int ignored = 0; ignored < 16 && cursor.distanceToSqr(end) > 0.0001D; ignored++) {
            HitResult hit = level.clip(new ClipContext(cursor, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity)null));
            if (!(hit instanceof BlockHitResult block) || hit.getType() == HitResult.Type.MISS) return distance;
            BlockState state = level.getBlockState(block.getBlockPos());
            if (!state.canBeReplaced() && !state.getCollisionShape(level, block.getBlockPos()).isEmpty())
                return start.distanceTo(block.getLocation());
            cursor = block.getLocation().add(direction.scale(0.05D));
        }
        return distance;
    }

    private BallPath() {}
}
