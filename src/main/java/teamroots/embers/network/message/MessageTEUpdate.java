package teamroots.embers.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import teamroots.embers.particle.ParticleUtil;
import teamroots.embers.world.EmberWorldData;

public class MessageTEUpdate implements IMessage {
	public NBTTagCompound tag = new NBTTagCompound();
	
	public MessageTEUpdate(){
		//
	}
	
	public MessageTEUpdate(TileEntity tile){
		this.tag = tile.getUpdateTag();
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		tag = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeTag(buf, tag);
	}

    public static class MessageHolder implements IMessageHandler<MessageTEUpdate,IMessage>
    {
    	@SideOnly(Side.CLIENT)
        @Override
        public IMessage onMessage(final MessageTEUpdate message, final MessageContext ctx) {
    		Minecraft.getMinecraft().addScheduledTask(()-> {
	    		if ((Minecraft.getMinecraft().thePlayer.getEntityWorld()).getTileEntity(new BlockPos(message.tag.getInteger("x"),message.tag.getInteger("y"),message.tag.getInteger("z"))) != null){
	    			Minecraft.getMinecraft().thePlayer.getEntityWorld().getTileEntity(new BlockPos(message.tag.getInteger("x"),message.tag.getInteger("y"),message.tag.getInteger("z"))).readFromNBT(message.tag);
	    		}
	    	});
    		return null;
	    }
    }
}
