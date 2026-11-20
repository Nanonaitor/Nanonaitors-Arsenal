package com.nanonaitor.arsenal.combat;

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
        for (WeaponTier tier : new WeaponTier[]{WeaponTier.LIVING, WeaponTier.SENTIENT}) {
            ItemArsenalWeapon[] weapons = {new ItemMorningStar(tier), new ItemScimitar(tier),
                new ItemDoubleBladedScimitar(tier), new ItemClaws(tier), new ItemFlail(tier),
                new ItemBallAndChain(tier), new ItemBatteringRam(tier), new ItemLinkedClaw(tier)};
            for (ItemArsenalWeapon weapon : weapons) {
                // Heavy (block-cost) families last 50% longer than the tier's base.
                boolean heavy = weapon instanceof ItemBallAndChain || weapon instanceof ItemBatteringRam;
                check(weapon.getMaxDamage() == (heavy ? 1500 : 1000));
                weapon.setBaseDurability(2000);
                check(weapon.getMaxDamage() == (heavy ? 3000 : 2000));
            }
        }
        check(new ItemFlail(WeaponTier.BRONZE).getMaxDamage() == 350);
        // Thrown-ball projectile smashing: segment distance between the ball's
        // and a projectile's per-tick paths.
        net.minecraft.util.math.Vec3d o = new net.minecraft.util.math.Vec3d(0, 0, 0);
        check(SegmentMath.distanceSq(o, new net.minecraft.util.math.Vec3d(2, 0, 0),
            new net.minecraft.util.math.Vec3d(1, -1, 0), new net.minecraft.util.math.Vec3d(1, 1, 0)) < 1.0E-9);
        check(Math.abs(SegmentMath.distanceSq(o, new net.minecraft.util.math.Vec3d(2, 0, 0),
            new net.minecraft.util.math.Vec3d(0, 1, 0), new net.minecraft.util.math.Vec3d(2, 1, 0)) - 1.0D) < 1.0E-9);
        // A fast arrow crossing the ball's short path between two ticks still counts.
        check(SegmentMath.distanceSq(o, new net.minecraft.util.math.Vec3d(0, 0, 1),
            new net.minecraft.util.math.Vec3d(-3, 0, 0.5), new net.minecraft.util.math.Vec3d(3, 0, 0.5)) < 1.0E-9);
        // Stationary ball versus a projectile passing 2 blocks away.
        check(Math.abs(SegmentMath.distanceSq(o, o,
            new net.minecraft.util.math.Vec3d(-3, 2, 0), new net.minecraft.util.math.Vec3d(3, 2, 0)) - 4.0D) < 1.0E-9);
        check(new ItemBatteringRam(WeaponTier.IRON).getMaxDamage() == 375);
        // The tick and direct-click paths share this gate. Guard against accidentally
        // reverting the client to the general shield-priority gate again.
        String input = new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/nanonaitor/arsenal/client/FlailInputHandler.java")), StandardCharsets.UTF_8);
        check(input.contains("ShieldUsePriority.weaponAttackSuppressed(player)"));
        check(!input.contains("ShieldUsePriority.requested(player)"));
        // The Ball and Chain keeps winding up and throwing while a Defender guards.
        String ball = new String(Files.readAllBytes(Paths.get(
            "src/main/java/com/nanonaitor/arsenal/client/BallAndChainInputHandler.java")), StandardCharsets.UTF_8);
        check(ball.contains("ShieldUsePriority.weaponAttackSuppressed(player)"));
        check(!ball.contains("ShieldUsePriority.requested("));
        check(ball.contains("raiseDefenderOverBall"));
        // World equipment never swaps a modpack's hand-made (legendary) items.
        net.minecraft.world.storage.loot.conditions.LootCondition[] none =
            new net.minecraft.world.storage.loot.conditions.LootCondition[0];
        net.minecraft.world.storage.loot.functions.LootFunction count =
            new net.minecraft.world.storage.loot.functions.SetCount(none,
                new net.minecraft.world.storage.loot.RandomValueRange(1));
        net.minecraft.nbt.NBTTagCompound named = new net.minecraft.nbt.NBTTagCompound();
        net.minecraft.nbt.NBTTagCompound display = new net.minecraft.nbt.NBTTagCompound();
        display.setString("Name", "Gungnir");
        named.setTag("display", display);
        net.minecraft.world.storage.loot.functions.LootFunction setNbt =
            new net.minecraft.world.storage.loot.functions.SetNBT(none, named);
        check(com.nanonaitor.arsenal.compat.WorldEquipmentCompat.isPlainLoot(null));
        check(com.nanonaitor.arsenal.compat.WorldEquipmentCompat.isPlainLoot(
            new net.minecraft.world.storage.loot.functions.LootFunction[] {count}));
        check(!com.nanonaitor.arsenal.compat.WorldEquipmentCompat.isPlainLoot(
            new net.minecraft.world.storage.loot.functions.LootFunction[] {count, setNbt}));
        net.minecraft.item.ItemStack plain = new net.minecraft.item.ItemStack(net.minecraft.init.Items.IRON_SWORD);
        check(!com.nanonaitor.arsenal.compat.WorldEquipmentCompat.isSpecialStack(plain));
        plain.addEnchantment(net.minecraft.init.Enchantments.SHARPNESS, 2);
        check(!com.nanonaitor.arsenal.compat.WorldEquipmentCompat.isSpecialStack(plain));
        net.minecraft.item.ItemStack legendary = new net.minecraft.item.ItemStack(net.minecraft.init.Items.IRON_SWORD);
        legendary.setTagCompound(named.copy());
        check(com.nanonaitor.arsenal.compat.WorldEquipmentCompat.isSpecialStack(legendary));
        System.out.println("Flail/Defender regression: " + checks + " checks passed.");
    }
}
