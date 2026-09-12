package com.nanonaitor.arsenal.config;
import com.nanonaitor.arsenal.combat.BallChargeRules;
public final class ConfigurationRegressionTest {
    private static int checks;
    private static void check(boolean value) { checks++; if(!value)throw new AssertionError("Check "+checks); }
    public static void main(String[] args) {
        check(EffectSpec.parse("",200,0)==null);
        check(EffectSpec.parse("   ",200,0)==null);
        check(EffectSpec.parse(null,200,0)==null);
        EffectSpec explicit=EffectSpec.parse("minecraft:weakness@2@10",200,0);
        check(explicit.amplifier==1 && explicit.ticks==200);
        EffectSpec tier=EffectSpec.parse("nanonaitors_arsenal:armor_fracture@tier@tier",600,4);
        check(tier.amplifier==4 && tier.ticks==600);
        check(EffectSpec.parse("minecraft:slowness@1@0.5",200,0).ticks==10);
        for(String bad:new String[]{"weakness","minecraft:weakness@0@10","minecraft:weakness@1@NaN",
            "minecraft:weakness@1@Infinity","minecraft:weakness@1@-1","minecraft:weakness@257@1","@1@1"}) {
            boolean rejected=false;
            try { EffectSpec.parse(bad,200,0); } catch(IllegalArgumentException expected){rejected=true;}
            check(rejected);
        }
        check(BallChargeRules.nextCharge(0,3,true)==1);
        check(BallChargeRules.nextCharge(1,3,true)==3);
        check(BallChargeRules.nextCharge(1,3,false)==2);
        check(BallChargeRules.nextCharge(2,3,false)==3);
        check(BallChargeRules.nextCharge(3,3,true)==3);
        check(BallChargeRules.nextCharge(1,2,true)==2);
        check(BallChargeRules.nextCharge(1,2,false)==2);
        String[][] families={{"morningStar","morning_star"},{"scimitar","scimitar"},
            {"bladeStaff","double_bladed_scimitar"},{"claws","claws"},{"flail","flail"},
            {"batteringRam","battering_ram"},{"ballAndChain","ball_and_chain"},
            {"tartsyShield","tartsy_shield"},{"sunWarBulwark","sun_war_bulwark"}};
        try {
            for(String[] family:families) {
                java.lang.reflect.Field field=ArsenalConfig.Weapons.class.getField(family[0]);
                check(ContentSwitches.enabled(family[1]+"_iron"));
                field.setBoolean(ArsenalConfig.weapons,false);
                check(!ContentSwitches.enabled(family[1]+"_iron"));
                check(ContentSwitches.enabled("iron_chain"));
                field.setBoolean(ArsenalConfig.weapons,true);
            }
            for(java.lang.reflect.Field field:ArsenalConfig.Effects.class.getFields())
                for(String entry:(String[])field.get(ArsenalConfig.effects))
                    check(EffectSpec.parse(entry,200,1)!=null);
        } catch(ReflectiveOperationException exception) { throw new AssertionError(exception); }
        System.out.println("Passed "+checks+" effect/charge regression checks");
    }
}
