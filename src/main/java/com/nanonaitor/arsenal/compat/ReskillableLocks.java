package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import java.lang.reflect.Method;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

/**
 * Arsenal's built-in Reskillable level requirements (config: reskillable).
 *
 * Added once every mod has loaded, after Reskillable has read its own config.
 * An item the pack's reskillable.cfg already locks keeps the pack's lock: the
 * pack always wins. Reskillable is optional; without it nothing happens.
 */
public final class ReskillableLocks {
    private ReskillableLocks() {}

    public static void register() {
        if (!ArsenalConfig.reskillable.enabled || !Loader.isModLoaded("reskillable")) return;
        Method fromString, addLock, getSkillLock, isRealLock;
        try {
            Class<?> handler = Class.forName("codersafterdark.reskillable.base.LevelLockHandler");
            Class<?> holder = Class.forName("codersafterdark.reskillable.api.data.RequirementHolder");
            fromString = holder.getMethod("fromString", String.class);
            isRealLock = holder.getMethod("isRealLock");
            addLock = handler.getMethod("addLock", ItemStack.class, holder);
            getSkillLock = handler.getMethod("getSkillLock", ItemStack.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            CompatLog.warnOnce("reskillable-locks", "Reskillable's lock API is not the expected one; Arsenal's level requirements are off.", e);
            return;
        }
        int added = 0, kept = 0, skipped = 0;
        for (String entry : ArsenalConfig.reskillable.requirements) {
            if (entry == null || entry.trim().isEmpty() || entry.trim().startsWith("#")) continue;
            Lock lock;
            try {
                lock = Lock.parse(entry);
            } catch (IllegalArgumentException bad) {
                NanonaitorsArsenal.LOGGER.warn("Skipping Reskillable requirement '{}': {}", entry, bad.getMessage());
                skipped++;
                continue;
            }
            Item item = Item.getByNameOrId(lock.item);
            if (item == null) {
                skipped++; // a disabled family or a custom material that is not configured
                continue;
            }
            ItemStack stack = new ItemStack(item, 1, lock.meta);
            try {
                Object existing = getSkillLock.invoke(null, stack);
                // Unlocked items come back as an empty holder: only a real lock counts.
                if (existing != null && Boolean.TRUE.equals(isRealLock.invoke(existing))) {
                    kept++; // the pack's reskillable.cfg already has this item
                    continue;
                }
                addLock.invoke(null, stack, fromString.invoke(null, lock.requirements));
                added++;
            } catch (ReflectiveOperationException | RuntimeException e) {
                NanonaitorsArsenal.LOGGER.warn("Could not add Reskillable requirement '{}'", entry, e);
                skipped++;
            }
        }
        NanonaitorsArsenal.LOGGER.info("Reskillable requirements: {} added, {} left to the pack's own locks, {} skipped",
            added, kept, skipped);
    }

    /** One requirement line: modid:item[:meta|*]=skill|level[,skill|level...] */
    public static final class Lock {
        public final String item, requirements;
        public final int meta;

        private Lock(String item, int meta, String requirements) {
            this.item = item; this.meta = meta; this.requirements = requirements;
        }

        public static Lock parse(String entry) {
            String line = entry.trim();
            int eq = line.indexOf('=');
            if (eq <= 0 || eq == line.length() - 1) throw new IllegalArgumentException("expected item=skill|level");
            String left = line.substring(0, eq).trim(), right = line.substring(eq + 1).trim();
            String[] parts = left.split(":");
            if (parts.length < 2 || parts.length > 3 || parts[0].isEmpty() || parts[1].isEmpty()) {
                throw new IllegalArgumentException("item must be modid:item or modid:item:meta");
            }
            int meta = 0;
            if (parts.length == 3) {
                if ("*".equals(parts[2])) meta = 32767;
                else {
                    try { meta = Integer.parseInt(parts[2]); }
                    catch (NumberFormatException e) { throw new IllegalArgumentException("meta must be a number or *"); }
                }
            }
            for (String requirement : right.split(",")) {
                String r = requirement.trim();
                if (r.isEmpty() || (!r.contains("|") && !r.contains(":"))) {
                    throw new IllegalArgumentException("requirement '" + r + "' should look like reskillable:attack|12");
                }
            }
            return new Lock(parts[0] + ":" + parts[1], meta, right);
        }
    }
}
