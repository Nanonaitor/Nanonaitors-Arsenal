package com.nanonaitor.arsenal.combat;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Weak player-keyed state with separate storage for client and server players.
 *
 * In singleplayer the client and integrated-server threads run at the same
 * time. A single shared WeakHashMap can be corrupted when both touch it, since
 * even {@code get} expunges stale entries. Each side only ever sees its own map.
 */
public final class SidedPlayerMap<V> {
    private final Map<EntityPlayer, V> client = new WeakHashMap<>();
    private final Map<EntityPlayer, V> server = new WeakHashMap<>();

    private Map<EntityPlayer, V> side(EntityPlayer player) {
        return player.world.isRemote ? client : server;
    }

    public V get(EntityPlayer player) { return side(player).get(player); }
    public V put(EntityPlayer player, V value) { return side(player).put(player, value); }
    public V remove(EntityPlayer player) { return side(player).remove(player); }
    public boolean containsKey(EntityPlayer player) { return side(player).containsKey(player); }
}
