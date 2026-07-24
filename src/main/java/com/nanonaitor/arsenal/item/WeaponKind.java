package com.nanonaitor.arsenal.item;

public enum WeaponKind {
    MORNING_STAR("morning_star", 4.0F, -3.0F),
    SCIMITAR("scimitar", 2.5F, -2.2F),
    CLAWS("claws", -2.0F, -1.6F),
    LINKED_CLAWS("linked_claws", -2.0F, -1.6F),
    FLAIL("flail", 2.0F, -3.2F),
    BATTERING_RAM("battering_ram", 7.0F, -3.6F),
    BALL_AND_CHAIN("ball_and_chain", 3.0F, -3.4F);

    public final String id;
    public final float damageBaseline;
    public final float speedModifier;
    WeaponKind(String id, float damageBaseline, float speedModifier) {
        this.id = id;
        this.damageBaseline = damageBaseline;
        this.speedModifier = speedModifier;
    }
}
