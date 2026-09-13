package com.nanonaitor.arsenal.config;

/** Pure parser; potion lookup happens only when applying the effect in-game. */
public final class EffectSpec {
    public final String id;
    public final int ticks;
    public final int amplifier;
    private EffectSpec(String id, int ticks, int amplifier) {
        this.id=id; this.ticks=ticks; this.amplifier=amplifier;
    }
    public static EffectSpec parse(String text, int defaultTicks, int defaultAmplifier) {
        if (text == null || text.trim().isEmpty()) return null;
        String[] parts=text.trim().split("@",-1);
        if (parts.length != 3 || !parts[0].trim().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Expected modid:potion@level@seconds");
        int amplifier=parts[1].trim().equals("tier") ? defaultAmplifier : Integer.parseInt(parts[1].trim())-1;
        double seconds=parts[2].trim().equals("tier") ? defaultTicks/20.0D : Double.parseDouble(parts[2].trim());
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > Integer.MAX_VALUE/20.0D || amplifier < 0 || amplifier > 255)
            throw new IllegalArgumentException("Invalid effect level/duration");
        int ticks=(int)Math.round(seconds*20);
        if (ticks < 1) throw new IllegalArgumentException("Effect duration is shorter than one tick");
        return new EffectSpec(parts[0].trim(),ticks,amplifier);
    }
}
