package com.nanonaitor.arsenal.dev;

import com.mojang.blaze3d.platform.InputConstants;
import com.nanonaitor.arsenal.ArsenalMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

/**
 * DEVELOPMENT ONLY, never in release jars (see build.gradle). Runs only with
 * {@code -Darsenal.combatProbe=true} ({@code gradlew runClient -PcombatProbe}): opens a flat
 * survival world, then for each scenario gives the player a weapon pair, puts a still zombie
 * in front of it, presses the real attack/use keys and counts the damage the zombie takes.
 * Each result is logged as "ARSENAL COMBAT PROBE ... -> PASS/FAIL"; then the game closes.
 * Used to check that other combat mods (such as an off-hand attack mod) do not add or
 * swallow hits from Arsenal's own off-hand weapons.
 */
public final class CombatProbe {
    private static final String ID = ArsenalMod.MOD_ID + ":";
    private record Press(int tick, boolean use) {}
    private record Scenario(String name, String main, String off, List<Press> presses, int expectedHits) {}
    private static final List<Scenario> SCENARIOS = new ArrayList<>();
    private static boolean launched, listening;
    private static int worldTicks, index = -1, tick;
    private static int hits;
    private static float damage;
    private static Zombie target;
    private static int releaseAt = -1;
    private static int failures;

    private static Press use(int t) { return new Press(t, true); }
    private static Press attack(int t) { return new Press(t, false); }

    private static void build() {
        String scim = ID + "scimitar_iron", claws = ID + "claws_iron", linked = ID + "linked_claws_iron";
        // A lone off-hand scimitar strikes on right-click: one press, one hit.
        // Presses wait 40 ticks so every hand's attack bar has fully charged.
        SCENARIOS.add(new Scenario("offhand scimitar, right-click", null, scim, List.of(use(40)), 1));
        SCENARIOS.add(new Scenario("sword + offhand scimitar, right-click", "minecraft:iron_sword", scim, List.of(use(40)), 1));
        SCENARIOS.add(new Scenario("blade staff + offhand scimitar, right-click", ID + "blade_staff_iron", scim, List.of(use(40)), 1));
        // Dual scimitars alternate hands on left-click: two presses, two hits.
        SCENARIOS.add(new Scenario("dual scimitars, two left-clicks", scim, scim, List.of(attack(5), attack(40)), 2));
        // Paired claws: left-click is the main claw, right-click the linked off-hand claw.
        SCENARIOS.add(new Scenario("paired claws, right-click", claws, linked, List.of(use(5)), 1));
        SCENARIOS.add(new Scenario("paired claws, left-click", claws, linked, List.of(attack(5)), 1));
        // Control: a plain sword left-click is one vanilla hit.
        SCENARIOS.add(new Scenario("iron sword, left-click", "minecraft:iron_sword", null, List.of(attack(5)), 1));
    }

    private static ItemStack stack(String id) {
        return id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id)));
    }

    private static void onDamage(LivingDamageEvent event) {
        // Only the player's hits count (not sunlight fire and the like).
        if (event.getEntity() == target && event.getAmount() > 0 && event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player) {
            hits++; damage += event.getAmount();
            com.mojang.logging.LogUtils.getLogger().info("ARSENAL COMBAT PROBE hit at tick {}: {} ({})", tick, event.getAmount(), event.getSource().getMsgId());
        }
    }

    public static void tick() {
        if (!Boolean.getBoolean("arsenal.combatProbe")) return;
        Minecraft mc = Minecraft.getInstance();
        if (!launched) {
            if (mc.getOverlay() != null) return;
            if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
                mc.options.onboardAccessibility = false; mc.options.save(); mc.setScreen(new TitleScreen()); return;
            }
            if (!(mc.screen instanceof TitleScreen)) return;
            launched = true;
            build();
            String name = "arsenal_combat_probe_" + System.currentTimeMillis();
            LevelSettings settings = new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.EASY, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(1L, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                    .value().createWorldDimensions());
            return;
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        ServerPlayer server = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (server == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (!listening) { listening = true; MinecraftForge.EVENT_BUS.addListener(CombatProbe::onDamage); }
        if (++worldTicks < 100) {
            if (worldTicks == 5) {
                var commands = mc.getSingleplayerServer().getCommands();
                var source = mc.getSingleplayerServer().createCommandSourceStack();
                for (String c : new String[] {"time set noon", "weather clear", "gamerule doDaylightCycle false",
                    "gamerule doMobSpawning false"}) commands.performPrefixedCommand(source, c);
            }
            return;
        }
        if (index < 0) { index = 0; begin(mc, server); return; }
        if (index >= SCENARIOS.size()) {
            com.mojang.logging.LogUtils.getLogger().info("ARSENAL COMBAT PROBE DONE: {} scenarios, {} failed",
                SCENARIOS.size(), failures);
            mc.stop();
            return;
        }
        Scenario scenario = SCENARIOS.get(index);
        tick++;
        // Keep looking straight at the zombie so the crosshair stays on it.
        mc.player.setYRot(0.0F); mc.player.setXRot(20.0F);
        // Keys are held for three ticks, like a quick real click: Arsenal reads held keys each tick.
        if (releaseAt == tick) { KeyMapping.set(mc.options.keyUse.getKey(), false); KeyMapping.set(mc.options.keyAttack.getKey(), false); }
        for (Press press : scenario.presses) {
            if (press.tick != tick) continue;
            KeyMapping key = press.use ? mc.options.keyUse : mc.options.keyAttack;
            InputConstants.Key bound = key.getKey();
            KeyMapping.set(bound, true);
            KeyMapping.click(bound);
            releaseAt = tick + 3;
        }
        int last = scenario.presses.get(scenario.presses.size() - 1).tick;
        if (tick < last + 30) return;
        KeyMapping.set(mc.options.keyUse.getKey(), false);
        KeyMapping.set(mc.options.keyAttack.getKey(), false);
        boolean pass = hits == scenario.expectedHits;
        if (!pass) failures++;
        com.mojang.logging.LogUtils.getLogger().info("ARSENAL COMBAT PROBE {}: hits={} expected={} damage={} -> {}",
            scenario.name, hits, scenario.expectedHits, damage, pass ? "PASS" : "FAIL");
        index++;
        if (index < SCENARIOS.size()) begin(mc, server);
    }

    private static void begin(Minecraft mc, ServerPlayer server) {
        Scenario scenario = SCENARIOS.get(index);
        tick = 0; hits = 0; damage = 0; releaseAt = -1;
        server.setItemSlot(EquipmentSlot.MAINHAND, stack(scenario.main));
        server.setItemSlot(EquipmentSlot.OFFHAND, stack(scenario.off));
        server.teleportTo(0.5D, -60.0D, 0.5D);
        server.setYRot(0.0F); server.setXRot(20.0F);
        server.resetAttackStrengthTicker();
        if (target != null) target.discard();
        var level = server.serverLevel();
        target = EntityType.HUSK.create(level); // husks do not burn in daylight
        target.moveTo(0.5D, -60.0D, 2.3D, 180.0F, 0.0F);
        target.setNoAi(true);
        target.setSilent(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0D);
        target.setHealth(1000.0F);
        level.addFreshEntity(target);
    }

    private CombatProbe() {}
}
