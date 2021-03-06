/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 * 
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 * 
 * File Created @ [Jan 21, 2014, 7:48:54 PM (GMT)]
 */
package vazkii.botania.common.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import net.minecraftforge.fml.common.registry.GameRegistry;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.lexicon.ILexiconable;
import vazkii.botania.api.lexicon.LexiconEntry;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.api.state.enums.AltarVariant;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.tile.TileAltar;
import vazkii.botania.common.block.tile.TileSimpleInventory;
import vazkii.botania.common.core.helper.InventoryHelper;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.block.ItemBlockWithMetadataAndName;
import vazkii.botania.common.item.rod.ItemWaterRod;
import vazkii.botania.common.lexicon.LexiconData;
import vazkii.botania.common.lib.LibBlockNames;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

public class BlockAltar extends BlockMod implements ILexiconable {

	private static final AxisAlignedBB AABB = new AxisAlignedBB(0.125, 0.125, 0.125, 0.875, 20.0/16, 0.875);

	protected BlockAltar() {
		super(Material.ROCK, LibBlockNames.ALTAR);
		setHardness(3.5F);
		setSoundType(SoundType.STONE);
		setDefaultState(blockState.getBaseState()
				.withProperty(BotaniaStateProps.ALTAR_VARIANT, AltarVariant.DEFAULT)
		);
	}

	@Nonnull
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return AABB;
	}

	@Nonnull
	@Override
	public BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BotaniaStateProps.ALTAR_VARIANT);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(BotaniaStateProps.ALTAR_VARIANT).ordinal();
	}

	@Nonnull
	@Override
	public IBlockState getStateFromMeta(int meta) {
		if (meta < 0 || meta >= AltarVariant.values().length ) {
			meta = 0;
		}
		return getDefaultState().withProperty(BotaniaStateProps.ALTAR_VARIANT, AltarVariant.values()[meta]);
	}

	@Nonnull
	@Override
	public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		TileEntity te = worldIn.getTileEntity(pos);
		if (te instanceof TileAltar) {
			TileAltar altar = ((TileAltar) te);

			if (altar.isMossy) {
				state = state.withProperty(BotaniaStateProps.ALTAR_VARIANT, AltarVariant.MOSSY);
			}
		}
		return state;
	}

	@Nonnull
	@Override
	public BlockRenderLayer getBlockLayer() {
		return BlockRenderLayer.CUTOUT;
	}

	@Override
	public void registerItemForm() {
		GameRegistry.register(new ItemBlockWithMetadataAndName(this), getRegistryName());
	}

	@Override
	public void getSubBlocks(@Nonnull Item item, CreativeTabs tab, List<ItemStack> list) {
		for(int i = 0; i < 9; i++)
			list.add(new ItemStack(item, 1, i));
	}

	@Override
	public void onEntityCollidedWithBlock(World par1World, BlockPos pos, IBlockState state, Entity par5Entity) {
		if(par5Entity instanceof EntityItem) {
			TileAltar tile = (TileAltar) par1World.getTileEntity(pos);
			if(tile.collideEntityItem((EntityItem) par5Entity))
				VanillaPacketDispatcher.dispatchTEToNearbyPlayers(tile);
		}
	}

	@Override
	public int getLightValue(@Nonnull IBlockState state, IBlockAccess world, @Nonnull BlockPos pos) {
		if(world.getBlockState(pos).getBlock() != this)
			return world.getBlockState(pos).getLightValue(world, pos);
		TileAltar tile = (TileAltar) world.getTileEntity(pos);
		return (tile != null && tile.hasLava) ? 15 : 0;
	}

	@Override
	public boolean onBlockActivated(World par1World, BlockPos pos, IBlockState state, EntityPlayer par5EntityPlayer, EnumHand hand, ItemStack stack, EnumFacing par6, float par7, float par8, float par9) {
		TileAltar tile = (TileAltar) par1World.getTileEntity(pos);
		if(par5EntityPlayer.isSneaking()) {
			InventoryHelper.withdrawFromInventory(tile, par5EntityPlayer);
		} else if(tile.isEmpty() && tile.hasWater && stack == null)
			tile.trySetLastRecipe(par5EntityPlayer);
		else {
			if(stack != null && (isValidWaterContainer(stack) || stack.getItem() == ModItems.waterRod && ManaItemHandler.requestManaExact(stack, par5EntityPlayer, ItemWaterRod.COST, false))) {
				if(!tile.hasWater) {
					if(stack.getItem() == ModItems.waterRod)
						ManaItemHandler.requestManaExact(stack, par5EntityPlayer, ItemWaterRod.COST, true);
					else if(!par5EntityPlayer.capabilities.isCreativeMode)
						par5EntityPlayer.inventory.setInventorySlotContents(par5EntityPlayer.inventory.currentItem, getContainer(stack));

					tile.setWater(true);
					par1World.updateComparatorOutputLevel(pos, this);
					par1World.checkLight(pos);
				}

				return true;
			} else if(stack != null && stack.getItem() == Items.LAVA_BUCKET) {
				if(!par5EntityPlayer.capabilities.isCreativeMode)
					par5EntityPlayer.inventory.setInventorySlotContents(par5EntityPlayer.inventory.currentItem, getContainer(stack));

				tile.setLava(true);
				tile.setWater(false);
				par1World.updateComparatorOutputLevel(pos, this);
				par1World.checkLight(pos);

				return true;
			} else if(stack != null && stack.getItem() == Items.BUCKET && (tile.hasWater || tile.hasLava) && !Botania.gardenOfGlassLoaded) {
				ItemStack bucket = tile.hasLava ? new ItemStack(Items.LAVA_BUCKET) : new ItemStack(Items.WATER_BUCKET);
				if(stack.stackSize == 1)
					par5EntityPlayer.inventory.setInventorySlotContents(par5EntityPlayer.inventory.currentItem, bucket);
				else {
					if(!par5EntityPlayer.inventory.addItemStackToInventory(bucket))
						par5EntityPlayer.dropItem(bucket, false);
					stack.stackSize--;
				}

				if(tile.hasLava)
					tile.setLava(false);
				else tile.setWater(false);
				par1World.updateComparatorOutputLevel(pos, this);
				par1World.checkLight(pos);

				return true;
			}
		}

		return false;
	}

	public void fillWithRain(World world, BlockPos pos) {
		if(world.rand.nextInt(20) == 1) {
			TileEntity tile = world.getTileEntity(pos);
			if(tile instanceof TileAltar) {
				TileAltar altar = (TileAltar) tile;
				if(!altar.hasLava && !altar.hasWater)
					altar.setWater(true);
				world.updateComparatorOutputLevel(pos, this);
			}
		}
	}

	@Override
	public int damageDropped(IBlockState state) {
		return getMetaFromState(state);
	}

	private boolean isValidWaterContainer(ItemStack stack) {
		if(stack == null || stack.stackSize != 1)
			return false;
		if(stack.getItem() == ModItems.waterBowl)
			return true;

		if(stack.getItem() instanceof IFluidContainerItem) {
			FluidStack fluidStack = ((IFluidContainerItem) stack.getItem()).getFluid(stack);
			return fluidStack != null && fluidStack.getFluid() == FluidRegistry.WATER && fluidStack.amount >= FluidContainerRegistry.BUCKET_VOLUME;
		}
		FluidStack fluidStack = FluidContainerRegistry.getFluidForFilledItem(stack);
		return fluidStack != null && fluidStack.getFluid() == FluidRegistry.WATER && fluidStack.amount >= FluidContainerRegistry.BUCKET_VOLUME;
	}

	private ItemStack getContainer(ItemStack stack) {
		if(stack.getItem() == ModItems.waterBowl)
			return new ItemStack(Items.BOWL);

		if (stack.getItem().hasContainerItem(stack))
			return stack.getItem().getContainerItem(stack);
		else if (stack.getItem() instanceof IFluidContainerItem) {
			((IFluidContainerItem) stack.getItem()).drain(stack, FluidContainerRegistry.BUCKET_VOLUME, true);
			return stack;
		}
		return FluidContainerRegistry.drainFluidContainer(stack);
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Nonnull
	@Override
	public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
		return new TileAltar();
	}

	@Override
	public void breakBlock(@Nonnull World par1World, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
		TileSimpleInventory inv = (TileSimpleInventory) par1World.getTileEntity(pos);

		InventoryHelper.dropInventory(inv, par1World, state, pos);

		super.breakBlock(par1World, pos, state);
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state) {
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState state, World par1World, BlockPos pos) {
		TileAltar altar = (TileAltar) par1World.getTileEntity(pos);
		return altar.hasWater ? 15 : 0;
	}

	@Override
	public LexiconEntry getEntry(World world, BlockPos pos, EntityPlayer player, ItemStack lexicon) {
		return LexiconData.apothecary;
	}

}
