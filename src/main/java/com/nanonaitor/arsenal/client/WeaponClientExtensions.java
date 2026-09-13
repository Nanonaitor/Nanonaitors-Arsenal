package com.nanonaitor.arsenal.client;
import com.nanonaitor.arsenal.item.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Item.initializeClient runs inside Item's constructor, before subclass fields exist. */
public final class WeaponClientExtensions implements IClientItemExtensions {
    private static final IClientItemExtensions RAM=new BatteringRamClientExtensions();
    private static final IClientItemExtensions STAR=new MorningStarClientExtensions();
    private static final IClientItemExtensions BALL=new BallChainClientExtensions();
    private static final IClientItemExtensions SCIMITAR=new ScimitarClientExtensions();
    private static final IClientItemExtensions STAFF=new BladeStaffClientExtensions();
    public static void bootstrap() {}
    @Override public HumanoidModel.ArmPose getArmPose(LivingEntity entity,InteractionHand hand,ItemStack stack){
        if(!(stack.getItem() instanceof ArsenalWeaponItem item))return null;
        IClientItemExtensions extension=switch(item.kind()){
            case BATTERING_RAM -> RAM;
            case MORNING_STAR -> STAR;
            case BALL_AND_CHAIN -> BALL;
            case SCIMITAR -> SCIMITAR;
            case BLADE_STAFF -> STAFF;
            default -> null;
        };
        return extension==null?null:extension.getArmPose(entity,hand,stack);
    }
}
