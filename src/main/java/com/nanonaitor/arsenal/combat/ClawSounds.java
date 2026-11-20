package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.registry.ModSounds;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Claw hit sounds: a high metal slash on every hit (main hand and linked
 * off-hand claw), with a ringing accent on every third hit of a combo. A
 * combo ends after a second without a hit.
 */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ClawSounds {
    private static final long COMBO_TIMEOUT_TICKS = 20L;
    private static final Map<EntityPlayer, long[]> COMBOS = new WeakHashMap<>();

    private ClawSounds() {}

    /** Server: a claw hit landed. */
    public static void slash(EntityPlayer player) {
        long now = player.world.getTotalWorldTime();
        long[] combo = COMBOS.get(player);
        if (combo == null) {
            combo = new long[] {0L, now};
            COMBOS.put(player, combo);
        }
        if (now - combo[1] > COMBO_TIMEOUT_TICKS) combo[0] = 0L;
        combo[0]++;
        combo[1] = now;
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            ModSounds.CLAWS_SLASH, player.getSoundCategory(),
            0.7F, 1.25F + player.getRNG().nextFloat() * 0.25F);
        if (combo[0] % 3L == 0L) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                ModSounds.CLAWS_RING, player.getSoundCategory(),
                0.22F, 1.8F + player.getRNG().nextFloat() * 0.15F);
        }
    }

    /** Main-hand claw hits go through the normal attack. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (event.isCanceled() || player.world.isRemote
            || !(player.getHeldItemMainhand().getItem() instanceof ItemClaws)) return;
        slash(player);
    }
}
