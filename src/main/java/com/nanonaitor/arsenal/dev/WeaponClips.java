package com.nanonaitor.arsenal.dev;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.client.BallAndChainInputHandler;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Dev-only clip recorder (same marker file as {@link PoseCapture}, with the word
 * "clips" in it). For each weapon: a still husk stands in front of the player,
 * the weapon's real controls are driven to hit it twice, and every other tick
 * is saved as a frame to {@code <gameDir>/arsenal-clips/<weapon>/}. A script
 * turns the frames into GIFs. Never included in release jars.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class WeaponClips {
    private static final String WORLD = "arsenal_weapon_clips";
    private static final int FRAME_W = 480;
    private static Boolean active;
    private static boolean launched;
    private static int worldTicks;
    private static int clipIndex = -1;
    private static int clipTick;
    private static boolean captureFrame;
    private static int frameNumber;
    private static File clipDir;

    private interface Script { void tick(int t, Minecraft mc, EntityPlayer player); }

    private static final class Clip {
        final String name, main, off;
        final int length;
        final Script script;
        Clip(String name, String main, String off, int length, Script script) {
            this.name = name; this.main = main; this.off = off; this.length = length; this.script = script;
        }
    }

    private static final List<Clip> CLIPS = new ArrayList<>();

    private WeaponClips() {}

    static boolean requested(File marker) {
        try {
            return new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).contains("clips");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean active() {
        if (active == null) {
            File marker = new File(Minecraft.getMinecraft().mcDataDir, "config/nanonaitors_arsenal_pose_capture.txt");
            active = marker.isFile() && requested(marker);
            if (active) build();
        }
        return active;
    }

    // --- input helpers -------------------------------------------------------
    private static void attackKey(Minecraft mc, boolean down) {
        int key = mc.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.setKeyBindState(key, down);
        if (down) KeyBinding.onTick(key);
    }

    private static void useKey(Minecraft mc, boolean down) {
        int key = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(key, down);
        if (down) KeyBinding.onTick(key);
    }

    /** A single click on tick t (pressed for one tick). */
    private static void click(Minecraft mc, int t, int at) {
        if (t == at) attackKey(mc, true);
        if (t == at + 1) attackKey(mc, false);
    }

    /** Attack held between two ticks (both the key binding and the raw-button override). */
    private static void hold(Minecraft mc, int t, int from, int to) {
        boolean down = t >= from && t < to;
        BallAndChainInputHandler.debugAttackHeld = down;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), down);
    }

    private static void build() {
        String id = NanonaitorsArsenal.MOD_ID + ":";
        // Warm-up: lets the world and the first husk settle before anything is kept.
        CLIPS.add(new Clip("_warmup", id + "blade_staff_iron", null, 30, (t, mc, p) -> {}));
        CLIPS.add(new Clip("blade_staff", id + "blade_staff_iron", null, 60,
            (t, mc, p) -> { click(mc, t, 10); click(mc, t, 36); }));
        CLIPS.add(new Clip("scimitars", id + "scimitar_iron", id + "scimitar_iron", 50,
            (t, mc, p) -> hold(mc, t, 8, 30)));
        CLIPS.add(new Clip("claws", id + "claws_iron", id + "linked_claw_iron", 55,
            (t, mc, p) -> hold(mc, t, 8, 32)));
        CLIPS.add(new Clip("morning_star", id + "morning_star_iron", null, 90,
            (t, mc, p) -> { hold(mc, t, 6, 30); if (t >= 30) hold(mc, t, 48, 72); }));
        CLIPS.add(new Clip("flail", id + "flail_iron", null, 70,
            (t, mc, p) -> hold(mc, t, 6, 56)));
        CLIPS.add(new Clip("ball_and_chain", id + "ball_and_chain_iron", null, 110,
            (t, mc, p) -> { hold(mc, t, 6, 22); if (t >= 22) hold(mc, t, 58, 74); }));
        CLIPS.add(new Clip("battering_ram", id + "battering_ram_iron", null, 84,
            (t, mc, p) -> {
                useKey(mc, (t >= 8 && t < 20) || (t >= 46 && t < 58));
                if (t == 32) backToStart(mc); // walk back for the second charge
            }));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!launched && mc.currentScreen instanceof GuiMainMenu) {
            launched = true;
            WorldSettings settings = new WorldSettings(4243L, GameType.SURVIVAL, false, false, WorldType.FLAT);
            settings.enableCommands();
            mc.launchIntegratedServer(WORLD, WORLD, settings);
            return;
        }
        if (mc.player == null || mc.world == null || mc.getIntegratedServer() == null) return;
        mc.gameSettings.hideGUI = true;
        mc.gameSettings.thirdPersonView = 2;
        mc.gameSettings.fovSetting = 60.0F;
        mc.player.capabilities.disableDamage = true;
        if (++worldTicks == 40) prepareWorld(mc);
        if (worldTicks < 80) return;
        if (clipIndex < 0 || clipTick >= CLIPS.get(clipIndex).length) {
            if (clipIndex >= 0) endClip(mc);
            clipIndex++;
            if (clipIndex >= CLIPS.size()) {
                active = false;
                mc.shutdown();
                return;
            }
            startClip(mc, CLIPS.get(clipIndex));
            return;
        }
        Clip clip = CLIPS.get(clipIndex);
        // Face the husk; the ram locks its own facing while charging.
        mc.player.rotationYaw = mc.player.prevRotationYaw = 180.0F;
        mc.player.rotationPitch = mc.player.prevRotationPitch = 8.0F;
        clip.script.tick(clipTick, mc, mc.player);
        if (clipTick >= 4 && clipTick % 2 == 0) captureFrame = true;
        clipTick++;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !captureFrame || active == null || !active) return;
        captureFrame = false;
        Minecraft mc = Minecraft.getMinecraft();
        BufferedImage screen = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        int h = (int) Math.round(screen.getHeight() * (FRAME_W / (double) screen.getWidth()));
        BufferedImage out = new BufferedImage(FRAME_W, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(screen, 0, 0, FRAME_W, h, null);
        g.dispose();
        try {
            ImageIO.write(out, "png", new File(clipDir, String.format("frame_%03d.png", frameNumber++)));
        } catch (Exception e) {
            NanonaitorsArsenal.LOGGER.warn("Clip frame failed", e);
        }
    }

    /** Side-on view: the player on one side of the frame, the husk on the other. */
    @SubscribeEvent
    public static void orbitCamera(EntityViewRenderEvent.CameraSetup event) {
        if (active == null || !active || clipIndex < 0) return;
        event.setYaw(event.getYaw() - 90.0F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void steadyBody(RenderPlayerEvent.Pre event) {
        if (active == null || !active || event.getEntityPlayer() != Minecraft.getMinecraft().player) return;
        EntityPlayer player = event.getEntityPlayer();
        player.renderYawOffset = player.prevRenderYawOffset = player.rotationYaw;
        player.rotationYawHead = player.prevRotationYawHead = player.rotationYaw;
    }

    private static void backToStart(Minecraft mc) {
        MinecraftServer server = mc.getIntegratedServer();
        java.util.UUID id = mc.player.getUniqueID();
        server.addScheduledTask(() -> {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
            if (player != null) player.connection.setPlayerLocation(0.5D, 4.0D, 0.5D, 180.0F, 8.0F);
        });
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
            player.getFoodStats().setFoodLevel(20);
        });
    }

    private static void startClip(Minecraft mc, Clip clip) {
        clipTick = 0;
        frameNumber = 0;
        clipDir = new File(mc.mcDataDir, "arsenal-clips/" + clip.name);
        clipDir.mkdirs();
        for (File old : clipDir.listFiles()) old.delete();
        ItemStack main = stack(clip.main), off = stack(clip.off);
        MinecraftServer server = mc.getIntegratedServer();
        java.util.UUID id = mc.player.getUniqueID();
        server.addScheduledTask(() -> {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(id);
            if (player == null) return;
            WorldServer world = player.getServerWorld();
            for (EntityHusk husk : world.getEntities(EntityHusk.class, e -> true)) husk.setDead();
            player.connection.setPlayerLocation(0.5D, 4.0D, 0.5D, 180.0F, 8.0F);
            player.getFoodStats().setFoodLevel(20);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, main.copy());
            player.inventory.offHandInventory.set(0, off.copy());
            player.inventoryContainer.detectAndSendChanges();
            player.getCooldownTracker().removeCooldown(main.getItem());
            // A still, sturdy husk 2.6 blocks ahead, facing the player.
            EntityHusk husk = new EntityHusk(world);
            husk.setLocationAndAngles(0.5D, 4.0D, -2.1D, 0.0F, 0.0F);
            husk.setNoAI(true);
            husk.enablePersistence();
            husk.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(400.0D);
            husk.setHealth(400.0F);
            // Stays put so both hits land (the ram would otherwise send it out of frame).
            husk.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
            ((EntityLiving) husk).rotationYawHead = 0.0F;
            world.spawnEntity(husk);
        });
        mc.player.inventory.setInventorySlotContents(mc.player.inventory.currentItem, main.copy());
        mc.player.inventory.offHandInventory.set(0, off.copy());
    }

    private static void endClip(Minecraft mc) {
        attackKey(mc, false);
        useKey(mc, false);
        BallAndChainInputHandler.debugAttackHeld = false;
        NanonaitorsArsenal.LOGGER.info("Clip {} recorded: {} frames", CLIPS.get(clipIndex).name, frameNumber);
    }

    private static ItemStack stack(String id) {
        if (id == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
