package com.nanonaitor.arsenal.dev;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.client.pose.WeaponPoses;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;

/**
 * DEVELOPMENT ONLY. Not included in release jars (see build.gradle).
 *
 * When config/nanonaitors_arsenal_pose_capture.txt exists, this opens a flat
 * creative test world, poses the player with each Arsenal weapon in each
 * animation state, photographs a front and a side view, writes one labelled
 * contact sheet per weapon to arsenal-pose-capture/, then closes the game.
 */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class PoseCapture {
    private static final String MARKER = "config/nanonaitors_arsenal_pose_capture.txt";
    private static final String WORLD = "arsenal_pose_capture";
    private static final float[] VIEWS = {25.0F, 90.0F};
    private static final String[] VIEW_NAMES = {"front", "side"};
    private static final int CELL_W = 340, CELL_H = 370, LABEL_H = 22;

    private static Boolean active;
    private static boolean launched;
    private static int worldTicks;
    private static int shotIndex = -1;
    private static int view;
    private static int waitTicks;
    private static boolean captureNow;
    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final Map<String, List<BufferedImage[]>> SHEETS = new LinkedHashMap<>();
    private static final Map<String, List<String>> LABELS = new LinkedHashMap<>();
    private static BufferedImage[] currentRow;

    private PoseCapture() {}

    private static final class Shot {
        final String sheet, label, main, off;
        final Consumer<WeaponPoses.Input> override;
        /** Hand to actually raise (so blocking item models are used), or null. */
        net.minecraft.util.EnumHand raise;
        /** Exact arm angles to force (calibration shots), or null. */
        float[] angles;
        /** Puts the player in a real attack state every tick (wind-up, throw, orbit), or null. */
        Consumer<EntityPlayer> live;
        /** Shoot from the first-person view instead of the orbiting third-person one. */
        boolean firstPerson;
        Shot(String sheet, String label, String main, String off, Consumer<WeaponPoses.Input> override) {
            this.sheet = sheet; this.label = label; this.main = main; this.off = off; this.override = override;
        }
    }

    private static Shot add(String sheet, String label, String main, String off,
                            Consumer<WeaponPoses.Input> override) {
        Shot shot = new Shot(sheet, label, main, off, override);
        SHOTS.add(shot);
        return shot;
    }

    private static void calibration(String label, float... angles) {
        add("calibration", label, "minecraft:stick", "minecraft:stick", in -> {}).angles = angles;
    }

    private static void buildShots() {
        // Known vanilla arm angles, to line the Mo' Bends path up with vanilla.
        calibration("both forward (X -1.57)", -1.57F, 0, 0, -1.57F, 0, 0);
        calibration("forward, turned inward (Y -/+0.6)", -1.57F, -0.6F, 0, -1.57F, 0.6F, 0);
        calibration("raised sideways (Z +/-0.6)", 0, 0, 0.6F, 0, 0, -0.6F);
        calibration("forward-down inward (-0.8, -/+0.5)", -0.8F, -0.5F, 0, -0.8F, 0.5F, 0);
        calibration("all three axes", -1.0F, -0.4F, -0.5F, -1.3F, 0.9F, 0.4F);
        add("calibration", "staff, three axes", NanonaitorsArsenal.MOD_ID + ":blade_staff_iron", null,
            in -> {}).angles = new float[] {-0.95F, -0.30F, -0.30F, -1.3F, 0.9F, 0.0F};
        String torch = "minecraft:torch";
        String ram = NanonaitorsArsenal.MOD_ID + ":battering_ram_iron";
        add("battering_ram", "idle, two hands", ram, null, in -> in.active = false);
        add("battering_ram", "ramming", ram, null, in -> in.active = true);
        add("battering_ram", "attack lunge 25%", ram, null, in -> { in.active = false; in.attackPhase = 0.25F; });
        add("battering_ram", "one hand, at the hip", ram, torch, in -> in.attackPhase = -1);
        add("battering_ram", "one hand lunge 25%", ram, torch, in -> in.attackPhase = 0.25F);

        String staff = NanonaitorsArsenal.MOD_ID + ":blade_staff_iron";
        add("blade_staff", "idle, two hands", staff, null, in -> { in.active = false; in.attackPhase = -1; });
        for (int side = 0; side < 2; side++) {
            final int count = side;
            for (float phase : new float[] {0.25F, 0.5F, 0.75F}) {
                add("blade_staff", "paddle " + (side == 0 ? "right " : "left ") + Math.round(phase * 100) + "%",
                    staff, null, in -> { in.active = false; in.attackPhase = phase; in.attackCount = count; });
            }
        }
        add("blade_staff", "reflecting", staff, null, in -> in.active = true);
        add("blade_staff", "one hand idle", staff, torch, in -> { in.active = false; in.attackPhase = -1; });
        for (float phase : new float[] {0.2F, 0.45F, 0.7F}) {
            add("blade_staff", "one hand strike " + Math.round(phase * 100) + "%", staff, torch,
                in -> { in.active = false; in.attackPhase = phase; in.attackCount = 0; });
        }

        String ball = NanonaitorsArsenal.MOD_ID + ":ball_and_chain_iron";
        add("ball_and_chain", "idle", ball, null, in -> { in.spinAngle = Double.NaN; in.throwProgress = -1; in.active = false; });
        // Real attack states, so the drawn ball and chain show with the pose.
        for (int q = 0; q < 4; q++) {
            final double angle = q * Math.PI / 2.0D;
            add("ball_and_chain", "wind-up ~" + (q * 90) + " deg", ball, null, in -> in.active = false)
                .live = p -> com.nanonaitor.arsenal.client.BallAndChainAnimationHandler.debugWindup(p, angle);
        }
        for (double t : new double[] {0.05D, 0.3D, 0.58D, 0.75D}) {
            add("ball_and_chain", "throw ~" + Math.round(t * 100) + "%", ball, null, in -> in.active = false)
                .live = p -> com.nanonaitor.arsenal.client.BallAndChainAnimationHandler.debugRelease(p, t);
        }
        Shot guard = add("ball_and_chain", "guard", ball, null, in -> {});
        guard.raise = net.minecraft.util.EnumHand.MAIN_HAND;
        guard.live = p -> com.nanonaitor.arsenal.client.BallAndChainInputHandler.debugGuard = true;
        for (String tier : new String[] {"gold", "sentient"}) {
            add("ball_and_chain", tier + " wind-up ~0 deg", NanonaitorsArsenal.MOD_ID + ":ball_and_chain_" + tier, null,
                in -> in.active = false)
                .live = p -> com.nanonaitor.arsenal.client.BallAndChainAnimationHandler.debugWindup(p, 0.0D);
        }
        add("ball_and_chain", "one hand wind-up ~90 deg", ball, torch, in -> in.active = false)
            .live = p -> com.nanonaitor.arsenal.client.BallAndChainAnimationHandler.debugWindup(p, Math.PI / 2.0D);

        String flail = NanonaitorsArsenal.MOD_ID + ":flail_iron";
        add("flail", "idle", flail, null, in -> in.spinAngle = Double.NaN);
        for (int q = 0; q < 4; q++) {
            final double angle = q * Math.PI / 2.0D;
            add("flail", "spin ~" + (q * 90) + " deg", flail, null, in -> {})
                .live = p -> com.nanonaitor.arsenal.client.FlailAnimationHandler.debugOrbit(p, angle);
        }

        String bulwark = NanonaitorsArsenal.MOD_ID + ":sun_war_bulwark";
        add("sun_war_bulwark", "carry, two hands", bulwark, null, in -> in.active = false);
        add("sun_war_bulwark", "guard, two hands", bulwark, null, in -> in.active = true)
            .raise = net.minecraft.util.EnumHand.MAIN_HAND;
        add("sun_war_bulwark", "one hand (struggle)", bulwark, torch, in -> in.active = false);
        add("sun_war_bulwark", "in offhand, guard", null, bulwark, in -> in.active = true)
            .raise = net.minecraft.util.EnumHand.OFF_HAND;

        String scimitar = NanonaitorsArsenal.MOD_ID + ":scimitar_iron";
        add("scimitars", "guard", scimitar, scimitar, in -> in.attackPhase = -1)
            .raise = net.minecraft.util.EnumHand.MAIN_HAND;
        for (float phase : new float[] {0.15F, 0.4F, 0.6F, 0.85F}) {
            add("scimitars", "bash " + Math.round(phase * 100) + "%", scimitar, scimitar, in -> in.attackPhase = phase)
                .raise = net.minecraft.util.EnumHand.MAIN_HAND;
        }

        for (float phase : new float[] {0.3F, 0.55F}) {
            Shot first = add("scimitars", "first person bash " + Math.round(phase * 100) + "%", scimitar, scimitar,
                in -> in.attackPhase = phase);
            first.raise = net.minecraft.util.EnumHand.MAIN_HAND;
            first.firstPerson = true;
            first.live = p -> com.nanonaitor.arsenal.client.pose.AttackTiming.debugPhase = phase;
        }

        String claws = NanonaitorsArsenal.MOD_ID + ":claws_iron";
        String linked = NanonaitorsArsenal.MOD_ID + ":linked_claw_iron";
        add("claws", "ready", claws, linked, in -> in.attackPhase = -1);
        for (float phase : new float[] {0.12F, 0.3F, 0.45F}) {
            add("claws", "right rake " + Math.round(phase * 100) + "%", claws, linked,
                in -> { in.attackPhase = phase; in.attackCount = 1; in.offAttackPhase = -1; })
                .live = p -> com.nanonaitor.arsenal.client.pose.AttackTiming.debugPhase = phase;
        }
        add("claws", "both hands 35%", claws, linked,
            in -> { in.attackPhase = 0.35F; in.attackCount = 1; in.offAttackPhase = 0.35F; in.offAttackCount = 1; })
            .live = p -> com.nanonaitor.arsenal.client.pose.AttackTiming.debugPhase = 0.35F;
        add("claws", "third-hit lunge 40%", claws, linked,
            in -> { in.attackPhase = 0.4F; in.attackCount = 3; in.offAttackPhase = -1; })
            .live = p -> com.nanonaitor.arsenal.client.pose.AttackTiming.debugPhase = 0.4F;

        String morning = NanonaitorsArsenal.MOD_ID + ":morning_star_iron";
        add("morning_star", "charging", morning, null, in -> { in.active = true; in.attackPhase = -1; });
        add("morning_star", "smash 15%", morning, null, in -> { in.active = false; in.attackPhase = 0.15F; });
        add("morning_star", "smash 40%", morning, null, in -> { in.active = false; in.attackPhase = 0.40F; });
        for (float charge : new float[] {0.0F, 0.5F, 1.0F}) {
            Shot first = add("morning_star", "first person charge " + Math.round(charge * 100) + "%", morning, null,
                in -> in.active = true);
            first.firstPerson = true;
            first.live = p -> com.nanonaitor.arsenal.client.ModernWeaponInputHandler.debugMorningCharge = charge;
        }
    }

    private static boolean active() {
        if (active == null) {
            active = new File(Minecraft.getMinecraft().mcDataDir, MARKER).isFile();
            if (active) {
                buildShots();
                NanonaitorsArsenal.LOGGER.info("Arsenal pose capture: {} shots queued", SHOTS.size());
            }
        }
        return active;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!launched && mc.currentScreen instanceof GuiMainMenu) {
            launched = true;
            WorldSettings settings = new WorldSettings(4242L, GameType.CREATIVE, false, false, WorldType.FLAT);
            settings.enableCommands();
            mc.launchIntegratedServer(WORLD, WORLD, settings);
            return;
        }
        if (mc.player == null || mc.world == null || mc.getIntegratedServer() == null) return;
        // First-person shots need the GUI pass on: it also draws the hands.
        mc.gameSettings.hideGUI = !(shotIndex >= 0 && shotIndex < SHOTS.size() && SHOTS.get(shotIndex).firstPerson);
        mc.gameSettings.thirdPersonView = shotIndex >= 0 && shotIndex < SHOTS.size()
            && SHOTS.get(shotIndex).firstPerson ? 0 : 2;
        mc.gameSettings.fovSetting = 50.0F;
        mc.player.rotationYaw = mc.player.prevRotationYaw = 180.0F;
        mc.player.rotationPitch = mc.player.prevRotationPitch = 0.0F;
        if (++worldTicks == 40) prepareWorld(mc);
        if (shotIndex >= 0 && shotIndex < SHOTS.size()) {
            // Keep a raised shield up: the game lowers it whenever the use key is not held.
            net.minecraft.util.EnumHand raise = SHOTS.get(shotIndex).raise;
            if (raise != null && !mc.player.isHandActive()) mc.player.setActiveHand(raise);
            if (raise == null && mc.player.isHandActive()) mc.player.resetActiveHand();
            Consumer<EntityPlayer> live = SHOTS.get(shotIndex).live;
            if (live != null) live.accept(mc.player);
        }
        if (worldTicks < 80 || captureNow) return;
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (shotIndex < 0 || view >= VIEWS.length) {
            if (shotIndex >= 0) finishRow();
            shotIndex++;
            view = 0;
            if (shotIndex >= SHOTS.size()) {
                writeSheets(mc);
                WeaponPoses.debugOverride = null;
                WeaponPoses.debugAngles = null;
                active = false;
                mc.shutdown();
                return;
            }
            Shot shot = SHOTS.get(shotIndex);
            equip(mc, shot);
            WeaponPoses.debugOverride = shot.override;
            WeaponPoses.debugAngles = shot.angles;
            currentRow = new BufferedImage[VIEWS.length];
            waitTicks = 8;
            return;
        }
        captureNow = true;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !captureNow || active == null || !active) return;
        Minecraft mc = Minecraft.getMinecraft();
        BufferedImage screen = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        currentRow[view] = SHOTS.get(shotIndex).firstPerson ? fit(screen) : crop(screen);
        captureNow = false;
        view++;
        waitTicks = 3;
    }

    /**
     * Orbits the third-person camera, not the body, so each view sees the player
     * from a fixed angle and effects drawn from the facing (chains) stay in place.
     */
    @SubscribeEvent
    public static void orbitCamera(net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup event) {
        if (active == null || !active || shotIndex < 0 || shotIndex >= SHOTS.size()
            || SHOTS.get(shotIndex).firstPerson) return;
        event.setYaw(event.getYaw() - VIEWS[Math.min(view, VIEWS.length - 1)]);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void faceView(RenderPlayerEvent.Pre event) {
        if (active == null || !active || shotIndex < 0 || shotIndex >= SHOTS.size()) return;
        EntityPlayer player = event.getEntityPlayer();
        if (player != Minecraft.getMinecraft().player) return;
        float yaw = player.rotationYaw;
        player.renderYawOffset = player.prevRenderYawOffset = yaw;
        player.rotationYawHead = player.prevRotationYawHead = yaw;
        player.limbSwing = player.limbSwingAmount = player.prevLimbSwingAmount = 0.0F;
    }

    private static void prepareWorld(Minecraft mc) {
        MinecraftServer server = mc.getIntegratedServer();
        java.util.UUID id = mc.player.getUniqueID();
        server.addScheduledTask(() -> {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
            if (player == null) return;
            player.world.setWorldTime(6000L);
            player.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            player.world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            player.world.getWorldInfo().setRaining(false);
            player.world.getWorldInfo().setThundering(false);
            standOnGround(player);
        });
    }

    /** The default flat world's surface is y = 4; the spawn chunk may not report a height yet. */
    private static void standOnGround(EntityPlayerMP player) {
        player.capabilities.isFlying = false;
        player.fallDistance = 0.0F;
        player.connection.setPlayerLocation(0.5D, 4.0D, 0.5D, 180.0F, 0.0F);
    }

    private static void equip(Minecraft mc, Shot shot) {
        com.nanonaitor.arsenal.client.BallAndChainInputHandler.debugGuard = false;
        com.nanonaitor.arsenal.client.pose.AttackTiming.debugPhase = -1.0F;
        com.nanonaitor.arsenal.client.ModernWeaponInputHandler.debugMorningCharge = -1.0F;
        ItemStack main = stack(shot.main), off = stack(shot.off);
        MinecraftServer server = mc.getIntegratedServer();
        java.util.UUID id = mc.player.getUniqueID();
        server.addScheduledTask(() -> {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
            if (player == null) return;
            standOnGround(player);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, main.copy());
            player.inventory.offHandInventory.set(0, off.copy());
            player.inventoryContainer.detectAndSendChanges();
        });
        mc.player.inventory.setInventorySlotContents(mc.player.inventory.currentItem, main.copy());
        mc.player.inventory.offHandInventory.set(0, off.copy());
    }

    private static ItemStack stack(String id) {
        if (id == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** The whole screen, scaled to fit a cell (first-person shots). */
    private static BufferedImage fit(BufferedImage screen) {
        double k = Math.min(CELL_W / (double) screen.getWidth(), CELL_H / (double) screen.getHeight());
        int w = (int) (screen.getWidth() * k), h = (int) (screen.getHeight() * k);
        BufferedImage out = new BufferedImage(CELL_W, CELL_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(screen, (CELL_W - w) / 2, (CELL_H - h) / 2, w, h, null);
        g.dispose();
        return out;
    }

    private static BufferedImage crop(BufferedImage screen) {
        // Layout is tuned for a 480-pixel-tall window; scale for larger windows.
        double s = screen.getHeight() / 480.0D;
        int cx = screen.getWidth() / 2, cy = screen.getHeight() / 2;
        int cw = (int) (CELL_W * s), ch = (int) (CELL_H * s);
        int x = Math.max(0, cx - cw / 2), y = Math.max(0, cy - (int) (150 * s));
        int w = Math.min(cw, screen.getWidth() - x), h = Math.min(ch, screen.getHeight() - y);
        BufferedImage out = new BufferedImage(CELL_W, CELL_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(screen.getSubimage(x, y, w, h), 0, 0, (int) (w / s), (int) (h / s), null);
        g.dispose();
        return out;
    }

    private static void finishRow() {
        Shot shot = SHOTS.get(shotIndex);
        SHEETS.computeIfAbsent(shot.sheet, k -> new ArrayList<>()).add(currentRow);
        LABELS.computeIfAbsent(shot.sheet, k -> new ArrayList<>()).add(shot.label);
    }

    private static void writeSheets(Minecraft mc) {
        File dir = new File(mc.mcDataDir, "arsenal-pose-capture");
        dir.mkdirs();
        String mode = net.minecraftforge.fml.common.Loader.isModLoaded("mobends") ? "mobends" : "vanilla";
        for (Map.Entry<String, List<BufferedImage[]>> entry : SHEETS.entrySet()) {
            List<BufferedImage[]> rows = entry.getValue();
            List<String> labels = LABELS.get(entry.getKey());
            int width = CELL_W * VIEWS.length;
            int rowH = CELL_H + LABEL_H;
            BufferedImage sheet = new BufferedImage(width, rows.size() * rowH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = sheet.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            for (int r = 0; r < rows.size(); r++) {
                int top = r * rowH;
                g.setColor(new Color(24, 24, 30));
                g.fillRect(0, top, width, LABEL_H);
                g.setColor(Color.WHITE);
                for (int v = 0; v < VIEWS.length; v++) {
                    g.drawString(labels.get(r) + " - " + VIEW_NAMES[v], v * CELL_W + 6, top + 16);
                    if (rows.get(r)[v] != null) g.drawImage(rows.get(r)[v], v * CELL_W, top + LABEL_H, null);
                }
                g.setColor(new Color(24, 24, 30));
                g.drawLine(CELL_W, top, CELL_W, top + rowH);
            }
            g.dispose();
            try {
                ImageIO.write(sheet, "png", new File(dir, mode + "_" + entry.getKey() + ".png"));
            } catch (java.io.IOException exception) {
                NanonaitorsArsenal.LOGGER.error("Arsenal pose capture: could not write sheet", exception);
            }
        }
        NanonaitorsArsenal.LOGGER.info("Arsenal pose capture done: {}", dir.getAbsolutePath());
    }
}
