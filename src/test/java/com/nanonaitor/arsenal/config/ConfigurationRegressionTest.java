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
        String[][] families={{"morningStar","morning_star"},{"scimitars","scimitar"},
            {"bladeStaff","blade_staff"},{"claws","claws"},{"flail","flail"},
            {"batteringRam","battering_ram"},{"ballAndChain","ball_and_chain"},
            {"tartsyShield","tartsy_shield"},{"sunWarBulwark","sun_war_bulwark"}};
        try {
            for(String[] family:families) {
                // Each weapon has its own titled config section with an enabled switch.
                Object section=ArsenalConfig.class.getField(family[0]).get(null);
                java.lang.reflect.Field field=section.getClass().getField("enabled");
                check(ContentSwitches.enabled(family[1]+"_iron"));
                field.setBoolean(section,false);
                check(!ContentSwitches.enabled(family[1]+"_iron"));
                check(ContentSwitches.enabled("iron_chain"));
                field.setBoolean(section,true);
            }
            // Every default effect list in every section parses.
            for(java.lang.reflect.Field category:ArsenalConfig.class.getFields()) {
                Object section=category.get(null);
                if(section==null||section instanceof String)continue;
                for(java.lang.reflect.Field field:section.getClass().getFields())
                    if(field.getType()==String[].class && !"entityBlacklist".equals(field.getName()))
                        for(String entry:(String[])field.get(section))
                            check(EffectSpec.parse(entry,200,1)!=null);
            }
        } catch(ReflectiveOperationException exception) { throw new AssertionError(exception); }
        // Custom material lines: valid ones parse, bad ones are rejected with a reason.
        com.nanonaitor.arsenal.item.WeaponTier.CustomSpec copper=com.nanonaitor.arsenal.item.WeaponTier.CustomSpec
            .parse(" copper, Copper, #D9824B, ore:ingotCopper, 2, 300, 1.5, 14 ");
        check(copper.id.equals("copper") && copper.color==0xD9824B && copper.harvest==2 && copper.durability==300
            && copper.damage==1.5F && copper.enchantability==14 && copper.repair.equals("ore:ingotCopper"));
        for(String bad:new String[]{"copper, Copper, #D9824B", "Copper!, Copper, #D9824B, ore:ingotCopper, 2, 300, 1.5, 14",
            "copper, Copper, orange, ore:ingotCopper, 2, 300, 1.5, 14", "copper, Copper, #D9824B, ingotCopper, 2, 300, 1.5, 14",
            "copper, Copper, #D9824B, ore:ingotCopper, two, 300, 1.5, 14", "copper, Copper, #D9824B, ore:ingotCopper, 2, 0, 1.5, 14"}) {
            boolean rejected=false;
            try { com.nanonaitor.arsenal.item.WeaponTier.CustomSpec.parse(bad); } catch(IllegalArgumentException expected){rejected=true;}
            check(rejected);
        }
        System.out.println("Passed "+checks+" effect/charge regression checks");
    }
}
