package com.nanonaitor.arsenal.combat;

public final class GuardRulesTest {
    private static int checks;
    private static void equal(int expected, int actual) {
        checks++;
        if (expected != actual) throw new AssertionError(expected + " != " + actual);
    }
    public static void main(String[] args) {
        for (String material : new String[]{"wood", "stone", "gold", "iron", "diamond", "silver", "bronze", "steel"}) {
            checks++;
            if (!material.equals(com.nanonaitor.arsenal.compat.ScimitarShieldCompat.shieldMaterial(material)))
                throw new AssertionError("Wrong shield mapping for " + material);
        }
        for (String material : new String[]{"umbrium", "desert_myrmex", "jungle_myrmex", "desert_venom", "jungle_venom"}) {
            checks++;
            if (!"iron".equals(com.nanonaitor.arsenal.compat.ScimitarShieldCompat.shieldMaterial(material)))
                throw new AssertionError("Wrong iron fallback for " + material);
        }
        checks++;
        if (!"diamond".equals(com.nanonaitor.arsenal.compat.ScimitarShieldCompat.shieldMaterial("sentient")))
            throw new AssertionError("Wrong upper-tier fallback");
        near(5.0D,GuardRules.pairedInterval(10,10));
        near(4.0D,GuardRules.pairedInterval(5,20));
        near(1.0D,GuardRules.pairedInterval(1,1));
        near(2.5D,GuardRules.pairedInterval(5,5));
        equal(1, GuardRules.scimitarWear(0));
        equal(1, GuardRules.scimitarWear(1));
        equal(1, GuardRules.scimitarWear(2));
        equal(2, GuardRules.scimitarWear(2.1F));
        equal(3, GuardRules.scimitarWear(5));
        equal(10, GuardRules.scimitarWear(20));
        equal(25, GuardRules.MAX_STRAIN);
        equal(60, GuardRules.RECOVERY_TICKS);
        equal(25, GuardRules.recoveredStrain(25, 19));
        equal(24, GuardRules.recoveredStrain(25, 20));
        equal(23, GuardRules.recoveredStrain(25, 40));
        equal(0, GuardRules.recoveredStrain(25, 500));
        equal(0, GuardRules.recoveredStrain(1, 10000));
        equal(2, GuardRules.recoveredStrain(2, -20));
        System.out.println("Guard regression: " + checks + " checks passed.");
    }
    private static void near(double expected,double actual) {
        checks++;
        if(Math.abs(expected-actual)>0.00001D)throw new AssertionError(expected+" != "+actual);
    }
}
