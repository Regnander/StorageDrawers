package com.jaquadro.minecraft.storagedrawers.block;

import com.jaquadro.minecraft.storagedrawers.api.storage.EnumBasicDrawer;
import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.item.ItemCustomDrawers;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import java.util.List;

public class BlockDrawersCustom extends BlockDrawers
{
    public BlockDrawersCustom (String blockName) {
        super(blockName);
    }

    protected void initDefaultState () {
        setDefaultState(blockState.getBaseState().withProperty(BLOCK, EnumBasicDrawer.FULL2));
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public boolean canRenderInLayer (BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT_MIPPED || layer == BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    protected ItemStack getMainDrop (IBlockAccess world, BlockPos pos, IBlockState state) {
        TileEntityDrawers tile = getTileEntity(world, pos);
        if (tile == null)
            return ItemCustomDrawers.makeItemStack(state, 1, null, null, null);

        return ItemCustomDrawers.makeItemStack(state, 1, tile.getMaterialSide(), tile.getMaterialTrim(), tile.getMaterialFront());
    }

    @Override
    public void getSubBlocks (Item item, CreativeTabs creativeTabs, List<ItemStack> list) {
        for (EnumBasicDrawer type : EnumBasicDrawer.values())
            list.add(new ItemStack(item, 1, type.getMetadata()));
    }

    @Override
    public boolean onBlockActivated (World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, ItemStack item, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntityDrawers tile = getTileEntity(world, pos);
        if (tile != null && tile.getMaterialSide() == null)
            return false;

        return super.onBlockActivated(world, pos, state, player, hand, item, side, hitX, hitY, hitZ);
    }

    @Override
    protected BlockStateContainer createBlockState () {
        return new ExtendedBlockState(this, new IProperty[] { BLOCK, FACING }, new IUnlistedProperty[] { TILE });
    }

    @Override
    public IBlockState getActualState (IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntityDrawers tile = getTileEntity(worldIn, pos);
        if (tile == null)
            return state;

        EnumFacing facing = EnumFacing.getFront(tile.getDirection());
        if (facing.getAxis() == EnumFacing.Axis.Y)
            facing = EnumFacing.NORTH;

        return state.withProperty(BLOCK, state.getValue(BLOCK))
            .withProperty(FACING, facing);
    }
}
