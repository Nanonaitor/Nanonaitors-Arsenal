package com.nanonaitor.arsenal.config;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class ConfiguredEffects {
    private static final Set<String> WARNED = new HashSet<>();
    /** Parsed specs keyed by entry and defaults; config edits produce new keys. */
    private static final java.util.Map<String, EffectSpec> PARSED =
        new java.util.concurrent.ConcurrentHashMap<>();
    private ConfiguredEffects() {}
    public static boolean stunImmune(EntityLivingBase target) {
        ResourceLocation id = EntityList.getKey(target);
        if (id == null) return false;
        for (String entry : ArsenalConfig.stunned.entityBlacklist)
            if (id.toString().equals(entry.trim())) return true;
        return false;
    }
    public static void apply(EntityLivingBase target, String[] entries, int defaultTicks, int defaultAmplifier) {
        if (target.world.isRemote) return;
        for (String entry : entries) {
            if (entry == null || entry.trim().isEmpty()) continue;
            try {
                EffectSpec spec = PARSED.computeIfAbsent(
                    entry + '|' + defaultTicks + '|' + defaultAmplifier,
                    key -> EffectSpec.parse(entry, defaultTicks, defaultAmplifier));
                Potion potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(spec.id));
                if (potion == null) { warn(entry); continue; }
                target.addPotionEffect(new PotionEffect(potion, spec.ticks, spec.amplifier, false, true));
            } catch (RuntimeException exception) { warn(entry); }
        }
    }

    /** Returns localized effect names so tooltips follow the live config instead of hardcoded defaults. */
    public static String displayNames(String[] entries) {
        Set<String> names = new LinkedHashSet<>();
        if (entries == null) return "";
        for (String entry : entries) {
            if (entry == null || entry.trim().isEmpty()) continue;
            String id = entry.trim().split("@", 2)[0];
            try {
                Potion potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(id));
                if (potion != null) names.add(net.minecraft.util.text.translation.I18n.translateToLocal(potion.getName()));
            } catch (RuntimeException ignored) {
                // Invalid entries are reported by apply(); tooltips simply omit them.
            }
        }
        return String.join(", ", names);
    }
    private static void warn(String entry) {
        if (WARNED.add(entry)) NanonaitorsArsenal.LOGGER.warn("Skipping invalid/unavailable configured effect: {}", entry);
    }
}
