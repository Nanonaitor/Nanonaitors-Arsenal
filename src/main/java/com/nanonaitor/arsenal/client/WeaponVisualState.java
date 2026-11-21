package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.combat.ChainWeaponStats;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * What a player's chain weapon is doing right now, for the local player (from
 * its own input) and other players (from the synced item data), so the pose and
 * the drawn ball or flail always agree.
 */
public final class WeaponVisualState {
    /** Capture tool only: forces states for the local player. */
    public static double debugWindupAngle = Double.NaN, debugThrowProgress = -1.0D, debugFlailAngle = Double.NaN;
    public static int debugThrowCharge = 2;
    public static double debugThrowDistance = 5.0D;

    private static boolean local(Player player) { return player == Minecraft.getInstance().player; }

    private static boolean is(ItemStack stack, WeaponKind kind) {
        return stack.getItem() instanceof ArsenalWeaponItem weapon && weapon.kind() == kind;
    }

    private static boolean activeTag(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("ArsenalActive");
    }

    public static int ballSwingTicks(Player player) {
        return ChainWeaponStats.swingIntervalTicks(player, player.getMainHandItem(),
            local(player) && ClientControls.ballWindBoost());
    }

    /**
     * The wind-up spin angle in radians, matching the drawn ball (0 = level in
     * front, PI/2 = top), or NaN when this player is not winding up.
     */
    public static double ballWindupAngle(Player player, float partial) {
        ItemStack stack = player.getMainHandItem();
        if (!is(stack, WeaponKind.BALL_AND_CHAIN)) return Double.NaN;
        long now = player.level().getGameTime();
        if (local(player) && !Double.isNaN(debugWindupAngle)) return debugWindupAngle;
        if (local(player) && debugThrowProgress >= 0.0D) return Double.NaN;
        long started;
        if (local(player)) {
            if (!ClientControls.ballWindup(now)) return Double.NaN;
            started = ClientControls.ballStarted();
        } else {
            if (!activeTag(stack) || stack.getTag().getBoolean("ArsenalBallThrown")) return Double.NaN;
            started = stack.getTag().getLong("ArsenalBallStarted");
        }
        return (now - started + partial) / ballSwingTicks(player) * Math.PI * 2.0D;
    }

    /** Throw progress 0..1 (out until 0.5, back after), or -1 when no throw is playing. */
    public static double ballThrowProgress(Player player, float partial) {
        ItemStack stack = player.getMainHandItem();
        if (!is(stack, WeaponKind.BALL_AND_CHAIN)) return -1.0D;
        if (local(player) && debugThrowProgress >= 0.0D) return debugThrowProgress;
        long now = player.level().getGameTime();
        long started; int duration;
        if (local(player)) {
            if (!ClientControls.ballRelease(now)) return -1.0D;
            started = ClientControls.ballReleaseStarted(); duration = ClientControls.ballReleaseDuration();
        } else {
            if (!activeTag(stack) || !stack.getTag().getBoolean("ArsenalBallThrown")) return -1.0D;
            started = stack.getTag().getLong("ArsenalBallRelease"); duration = stack.getTag().getInt("ArsenalBallDuration");
        }
        return Math.max(0.0D, Math.min(1.0D, (now - started + partial) / Math.max(1, duration)));
    }

    public static int throwCharge(Player player) {
        if (local(player) && debugThrowProgress >= 0.0D) return debugThrowCharge;
        return local(player) ? ClientControls.releasedCharge()
            : player.getMainHandItem().getOrCreateTag().getInt("ArsenalBallCharge");
    }

    public static double throwDistance(Player player) {
        if (local(player) && debugThrowProgress >= 0.0D) return debugThrowDistance;
        return local(player) ? ClientControls.releasedDistance()
            : player.getMainHandItem().getOrCreateTag().getDouble("ArsenalBallDistance");
    }

    public static Vec3 throwDirection(Player player) {
        if (local(player) && debugThrowProgress >= 0.0D) {
            double yaw = Math.toRadians(player.getYRot());
            return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        }
        if (local(player)) return ClientControls.releasedDirection().normalize();
        var tag = player.getMainHandItem().getOrCreateTag();
        return new Vec3(tag.getDouble("ArsenalBallX"), tag.getDouble("ArsenalBallY"), tag.getDouble("ArsenalBallZ"));
    }

    /** Crouch + use with an empty offhand and no attack playing. */
    public static boolean ballGuarding(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (local(player) && com.nanonaitor.arsenal.client.pose.WeaponPoses.debugBallGuard) return is(stack, WeaponKind.BALL_AND_CHAIN);
        return is(stack, WeaponKind.BALL_AND_CHAIN) && player.getOffhandItem().isEmpty() && player.isCrouching()
            && player.isUsingItem() && player.getUseItem() == stack
            && Double.isNaN(ballWindupAngle(player, 0.0F)) && ballThrowProgress(player, 0.0F) < 0.0D;
    }

    public static boolean flailSpinning(Player player) {
        if (!is(player.getMainHandItem(), WeaponKind.FLAIL)) return false;
        if (local(player) && !Double.isNaN(debugFlailAngle)) return true;
        return local(player) ? ClientControls.flailActive()
            : player.isUsingItem() && is(player.getUseItem(), WeaponKind.FLAIL);
    }

    /** The flail ball's orbit angle in radians (world-fixed), or NaN when not spinning. */
    public static double flailAngle(Player player, float partial) {
        if (!flailSpinning(player)) return Double.NaN;
        if (local(player) && !Double.isNaN(debugFlailAngle)) return debugFlailAngle;
        int ticks = ChainWeaponStats.swingIntervalTicks(player, player.getMainHandItem());
        return -(player.level().getGameTime() + partial) / ticks * Math.PI * 2.0D;
    }

    private WeaponVisualState() {}
}
