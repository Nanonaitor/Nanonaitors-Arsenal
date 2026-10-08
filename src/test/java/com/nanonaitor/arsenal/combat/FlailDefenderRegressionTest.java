package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.client.ParasiteChainColors;
import com.nanonaitor.arsenal.item.*;
import net.minecraft.init.Bootstrap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public final class FlailDefenderRegressionTest {
    private static int checks;
    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Flail regression check " + checks);
    }
    public static void main(String[] args) throws Exception {
        Bootstrap.register();
        check(ChainWeaponStats.intervalForSpeed(0.8D) == 25);
        check(ChainWeaponStats.intervalForSpeed(1.6D * 1.4D) == 9);
        check(ChainWeaponStats.intervalForSpeed((0.8D + 1.6D) * 1.4D) == 6);
        check(ChainWeaponStats.intervalForSpeed(100D) == 1);
        check(ParasiteChainColors.colorForPart(1) == 0xFFCC3030);
        check(ParasiteChainColors.colorForPart(3) == 0xFFCC3030);
        for (int part : new int[]{0,2,4,5}) check(ParasiteChainColors.colorForPart(part) == -1);
        for (WeaponTier tier : new WeaponTier[]{WeaponTier.LIVING, WeaponTier.SENTIENT}) {
            ItemArsenalWeapon[] weapons = {new ItemMorningStar(tier), new ItemScimitar(tier),
                new ItemDoubleBladedScimitar(tier), new ItemClaws(tier), new ItemFlail(tier),
                new ItemBallAndChain(tier), new ItemBatteringRam(tier), new ItemLinkedClaw(tier)};
            for (ItemArsenalWeapon weapon : weapons) check(weapon.getMaxDamage() == 1000);
        }
        // The tick and direct-click paths share this gate. Guard against accidentally
        // reverting the client to the general shield-priority gate again.
        String input = new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/nanonaitor/arsenal/client/FlailInputHandler.java")), StandardCharsets.UTF_8);
        check(input.contains("ShieldUsePriority.flailSuppressed(player)"));
        check(!input.contains("ShieldUsePriority.requested(player)"));
        System.out.println("Flail/Defender regression: " + checks + " checks passed.");
    }
}
