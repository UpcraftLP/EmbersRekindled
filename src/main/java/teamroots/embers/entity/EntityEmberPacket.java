package teamroots.embers.entity;

import java.awt.Color;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import teamroots.embers.lighting.ILightProvider;
import teamroots.embers.lighting.Light;
import teamroots.embers.network.PacketHandler;
import teamroots.embers.network.message.MessageEmberSparkleFX;
import teamroots.embers.particle.ParticleUtil;
import teamroots.embers.power.EmberCapabilityProvider;
import teamroots.embers.power.IEmberPacketReceiver;
import teamroots.embers.util.Misc;

public class EntityEmberPacket extends Entity implements ILightProvider {

	BlockPos pos = new BlockPos(0,0,0);
	public BlockPos dest = new BlockPos(0,0,0);
	public double value = 0;
	public int lifetime = 80;
	public float hue = Misc.random.nextFloat()*360.0f;
	public boolean dead = false;
	
	public EntityEmberPacket(World worldIn) {
		super(worldIn);
		setSize(0,0);
		this.setInvisible(true);
	}
	
	public void initCustom(BlockPos pos, BlockPos dest, double vx, double vy, double vz, double value){
		this.posX = pos.getX()+0.5;
		this.posY = pos.getY()+0.5;
		this.posZ = pos.getZ()+0.5;
		this.motionX = vx;
		this.motionY = vy;
		this.motionZ = vz;
		this.dest = dest;
		this.pos = pos;
		this.value = value;
	}

	@Override
	protected void entityInit() {
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		if (compound.hasKey("destX")){
			dest = new BlockPos(compound.getInteger("destX"),compound.getInteger("destY"),compound.getInteger("destZ"));
			setPosition(compound.getDouble("x"),compound.getDouble("y"),compound.getDouble("z"));
			value = compound.getDouble("value");
			motionX = compound.getDouble("vx");
			motionY = compound.getDouble("vy");
			motionZ = compound.getDouble("vz");
			dead = compound.getBoolean("dead");
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
		if (dest != null){
			compound.setInteger("destX",dest.getX());
			compound.setInteger("destY",dest.getY());
			compound.setInteger("destZ",dest.getZ());
		}
		compound.setDouble("x", posX);
		compound.setDouble("y", posY);
		compound.setDouble("z", posZ);
		compound.setDouble("value", value);
		compound.setDouble("vx", motionX);
		compound.setDouble("vy", motionY);
		compound.setDouble("vz", motionZ);
		compound.setBoolean("dead", dead);
	}
	
	@Override
	public void onUpdate(){
		super.onUpdate();
		this.hue += 2.0f;
		if (this.lifetime == 79){
			if (getEntityWorld().isRemote){
				PacketHandler.INSTANCE.sendToAll(new MessageEmberSparkleFX(posX,posY,posZ));
			}
		}
		
		lifetime --;
		if (lifetime <= 0){
			getEntityWorld().removeEntity(this);
			this.kill();
		}
		if (!dead){
			if (dest.getX() != 0 || dest.getY() != 0 || dest.getZ() != 0){
				double targetX = dest.getX()+0.5;
				double targetY = dest.getY()+0.5;
				double targetZ = dest.getZ()+0.5;
				Vec3d targetVector = new Vec3d(targetX-posX,targetY-posY,targetZ-posZ);
				double length = targetVector.lengthVector();
				targetVector = targetVector.scale(0.3/length);
				double weight  = 0;
				if (length <= 3){
					weight = 0.9*((3.0-length)/3.0);
				}
				motionX = (0.9-weight)*motionX+(0.1+weight)*targetVector.xCoord;
				motionY = (0.9-weight)*motionY+(0.1+weight)*targetVector.yCoord;
				motionZ = (0.9-weight)*motionZ+(0.1+weight)*targetVector.zCoord;
			}
			posX += motionX;
			posY += motionY;
			posZ += motionZ;
			IBlockState state = getEntityWorld().getBlockState(getPosition());
			TileEntity tile = getEntityWorld().getTileEntity(getPosition());
			BlockPos pos = getPosition();
			if (posX > pos.getX()+0.25 && posX < pos.getX()+0.75 && posY > pos.getY()+0.25 && posY < pos.getY()+0.75 && posZ > pos.getZ()+0.25 && posZ < pos.getZ()+0.75){
				affectTileEntity(state,tile);
				if (state.isFullCube() && state.isOpaqueCube()){
					if (!getEntityWorld().isRemote){
						PacketHandler.INSTANCE.sendToAll(new MessageEmberSparkleFX(posX,posY,posZ));
					}
				}
			}
			if (getEntityWorld().isRemote){
				for (double i = 0; i < 9; i ++){
					double coeff = i/9.0;
					ParticleUtil.spawnParticleGlow(getEntityWorld(), (float)(prevPosX+(posX-prevPosX)*coeff), (float)(prevPosY+(posY-prevPosY)*coeff), (float)(prevPosZ+(posZ-prevPosZ)*coeff), 0.0125f*(rand.nextFloat()-0.5f), 0.0125f*(rand.nextFloat()-0.5f), 0.0125f*(rand.nextFloat()-0.5f), 255, 64, 16, 2.0f, 24);
				}
			}
		}
	}
	
	public void affectTileEntity(IBlockState state, TileEntity tile){
		if (tile instanceof IEmberPacketReceiver){
			if (((IEmberPacketReceiver)tile).onReceive(this)){
				if (tile.hasCapability(EmberCapabilityProvider.emberCapability, null)){
					if (!getEntityWorld().isRemote){
						PacketHandler.INSTANCE.sendToAll(new MessageEmberSparkleFX(posX,posY,posZ));
					}
					tile.getCapability(EmberCapabilityProvider.emberCapability, null).addAmount(value, true);
					tile.markDirty();
					this.motionX = 0;
					this.motionY = 0;
					this.motionZ = 0;
					this.lifetime = 20;
					this.dead = true;
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public Light provideLight() {
		float shiftedLifetime = Math.max(0.0f, (float)lifetime-Minecraft.getMinecraft().getRenderPartialTicks());
		if (!dead){
			return new Light((float)posX,(float)posY,(float)posZ,1.0f,0.25f,0.0625f,1.0f,4.0f);
		}
		else if (shiftedLifetime > 75){
			return new Light((float)posX,(float)posY,(float)posZ,1.0f,0.25f,0.0625f,(80.0f-shiftedLifetime)/5.0f,4.0f);
		}
		else {
			return new Light((float)posX,(float)posY,(float)posZ,1.0f,0.25f,0.0625f,(shiftedLifetime/20.0f),4.0f*(shiftedLifetime/20.0f));
		}
	}

}
