package com.nanonaitor.arsenal.config;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Carries settings from the old config layout (weapons, reach, effects) into
 * the per-weapon sections, once, then removes the old sections from the file.
 */
public final class ArsenalConfigMigration {
    private ArsenalConfigMigration() {}

    public static void migrate() {
        Configuration cfg = loadedConfig();
        if (cfg == null) {
            // Not loaded yet: load it now, then look again.
            ConfigManager.sync(NanonaitorsArsenal.MOD_ID, Config.Type.INSTANCE);
            cfg = loadedConfig();
        }
        if (cfg == null) return;
        boolean changed = false;
        if (cfg.hasCategory("weapons")) {
            ConfigCategory c = cfg.getCategory("weapons");
            ArsenalConfig.morningStar.enabled = bool(c, "morningStar", ArsenalConfig.morningStar.enabled);
            ArsenalConfig.scimitars.enabled = bool(c, "scimitar", ArsenalConfig.scimitars.enabled);
            ArsenalConfig.bladeStaff.enabled = bool(c, "bladeStaff", ArsenalConfig.bladeStaff.enabled);
            ArsenalConfig.claws.enabled = bool(c, "claws", ArsenalConfig.claws.enabled);
            ArsenalConfig.flail.enabled = bool(c, "flail", ArsenalConfig.flail.enabled);
            ArsenalConfig.batteringRam.enabled = bool(c, "batteringRam", ArsenalConfig.batteringRam.enabled);
            ArsenalConfig.ballAndChain.enabled = bool(c, "ballAndChain", ArsenalConfig.ballAndChain.enabled);
            ArsenalConfig.tartsyShield.enabled = bool(c, "tartsyShield", ArsenalConfig.tartsyShield.enabled);
            ArsenalConfig.sunWarBulwark.enabled = bool(c, "sunWarBulwark", ArsenalConfig.sunWarBulwark.enabled);
            cfg.removeCategory(c);
            changed = true;
        }
        if (cfg.hasCategory("reach")) {
            ConfigCategory c = cfg.getCategory("reach");
            ArsenalConfig.flail.radius = number(c, "flailRadius", ArsenalConfig.flail.radius);
            ArsenalConfig.ballAndChain.windupReach = number(c, "ballWindupReach", ArsenalConfig.ballAndChain.windupReach);
            ArsenalConfig.ballAndChain.throwReachPerCharge = number(c, "ballThrowReachPerCharge",
                ArsenalConfig.ballAndChain.throwReachPerCharge);
            cfg.removeCategory(c);
            changed = true;
        }
        if (cfg.hasCategory("effects")) {
            ConfigCategory c = cfg.getCategory("effects");
            ArsenalConfig.scimitars.hitEffects = list(c, "scimitarHitExtra", ArsenalConfig.scimitars.hitEffects);
            ArsenalConfig.morningStar.fractureEffects = list(c, "morningStarFracture", ArsenalConfig.morningStar.fractureEffects);
            ArsenalConfig.morningStar.stunEffects = list(c, "morningStarStun", ArsenalConfig.morningStar.stunEffects);
            ArsenalConfig.ballAndChain.playerFracture = list(c, "ballPlayerFracture", ArsenalConfig.ballAndChain.playerFracture);
            ArsenalConfig.bladeStaff.reflectEffects = list(c, "bladeStaffReflect", ArsenalConfig.bladeStaff.reflectEffects);
            ArsenalConfig.tartsyShield.dashEffects = list(c, "tartsyDash", ArsenalConfig.tartsyShield.dashEffects);
            ArsenalConfig.materials.venomHit = list(c, "venomHit", ArsenalConfig.materials.venomHit);
            ArsenalConfig.materials.icedDragonboneHit = list(c, "icedDragonboneHit", ArsenalConfig.materials.icedDragonboneHit);
            ArsenalConfig.morningStar.livingEffects = list(c, "livingMorningStar", ArsenalConfig.morningStar.livingEffects);
            ArsenalConfig.claws.livingEffects = list(c, "livingClaws", ArsenalConfig.claws.livingEffects);
            ArsenalConfig.flail.livingEffects = list(c, "livingFlail", ArsenalConfig.flail.livingEffects);
            ArsenalConfig.ballAndChain.livingEffects = list(c, "livingBallAndChain", ArsenalConfig.ballAndChain.livingEffects);
            ArsenalConfig.claws.hitEffects = list(c, "clawsHit", ArsenalConfig.claws.hitEffects);
            ArsenalConfig.flail.hitEffects = list(c, "flailHit", ArsenalConfig.flail.hitEffects);
            ArsenalConfig.ballAndChain.hitEffects = list(c, "ballAndChainHit", ArsenalConfig.ballAndChain.hitEffects);
            ArsenalConfig.batteringRam.hitEffects = list(c, "batteringRamHit", ArsenalConfig.batteringRam.hitEffects);
            ArsenalConfig.morningStar.hitEffects = list(c, "morningStarHit", ArsenalConfig.morningStar.hitEffects);
            ArsenalConfig.bladeStaff.hitEffects = list(c, "bladeStaffHit", ArsenalConfig.bladeStaff.hitEffects);
            cfg.removeCategory(c);
            changed = true;
        }
        if (changed) {
            // Writes the carried-over values into the new sections and saves.
            ConfigManager.sync(NanonaitorsArsenal.MOD_ID, Config.Type.INSTANCE);
            NanonaitorsArsenal.LOGGER.info("Moved Arsenal settings into the per-weapon config sections.");
        }
    }

    /** The Configuration Forge already loaded for Arsenal's @Config class, or null. */
    @SuppressWarnings("unchecked")
    private static Configuration loadedConfig() {
        try {
            Field configs = ConfigManager.class.getDeclaredField("CONFIGS");
            configs.setAccessible(true);
            for (Map.Entry<String, Configuration> entry : ((Map<String, Configuration>) configs.get(null)).entrySet()) {
                String path = entry.getKey().replace('\\', '/');
                if (path.endsWith("/nanonaitors_arsenal.cfg")) return entry.getValue();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            NanonaitorsArsenal.LOGGER.warn("Could not check for old Arsenal config settings to carry over", e);
        }
        return null;
    }

    private static boolean bool(ConfigCategory c, String key, boolean fallback) {
        Property p = c.get(key);
        return p == null ? fallback : p.getBoolean(fallback);
    }

    private static double number(ConfigCategory c, String key, double fallback) {
        Property p = c.get(key);
        return p == null ? fallback : p.getDouble(fallback);
    }

    private static String[] list(ConfigCategory c, String key, String[] fallback) {
        Property p = c.get(key);
        return p == null ? fallback : p.getStringList();
    }
}
