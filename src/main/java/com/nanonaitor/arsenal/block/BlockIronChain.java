package com.nanonaitor.arsenal.block;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class BlockIronChain extends Block {
    public static final PropertyEnum<EnumFacing.Axis> AXIS =
        PropertyEnum.create("axis", EnumFacing.Axis.class);
    private static final AxisAlignedBB X_BOX =
        new AxisAlignedBB(0.0D, 0.375D, 0.375D, 1.0D, 0.625D, 0.625D);
    private static final AxisAlignedBB Y_BOX =
        new AxisAlignedBB(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D);
    private static final AxisAlignedBB Z_BOX =
        new AxisAlignedBB(0.375D, 0.375D, 0.0D, 0.625D, 0.625D, 1.0D);

    public BlockIronChain() {
        super(Material.IRON);
        setRegistryName(NanonaitorsArsenal.MOD_ID, "iron_chain");
        setUnlocalizedName(NanonaitorsArsenal.MOD_ID + ".iron_chain");
        setCreativeTab(NanonaitorsArsenal.CREATIVE_TAB);
        setHardness(5.0F);
        setResistance(6.0F);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
        setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos,
            EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {
        return getDefaultState().withProperty(AXIS, facing.getAxis());
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source,
                                        BlockPos pos) {
        switch (state.getValue(AXIS)) {
            case X: return X_BOX;
            case Z: return Z_BOX;
            default: return Y_BOX;
        }
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state,
            BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AXIS).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing.Axis[] axes = EnumFacing.Axis.values();
        return getDefaultState().withProperty(AXIS,
            axes[Math.max(0, Math.min(meta, axes.length - 1))]);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AXIS);
    }
}
