package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.registry.ModSounds;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Claw hit sounds: a high metal slash on every hit (main claw and linked
 * off-hand claw), with a ringing accent on every third hit of a combo. A
 * combo ends after a second without a hit.
 */
public final class ClawSounds {
    private static final long COMBO_TIMEOUT_TICKS = 20L;
    private static final Map<Player, long[]> COMBOS = new WeakHashMap<>();

    /** Server: a claw hit landed. Returns the hit's number in the current combo. */
    public static long slash(Player player) {
        if (player.level().isClientSide) return 0L;
        long now = player.level().getGameTime();
        long[] combo = COMBOS.computeIfAbsent(player, ignored -> new long[] {0L, now});
        if (now - combo[1] > COMBO_TIMEOUT_TICKS) combo[0] = 0L;
        combo[0]++;
        combo[1] = now;
        player.level().playSound(null, player.blockPosition(), ModSounds.CLAWS_SLASH.get(), SoundSource.PLAYERS,
            0.7F, 1.25F + player.getRandom().nextFloat() * 0.25F);
        if (ringsOn(combo[0])) player.level().playSound(null, player.blockPosition(), ModSounds.CLAWS_RING.get(),
            SoundSource.PLAYERS, 0.22F, 1.8F + player.getRandom().nextFloat() * 0.15F);
        return combo[0];
    }

    /** Every third hit of a combo rings. */
    public static boolean ringsOn(long hit) {
        return hit > 0 && hit % 3L == 0L;
    }

    private ClawSounds() {}
}
