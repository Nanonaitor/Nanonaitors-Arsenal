package com.nanonaitor.arsenal.client;

/** Standalone regression test; no Minecraft or optional mod runtime required. */
public final class ChainSwingSoundFilterTest {
    public static void main(String[] args) {
        for (String type : new String[] {"2h", "metal_axe", "metal_blade",
                "metal_blunt", "normal", "quick", "slow"}) {
            for (String side : new String[] {"", "_left", "_right"}) {
                check("bettercombatmod", "player.swing_" + type + side, true);
            }
        }
        check("bettercombatmod", "player.swordslash", true);
        for (String type : new String[] {"sword", "blunt", "tool"}) {
            check("dsurround", type + ".swing", true);
        }
        for (String type : new String[] {"strong", "weak", "nodamage", "sweep", "crit"}) {
            check("minecraft", "entity.player.attack." + type, true);
        }
        check("nanonaitors_arsenal", "ball_chain.swing", false);
        check("nanonaitors_arsenal", "ball_chain.hit", false);
        check("minecraft", "entity.player.hurt", false);
        check("dsurround", "sword.equip", false);
        check("bettercombatmod", "player.equip_blade", false);
        System.out.println("Passed 35 swing sound classification cases");
    }

    private static void check(String domain, String path, boolean expected) {
        if (ChainSwingSoundFilter.isExtraMeleeSound(domain, path) != expected) {
            throw new AssertionError(domain + ":" + path);
        }
    }
}
