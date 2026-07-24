package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.item.WeaponTier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class VisualEffects {
    public static void flail(ServerLevel level, ServerPlayer player, WeaponTier tier) {
        double base = Math.toRadians(player.getYRot());
        for (int i = 0; i < 32; i++) {
            double angle = base + Math.PI * 2.0D * i / 32.0D;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                player.getX() + Math.cos(angle) * 4.0D, player.getY() + 1.0D,
                player.getZ() + Math.sin(angle) * 4.0D, 1, 0, 0, 0, 0);
        }
    }
    public static void ballWindup(ServerLevel level, ServerPlayer player, WeaponTier tier, int charge) {
        Vec3 look = player.getLookAngle();
        level.sendParticles(ParticleTypes.CRIT, player.getX() + look.x * 1.2D,
            player.getY() + 1.0D + Math.sin(player.tickCount * 0.5D) * 0.7D,
            player.getZ() + look.z * 1.2D, 3, 0.08D, 0.08D, 0.08D, 0.02D);
    }
    public static void ballRelease(ServerLevel level, ServerPlayer player, WeaponTier tier,
            CombatEvents.BallState state, double progress) {
        double distance = state.distance() * Math.sin(Math.PI * progress);
        Vec3 point = player.getEyePosition().add(player.getLookAngle().scale(distance));
        level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 5, 0.12D, 0.12D, 0.12D, 0.02D);
    }
    private VisualEffects() {}
}
