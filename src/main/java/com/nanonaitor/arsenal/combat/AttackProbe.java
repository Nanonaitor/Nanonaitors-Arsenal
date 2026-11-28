package com.nanonaitor.arsenal.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/**
 * Tells real attacks from protection checks.
 *
 * Some mods ask "may this player act here?" by posting a made-up attack and
 * seeing whether anything cancels it. Callable Horses does this before calling
 * a horse: it moves the horse onto the player and posts an AttackEntityEvent.
 * Arsenal cancels vanilla attacks for weapons that make their own hits (flail,
 * ball and chain, morning star...), which those mods read as a protected area,
 * so the horse could never be called. Arsenal's handlers ignore such checks.
 */
public final class AttackProbe {
    private AttackProbe() {}

    /** True for a made-up attack: the target stands exactly where the player is. */
    public static boolean isAreaProbe(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        Entity target = event.getTarget();
        return player != null && target != null && target != player
            && target.getDistanceSq(player.posX, player.posY, player.posZ) < 1.0E-4D;
    }
}
