package teamroots.embers.tileentity;

import java.util.ArrayList;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.BlockLever;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.TileFluidHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import teamroots.embers.RegistryManager;
import teamroots.embers.network.PacketHandler;
import teamroots.embers.network.message.MessageTEUpdate;
import teamroots.embers.particle.ParticleUtil;
import teamroots.embers.power.DefaultEmberCapability;
import teamroots.embers.power.EmberCapabilityProvider;
import teamroots.embers.power.IEmberCapability;
import teamroots.embers.power.IEmberPacketReceiver;
import teamroots.embers.util.Misc;
import teamroots.embers.world.EmberWorldData;

public class TileEntityCrystalCell extends TileEntity implements ITileEntityBase, ITickable, IMultiblockMachine {
	Random random = new Random();
	public long ticksExisted = 0;
	public float angle = 0;
	public int ticksFueled = 0;
	public long seed = 0;
	public IEmberCapability capability = new DefaultEmberCapability();
	public ItemStackHandler inventory = new ItemStackHandler(1){
        @Override
        protected void onContentsChanged(int slot) {
        	TileEntityCrystalCell.this.markDirty();
        	if (!TileEntityCrystalCell.this.getWorld().isRemote){
        		IBlockState state = TileEntityCrystalCell.this.getWorld().getBlockState(TileEntityCrystalCell.this.getPos());
        		TileEntityCrystalCell.this.getWorld().notifyBlockUpdate(TileEntityCrystalCell.this.getPos(), state, state, 8);
        	}
        }
        
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate){
        	if (stack.getItem() != RegistryManager.shardEmber && stack.getItem() != RegistryManager.crystalEmber){
        		return stack;
        	}
        	return super.insertItem(slot, stack, simulate);
        }
        
	};
	
	public TileEntityCrystalCell(){
		super();
		capability.setEmberCapacity(64000);
		seed = random.nextLong();
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag){
		super.writeToNBT(tag);
		tag.setLong("seed", seed);
		tag.setTag("inventory", inventory.serializeNBT());
		capability.writeToNBT(tag);
		return tag;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag){
		super.readFromNBT(tag);
		inventory.deserializeNBT(tag.getCompoundTag("inventory"));
		capability.readFromNBT(tag);
		seed = tag.getLong("seed");
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public boolean activate(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
			ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
		return false;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		this.invalidate();
		Misc.spawnInventoryInWorld(world, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, inventory);
		world.setBlockToAir(pos.add(1,0,0));
		world.setBlockToAir(pos.add(0,0,1));
		world.setBlockToAir(pos.add(-1,0,0));
		world.setBlockToAir(pos.add(0,0,-1));
		world.setBlockToAir(pos.add(1,0,-1));
		world.setBlockToAir(pos.add(-1,0,1));
		world.setBlockToAir(pos.add(1,0,1));
		world.setBlockToAir(pos.add(-1,0,-1));
		world.setTileEntity(pos, null);
	}

	@Override
	public void update() {
		ticksExisted ++;
		if (inventory.getStackInSlot(0) != null && ticksExisted % 4 == 0){
			ItemStack stack = inventory.extractItem(0, 1, true);
			if (!getWorld().isRemote && stack != null){
				inventory.extractItem(0, 1, false);
				if (stack.getItem() == RegistryManager.shardEmber){
					this.capability.setEmberCapacity(Math.min(1440000, this.capability.getEmberCapacity()+1500.0));
					markDirty();
					if (!getWorld().isRemote){
						PacketHandler.INSTANCE.sendToAll(new MessageTEUpdate(this));
					}
				}
				else if (stack.getItem() == RegistryManager.crystalEmber){
					this.capability.setEmberCapacity(Math.min(1440000, this.capability.getEmberCapacity()+9000.0));
					markDirty();
					if (!getWorld().isRemote){
						PacketHandler.INSTANCE.sendToAll(new MessageTEUpdate(this));
					}
				}
			}
			if (getWorld().isRemote && stack != null){
				double angle = random.nextDouble()*2.0*Math.PI;
				double x = getPos().getX()+0.5+0.5*Math.sin(angle);
				double z = getPos().getZ()+0.5+0.5*Math.cos(angle);
				double x2 = getPos().getX()+0.5;
				double z2 = getPos().getZ()+0.5;
				float layerHeight = 0.25f;
				float numLayers = 2+(float) Math.floor(capability.getEmberCapacity()/128000.0f);
				float height = layerHeight*numLayers;
				for (float i = 0; i < 72; i ++){
					float coeff = i/72.0f;
					ParticleUtil.spawnParticleGlow(getWorld(), (float)x*(1.0f-(float)coeff)+(float)x2*coeff, getPos().getY()+(1.0f-(float)coeff)+(height/2.0f+1.5f)*coeff, (float)z*(1.0f-(float)coeff)+(float)z2*coeff, 0, 0, 0, 255, 64, 16, 2.0f);
				}
			}
		}
		float numLayers = 2+(float) Math.floor(capability.getEmberCapacity()/128000.0f);
		for (int i = 0; i < numLayers/2; i ++){
			float layerHeight = 0.25f;
			float height = layerHeight*numLayers;
			float xDest = getPos().getX()+0.5f;
			float yDest = getPos().getY()+height/2.0f+1.5f;
			float zDest = getPos().getZ()+0.5f;
			float x = getPos().getX()+0.5f+2.0f*(random.nextFloat()-0.5f);
			float z = getPos().getZ()+0.5f+2.0f*(random.nextFloat()-0.5f);
			float y = getPos().getY()+1.0f;
			ParticleUtil.spawnParticleGlow(getWorld(), x, y, z, (xDest-x)/24.0f, (yDest-y)/24.0f, (zDest-z)/24.0f, 255, 64, 16, 2.0f);
		}
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing){
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY){
			return true;
		}
		if (capability == EmberCapabilityProvider.emberCapability){
			return true;
		}
		return super.hasCapability(capability, facing);
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing){
		super.getCapability(capability, facing);
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY){
			return (T)this.inventory;
		}
		if (capability == EmberCapabilityProvider.emberCapability){
			return (T)this.capability;
		}
		return null;
	}
}
