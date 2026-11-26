package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.config.ArsenalConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Electric Dragonbone chain lightning, rebuilt natively because Ice & Fire's
 * 1.20.1 release has no chain-lightning API. Matches the 1.12.2 defaults: the
 * struck target takes 5 lightning damage, then the bolt jumps to the nearest
 * enemy within 8 blocks it can see, 4, 3, 2, 1. Lightning damage bypasses armor.
 */
public final class ChainLightning {
    /** Strikes from the target and returns everything hit, in order. */
    public static List<LivingEntity> strike(Player attacker, LivingEntity target) {
        List<LivingEntity> hit = new ArrayList<>();
        if (!ArsenalConfig.CHAIN_LIGHTNING.get() || !(attacker.level() instanceof ServerLevel level)) return hit;
        List<? extends Double> damage = ArsenalConfig.CHAIN_LIGHTNING_DAMAGE.get();
        double range = ArsenalConfig.CHAIN_LIGHTNING_RANGE.get();
        // No direct attacker: the bolt itself is the source, so it never re-triggers weapon effects.
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(DamageTypes.INDIRECT_MAGIC), null, attacker);
        Set<Integer> done = new HashSet<>();
        done.add(attacker.getId());
        LivingEntity current = target;
        Vec3 from = attacker.getEyePosition();
        for (int hop = 0; hop < damage.size() && current != null; hop++) {
            done.add(current.getId());
            Vec3 at = current.getBoundingBox().getCenter();
            arc(level, from, at);
            current.invulnerableTime = 0;
            if (current.hurt(source, damage.get(hop).floatValue())) hit.add(current);
            from = at;
            LivingEntity next = null;
            double best = range * range;
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                    current.getBoundingBox().inflate(range), e -> !done.contains(e.getId()) && validTarget(attacker, e))) {
                double distance = candidate.distanceToSqr(current);
                if (distance <= best && current.hasLineOfSight(candidate)) { best = distance; next = candidate; }
            }
            current = next;
        }
        if (!hit.isEmpty()) level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
            SoundSource.PLAYERS, 0.6F, 1.4F + level.getRandom().nextFloat() * 0.2F);
        return hit;
    }

    private static boolean validTarget(Player attacker, LivingEntity target) {
        return target != attacker && target.isAlive() && !target.isSpectator() && !attacker.isAlliedTo(target)
            && (!(target instanceof Player other) || attacker.canHarmPlayer(other)
                && (attacker.getServer() == null || attacker.getServer().isPvpAllowed()));
    }

    /** A jagged spark arc between two points. */
    private static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        int steps = Math.max(4, (int)(d.length() * 4.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(d.scale(i / (double)steps));
            double jitter = i == 0 || i == steps ? 0.0D : 0.12D;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                p.x + (level.getRandom().nextDouble() - 0.5D) * jitter,
                p.y + (level.getRandom().nextDouble() - 0.5D) * jitter,
                p.z + (level.getRandom().nextDouble() - 0.5D) * jitter, 1, 0, 0, 0, 0);
        }
    }

    private ChainLightning() {}
}
