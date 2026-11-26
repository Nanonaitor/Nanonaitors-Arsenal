package com.nanonaitor.arsenal.config;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Carries settings from the old config layout (weapons, reach, effects) into
 * the per-weapon sections, once, then removes the old sections from the file.
 * Each value is written both to the live setting and to its new place in the
 * file, so it survives the save.
 */
public final class ArsenalConfigMigration {
    private ArsenalConfigMigration() {}

    /** old section, old key, new section (ArsenalConfig field), new key */
    private static final String[][] MOVES = {
        {"weapons", "morningStar", "morningStar", "enabled"},
        {"weapons", "scimitar", "scimitars", "enabled"},
        {"weapons", "bladeStaff", "bladeStaff", "enabled"},
        {"weapons", "claws", "claws", "enabled"},
        {"weapons", "flail", "flail", "enabled"},
        {"weapons", "batteringRam", "batteringRam", "enabled"},
        {"weapons", "ballAndChain", "ballAndChain", "enabled"},
        {"weapons", "tartsyShield", "tartsyShield", "enabled"},
        {"weapons", "sunWarBulwark", "sunWarBulwark", "enabled"},
        {"reach", "flailRadius", "flail", "radius"},
        {"reach", "ballWindupReach", "ballAndChain", "windupReach"},
        {"reach", "ballThrowReachPerCharge", "ballAndChain", "throwReachPerCharge"},
        {"effects", "scimitarHitExtra", "scimitars", "hitEffects"},
        {"effects", "morningStarFracture", "morningStar", "fractureEffects"},
        {"effects", "morningStarStun", "morningStar", "stunEffects"},
        {"effects", "ballPlayerFracture", "ballAndChain", "playerFracture"},
        {"effects", "bladeStaffReflect", "bladeStaff", "reflectEffects"},
        {"effects", "tartsyDash", "tartsyShield", "dashEffects"},
        {"effects", "venomHit", "materials", "venomHit"},
        {"effects", "icedDragonboneHit", "materials", "icedDragonboneHit"},
        {"effects", "livingMorningStar", "morningStar", "livingEffects"},
        {"effects", "livingClaws", "claws", "livingEffects"},
        {"effects", "livingFlail", "flail", "livingEffects"},
        {"effects", "livingBallAndChain", "ballAndChain", "livingEffects"},
        {"effects", "clawsHit", "claws", "hitEffects"},
        {"effects", "flailHit", "flail", "hitEffects"},
        {"effects", "ballAndChainHit", "ballAndChain", "hitEffects"},
        {"effects", "batteringRamHit", "batteringRam", "hitEffects"},
        {"effects", "morningStarHit", "morningStar", "hitEffects"},
        {"effects", "bladeStaffHit", "bladeStaff", "hitEffects"},
    };

    public static void migrate() {
        Configuration cfg = loadedConfig();
        if (cfg == null) {
            // Not loaded yet: load it now, then look again.
            ConfigManager.sync(NanonaitorsArsenal.MOD_ID, Config.Type.INSTANCE);
            cfg = loadedConfig();
        }
        if (cfg == null) return;
        int moved = 0;
        java.util.Set<ConfigCategory> oldSections = new java.util.LinkedHashSet<>();
        for (String[] move : MOVES) {
            ConfigCategory from = old(cfg, move[0]);
            if (from == null) continue;
            oldSections.add(from);
            Property value = from.get(move[1]);
            if (value == null) continue;
            try {
                if (carry(cfg, value, move[2], move[3])) moved++;
            } catch (ReflectiveOperationException | RuntimeException e) {
                NanonaitorsArsenal.LOGGER.warn("Could not carry over Arsenal setting {}.{}", move[0], move[1], e);
            }
        }
        if (oldSections.isEmpty()) return;
        for (ConfigCategory section : oldSections) cfg.removeCategory(section);
        cfg.save();
        NanonaitorsArsenal.LOGGER.info("Moved {} Arsenal settings into the per-weapon config sections.", moved);
    }

    /** Copies one old value into the live setting and into its new place in the file. */
    private static boolean carry(Configuration cfg, Property value, String sectionName, String key)
        throws ReflectiveOperationException {
        Object section = ArsenalConfig.class.getField(sectionName).get(null);
        Field field = section.getClass().getField(key);
        String category = "general." + sectionName.toLowerCase(Locale.ROOT);
        Property target = cfg.hasCategory(category) ? cfg.getCategory(category).get(key) : null;
        Class<?> type = field.getType();
        if (type == boolean.class) {
            boolean v = value.getBoolean(field.getBoolean(section));
            field.setBoolean(section, v);
            if (target != null) target.set(v);
        } else if (type == double.class) {
            double v = value.getDouble(field.getDouble(section));
            field.setDouble(section, v);
            if (target != null) target.set(v);
        } else if (type == String[].class) {
            String[] v = value.getStringList();
            field.set(section, v);
            if (target != null) target.set(v);
        } else {
            return false;
        }
        return true;
    }

    /** An old section: @Config nests every section under "general". */
    private static ConfigCategory old(Configuration cfg, String name) {
        for (String key : new String[] {"general." + name, name}) {
            if (cfg.hasCategory(key)) return cfg.getCategory(key);
        }
        return null;
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
}
