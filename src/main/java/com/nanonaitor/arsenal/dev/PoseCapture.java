package com.nanonaitor.arsenal.dev;

import com.mojang.blaze3d.platform.NativeImage;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.client.ClientControls;
import com.nanonaitor.arsenal.client.WeaponVisualState;
import com.nanonaitor.arsenal.client.pose.AttackTiming;
import com.nanonaitor.arsenal.client.pose.WeaponPoses;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * DEVELOPMENT ONLY, never in release jars (see build.gradle). Runs only with
 * {@code -Darsenal.poseCapture=true} ({@code gradlew runClient -PposeCapture}):
 * opens a flat creative test world, poses the player with each Arsenal weapon in
 * each animation state, photographs it from the front and the side (and in first
 * person where that view changed), writes the shots to
 * {@code run/arsenal-pose-capture/}, then closes the game.
 */
public final class PoseCapture {
    private static final String WORLD = "arsenal_pose_capture";
    private static final String ID = ArsenalMod.MOD_ID + ":";
    private static boolean launched;
    private static int worldTicks, shotIndex = -1, view, wait;
    private static ArmorStand camera;
    private static final List<Shot> SHOTS = new ArrayList<>();
    private static File out;

    private static final class Shot {
        final String sheet, label, main, off;
        final Consumer<WeaponPoses.Input> override;
        Runnable setup = () -> {};
        boolean firstPerson, crouch, enchanted;
        Shot(String sheet, String label, String main, String off, Consumer<WeaponPoses.Input> override) {
            this.sheet = sheet; this.label = label; this.main = main; this.off = off; this.override = override;
        }
    }

    private static Shot add(String sheet, String label, String main, String off, Consumer<WeaponPoses.Input> override) {
        Shot shot = new Shot(sheet, label, main, off, override);
        SHOTS.add(shot);
        return shot;
    }

    private static void build() {
        String torch = "minecraft:torch";
        String ram = ID + "battering_ram_iron";
        add("battering_ram", "two hands, carry", ram, null, in -> in.active = false);
        add("battering_ram", "ramming", ram, null, in -> in.active = true).setup = () -> WeaponPoses.debugSegmentAge = 12;
        add("battering_ram", "ramming, impact recoil", ram, null, in -> { in.active = true; in.attackPhase = 0.5F; })
            .setup = () -> WeaponPoses.debugSegmentAge = 12;
        add("battering_ram", "attack lunge 25%", ram, null, in -> { in.active = false; in.attackPhase = 0.25F; });
        add("battering_ram", "attack swing 60%", ram, null, in -> { in.active = false; in.attackPhase = 0.6F; });
        add("battering_ram", "one hand, hip", ram, torch, in -> in.attackPhase = -1);
        add("battering_ram", "one hand, lunge 25%", ram, torch, in -> in.attackPhase = 0.25F);

        String staff = ID + "blade_staff_iron";
        add("blade_staff", "two hands, crosswise", staff, null, in -> { in.active = false; in.attackPhase = -1; });
        add("blade_staff", "paddle right 50%", staff, null, in -> { in.active = false; in.attackPhase = 0.5F; in.attackCount = 0; });
        add("blade_staff", "paddle left 50%", staff, null, in -> { in.active = false; in.attackPhase = 0.5F; in.attackCount = 1; });
        add("blade_staff", "reflecting", staff, null, in -> in.active = true).setup = () -> ClientControls.debugReflect = true;
        add("blade_staff", "one hand, idle", staff, torch, in -> { in.active = false; in.attackPhase = -1; });
        for (float phase : new float[] {0.2F, 0.45F, 0.7F})
            add("blade_staff", "one hand strike " + Math.round(phase * 100) + "%", staff, torch,
                in -> { in.active = false; in.attackPhase = phase; });

        String ball = ID + "ball_and_chain_iron";
        add("ball_and_chain", "idle", ball, null, in -> {});
        for (int q = 0; q < 4; q++) {
            double angle = q * Math.PI / 2.0D;
            add("ball_and_chain", "wind-up " + (q * 90) + " deg", ball, null, in -> {})
                .setup = () -> WeaponVisualState.debugWindupAngle = angle;
        }
        for (double t : new double[] {0.1D, 0.3D, 0.58D, 0.75D})
            add("ball_and_chain", "throw " + Math.round(t * 100) + "%", ball, null, in -> {})
                .setup = () -> { WeaponVisualState.debugThrowProgress = t; WeaponVisualState.debugThrowCharge = 2; };
        add("ball_and_chain", "full-charge throw 35% (trail, tumble)", ball, null, in -> {})
            .setup = () -> { WeaponVisualState.debugThrowProgress = 0.35D; WeaponVisualState.debugThrowCharge = 3; };
        Shot guard = add("ball_and_chain", "crouch guard", ball, null, in -> {});
        guard.setup = () -> WeaponPoses.debugBallGuard = true; guard.crouch = true;
        add("ball_and_chain", "armour crack icon", ball, null, in -> {}).setup = () ->
            com.nanonaitor.arsenal.client.ArsenalClientFx.receive(com.nanonaitor.arsenal.network.ModNetwork.FX_ARMOR_BREAK,
                Minecraft.getInstance().player.getId(), 200);
        add("ball_and_chain", "one hand wind-up 90 deg", ball, torch, in -> {})
            .setup = () -> WeaponVisualState.debugWindupAngle = Math.PI / 2.0D;
        Shot guardFp = add("ball_and_chain", "first person guard", ball, null, in -> {});
        guardFp.setup = () -> WeaponPoses.debugBallGuard = true; guardFp.firstPerson = true; guardFp.crouch = true;

        String flail = ID + "flail_iron";
        add("flail", "idle", flail, null, in -> {});
        for (int q = 0; q < 4; q++) {
            double angle = -q * Math.PI / 2.0D;
            add("flail", "whirl " + (q * 90) + " deg", flail, null, in -> {}).setup = () -> WeaponVisualState.debugFlailAngle = angle;
        }
        for (int q = 0; q < 4; q++) {
            double angle = -q * Math.PI / 2.0D - Math.PI / 4.0D;
            Shot fp = add("flail", "first person " + (q * 90 + 45) + " deg", flail, null, in -> {});
            fp.setup = () -> WeaponVisualState.debugFlailAngle = angle; fp.firstPerson = true;
        }

        String claws = ID + "claws_iron";
        add("claws", "ready", claws, null, in -> { in.attackPhase = -1; in.offAttackPhase = -1; });
        for (int count = 1; count <= 3; count++) for (float phase : new float[] {0.1F, 0.35F, 0.55F}) {
            int c = count;
            add("claws", "main strike " + c + (c == 3 ? " (lunge)" : "") + " " + Math.round(phase * 100) + "%", claws, null,
                in -> { in.attackPhase = phase; in.attackCount = c; in.offAttackPhase = -1; });
        }
        add("claws", "off-hand strike 35%", claws, null, in -> { in.attackPhase = -1; in.offAttackPhase = 0.35F; in.offAttackCount = 1; });
        add("claws", "both hands at once 40%", claws, null,
            in -> { in.attackPhase = 0.4F; in.attackCount = 1; in.offAttackPhase = 0.35F; in.offAttackCount = 2; });

        String scim = ID + "scimitar_iron";
        add("scimitars", "crossed guard", scim, scim, in -> { in.kind = WeaponPoses.Kind.SCIMITARS; in.active = true; in.attackPhase = -1; });
        for (float phase : new float[] {0.15F, 0.45F, 0.65F, 0.85F})
            add("scimitars", "bash " + Math.round(phase * 100) + "%", scim, scim,
                in -> { in.kind = WeaponPoses.Kind.SCIMITARS; in.active = true; in.attackPhase = phase; });
        for (float phase : new float[] {-1.0F, 0.45F, 0.65F}) {
            Shot fp = add("scimitars", "first person " + (phase < 0 ? "guard" : "bash " + Math.round(phase * 100) + "%"), scim, scim, in -> {});
            fp.setup = () -> ClientControls.debugScimitarBash = phase < 0 ? 0.0F : phase; fp.firstPerson = true;
        }

        // A single scimitar's ordinary swing leaves a faint streak in the air.
        for (float phase : new float[] {0.3F, 0.5F})
            add("scimitars", "single swing streak " + Math.round(phase * 100) + "%", scim, null, in -> {}).setup = () -> {
                var p = Minecraft.getInstance().player;
                AttackTiming.debugPhase = phase;
                p.swinging = true; p.swingingArm = net.minecraft.world.InteractionHand.MAIN_HAND;
                p.swingTime = 2; p.attackAnim = p.oAttackAnim = phase;
            };

        String star = ID + "morning_star_iron";
        add("morning_star", "idle", star, null, in -> {});
        for (float k : new float[] {0.0F, 0.25F, 0.5F, 0.75F, 1.0F}) {
            add("morning_star", "charge " + Math.round(k * 100) + "%", star, null, in -> in.active = true)
                .setup = () -> { ClientControls.debugMorningCharge = k; WeaponPoses.debugSegmentAge = k * 40.0F; };
            Shot fp = add("morning_star", "first person charge " + Math.round(k * 100) + "%", star, null, in -> {});
            fp.setup = () -> ClientControls.debugMorningCharge = k; fp.firstPerson = true;
        }
        add("morning_star", "release swing 20%", star, null, in -> { in.active = false; in.attackPhase = 0.2F; });

        String bulwark = ID + "sun_war_bulwark";
        add("bulwark", "two hands, carry", bulwark, null, in -> in.active = false);
        add("bulwark", "braced", bulwark, null, in -> in.active = true).setup = () -> {
            var p = Minecraft.getInstance().player;
            if (!p.isUsingItem()) p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        };
        add("bulwark", "one hand, carry", bulwark, torch, in -> {});

        // Enchanted: the glint runs along the chain and ball too.
        add("glint", "enchanted ball wind-up", ball, null, in -> {}).setup = () -> WeaponVisualState.debugWindupAngle = Math.PI / 2.0D;
        SHOTS.get(SHOTS.size() - 1).enchanted = true;
        add("glint", "enchanted flail whirl", flail, null, in -> {}).setup = () -> WeaponVisualState.debugFlailAngle = -Math.PI / 2.0D;
        SHOTS.get(SHOTS.size() - 1).enchanted = true;

        // Custom materials from run/config/nanonaitors_arsenal-materials.toml (skipped when none are set).
        for (var tier : com.nanonaitor.arsenal.item.WeaponTier.activeValues()) {
            if (!tier.custom()) continue;
            String t = tier.id;
            add("custom_" + t, "morning star", ID + "morning_star_" + t, null, in -> {});
            add("custom_" + t, "scimitars guard", ID + "scimitar_" + t, ID + "scimitar_" + t,
                in -> { in.kind = WeaponPoses.Kind.SCIMITARS; in.active = true; in.attackPhase = -1; });
            add("custom_" + t, "blade staff", ID + "blade_staff_" + t, null, in -> { in.active = false; in.attackPhase = -1; });
            add("custom_" + t, "claws", ID + "claws_" + t, null, in -> { in.attackPhase = -1; in.offAttackPhase = -1; });
            add("custom_" + t, "battering ram", ID + "battering_ram_" + t, null, in -> in.active = false);
            add("custom_" + t, "flail whirl", ID + "flail_" + t, null, in -> {}).setup = () -> WeaponVisualState.debugFlailAngle = -Math.PI / 2.0D;
            add("custom_" + t, "ball wind-up", ID + "ball_and_chain_" + t, null, in -> {}).setup = () -> WeaponVisualState.debugWindupAngle = Math.PI / 2.0D;
        }
    }

    private static void reset() {
        WeaponPoses.debugOverride = null;
        WeaponPoses.debugBallGuard = false;
        WeaponPoses.debugSegmentAge = -1.0F;
        WeaponPoses.debugGuard = null;
        WeaponVisualState.debugWindupAngle = Double.NaN;
        WeaponVisualState.debugThrowProgress = -1.0D;
        WeaponVisualState.debugFlailAngle = Double.NaN;
        AttackTiming.debugPhase = -1.0F;
        ClientControls.debugMorningCharge = -1.0F;
        ClientControls.debugScimitarBash = -1.0F;
        ClientControls.debugReflect = false;
    }

    public static void tick() {
        if (!Boolean.getBoolean("arsenal.poseCapture")) return;
        Minecraft mc = Minecraft.getInstance();
        if (!launched) {
            if (mc.getOverlay() != null) return;
            // A first launch opens the accessibility onboarding screen instead of the title.
            if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
                mc.options.onboardAccessibility = false;
                mc.options.save();
                mc.setScreen(new TitleScreen());
                return;
            }
            if (!(mc.screen instanceof TitleScreen)) return;
            launched = true;
            build();
            out = new File(mc.gameDirectory, "arsenal-pose-capture");
            out.mkdirs();
            for (File old : out.listFiles()) old.delete();
            String name = WORLD + "_" + System.currentTimeMillis();
            LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(1L, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                    .value().createWorldDimensions());
            return;
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        var server = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (server == null) return;
        // Keep the game running while the window is in the background.
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (++worldTicks < 120) {
            if (worldTicks == 5) {
                var commands = mc.getSingleplayerServer().getCommands();
                var source = mc.getSingleplayerServer().createCommandSourceStack();
                for (String c : new String[] {"time set noon", "weather clear", "gamerule doDaylightCycle false",
                    "gamerule doWeatherCycle false", "gamerule doMobSpawning false"}) commands.performPrefixedCommand(source, c);
            }
            return;
        }
        if (shotIndex < 0) {
            shotIndex = 0; view = 0; wait = 0;
            mc.options.hideGui = true;
            camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level); hookCamera();
            checkRemoteSwings(mc);
            com.mojang.logging.LogUtils.getLogger().info("ARSENAL CONFIG SYNC CHECK: server values received -> {}",
                com.nanonaitor.arsenal.config.SyncedConfig.active() ? "PASS" : "FAIL");
            camera.setInvisible(true);
            begin(mc, server);
            return;
        }
        if (shotIndex >= SHOTS.size()) {
            reset();
            com.mojang.logging.LogUtils.getLogger().info("ARSENAL POSE CAPTURE DONE: {} shots in {}", SHOTS.size(), out);
            mc.stop();
            return;
        }
        Shot shot = SHOTS.get(shotIndex);
        WeaponPoses.debugOverride = shot.override;
        shot.setup.run();
        mc.player.setShiftKeyDown(shot.crouch);
        if (++wait < (view == 0 ? 10 : 5)) return;
        place(mc, shot);
        if (wait < (view == 0 ? 13 : 8)) return;
        capture(mc, shot.sheet + "__" + String.format("%02d", shotIndex) + "_" + shot.label.replaceAll("[^A-Za-z0-9%]+", "_")
            + "__" + (shot.firstPerson ? "fp" : view == 0 ? "front" : "side"));
        wait = 0;
        if (!shot.firstPerson && view == 0) { view = 1; return; }
        view = 0;
        shotIndex++;
        if (shotIndex < SHOTS.size()) begin(mc, server);
    }

    private static ItemStack stack(String id) {
        if (id == null) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id)));
    }

    private static void begin(Minecraft mc, net.minecraft.server.level.ServerPlayer server) {
        reset();
        Shot shot = SHOTS.get(shotIndex);
        ItemStack main = stack(shot.main);
        if (shot.enchanted) main.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
        server.setItemSlot(EquipmentSlot.MAINHAND, main);
        server.setItemSlot(EquipmentSlot.OFFHAND, stack(shot.off));
        server.teleportTo(0.5D, -60.0D, 0.5D);
        server.setYRot(0.0F); server.setXRot(0.0F); server.setYHeadRot(0.0F); server.setYBodyRot(0.0F);
    }

    /** Front: the camera faces the player from 3.2 blocks ahead. Side: from its right. First person: its own eyes. */
    private static void place(Minecraft mc, Shot shot) {
        var p = mc.player;
        p.setYRot(0.0F); p.yRotO = 0.0F; p.setXRot(shot.firstPerson ? 10.0F : 0.0F); p.xRotO = p.getXRot();
        p.yBodyRot = p.yBodyRotO = 0.0F; p.yHeadRot = p.yHeadRotO = 0.0F;
        mc.setCameraEntity(p);
        if (shot.firstPerson) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.hideGui = false;
            camPos = null;
            return;
        }
        mc.options.hideGui = true;
        // The local player is drawn only while it is the camera entity, so use a
        // detached third-person camera and move it in the camera event.
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        double x = p.getX(), y = p.getY() + 1.45D, z = p.getZ();
        if (view == 0) { z += 3.2D; camYaw = 180.0F; } else { x -= 3.2D; camYaw = -90.0F; }
        camPos = new net.minecraft.world.phys.Vec3(x, y, z);
    }

    private static net.minecraft.world.phys.Vec3 camPos;
    private static float camYaw;
    private static boolean cameraHooked;

    private static void hookCamera() {
        if (cameraHooked) return;
        cameraHooked = true;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
            (net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles e) -> {
                if (camPos == null || Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
                e.setYaw(camYaw);
                e.setPitch(8.0F);
                try {
                    var set = net.minecraft.client.Camera.class.getDeclaredMethod("setPosition", net.minecraft.world.phys.Vec3.class);
                    set.setAccessible(true);
                    set.invoke(e.getCamera(), camPos);
                } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            });
    }

    private static void capture(Minecraft mc, String name) {
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(new File(out, name + ".png"));
        } catch (Exception error) {
            com.mojang.logging.LogUtils.getLogger().warn("Pose capture failed for {}", name, error);
        }
    }

    /** Another player swinging both claws in one tick: both hands must animate. */
    private static void checkRemoteSwings(Minecraft mc) {
        var other = new net.minecraft.client.player.RemotePlayer(mc.level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "SwingCheck"));
        other.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        other.swing(net.minecraft.world.InteractionHand.OFF_HAND);
        boolean main = AttackTiming.phase(other, 0.0F, net.minecraft.world.InteractionHand.MAIN_HAND) >= 0.0F;
        boolean off = AttackTiming.phase(other, 0.0F, net.minecraft.world.InteractionHand.OFF_HAND) >= 0.0F;
        com.mojang.logging.LogUtils.getLogger().info("ARSENAL REMOTE SWING CHECK: main={} off={} -> {}", main, off,
            main && off ? "PASS" : "FAIL");
    }

    private PoseCapture() {}
}
