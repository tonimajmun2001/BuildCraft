/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.builders.urbanism;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import buildcraft.builders.blueprints.BlueprintBuilder;
import buildcraft.builders.blueprints.BlueprintBuilder.SchematicBuilder;
import buildcraft.builders.filler.pattern.FillerPattern;
import buildcraft.core.Box;
import buildcraft.core.EntityFrame;
import buildcraft.core.EntityFrame.Kind;
import buildcraft.core.IBuilderInventory;
import buildcraft.core.TileBuildCraft;
import buildcraft.core.network.RPC;
import buildcraft.core.network.RPCHandler;
import buildcraft.core.network.RPCSide;

public class TileUrbanist extends TileBuildCraft implements IBuilderInventory {

	public EntityUrbanist urbanist;
	EntityLivingBase player;
	int thirdPersonView = 0;

	double posX, posY, posZ;
	float yaw;

	int p2x = 0, p2y = 0, p2z = 0;
	EntityFrame frame;

	LinkedList <EntityRobotUrbanism> robots = new LinkedList<EntityRobotUrbanism>();

	LinkedList <UrbanistTask> tasks = new LinkedList <UrbanistTask> ();

	public void createUrbanistEntity() {
		if (worldObj.isRemote) {
			if (urbanist == null) {
				urbanist = new EntityUrbanist(worldObj);
				worldObj.spawnEntityInWorld(urbanist);
				player = Minecraft.getMinecraft().renderViewEntity;

				urbanist.copyLocationAndAnglesFrom(player);
				urbanist.tile = this;
				urbanist.player = player;

				urbanist.rotationYaw = 0;
				urbanist.rotationPitch = 0;

				Minecraft.getMinecraft().renderViewEntity = urbanist;
				thirdPersonView = Minecraft.getMinecraft().gameSettings.thirdPersonView;
				Minecraft.getMinecraft().gameSettings.thirdPersonView = 8;

				posX = urbanist.posX;
				posY = urbanist.posY + 10;
				posZ = urbanist.posZ;

				yaw = 0;

				urbanist.setPositionAndRotation(posX, posY, posZ, yaw, 50);
				urbanist.setPositionAndUpdate(posX, posY, posZ);

				RPCHandler.rpcServer(this, "spawnRobot");
			}
		}
	}

	@Override
	public void updateEntity() {
		super.updateEntity();

		if (tasks.size() > 0) {
			UrbanistTask headTask = tasks.getFirst();

			for (EntityRobotUrbanism robot : robots) {
				if (robot.isAvailable()) {
					robot.setTask(headTask);
					tasks.removeFirst();
					break;
				}
			}
		}
	}

	@RPC (RPCSide.SERVER)
	public void setBlock (int x, int y, int z) {
		worldObj.setBlock(x, y, z, Blocks.brick_block);
	}

	@RPC (RPCSide.SERVER)
	public void eraseBlock (int x, int y, int z) {
		tasks.add(new UrbanistTaskErase(this, x, y, z));
	}

	public void rpcEraseBlock (int x, int y, int z) {
		RPCHandler.rpcServer(this, "eraseBlock", x, y, z);
	}

	@RPC (RPCSide.SERVER)
	public void createFrame (int x, int y, int z) {
		if (frame == null) {
			frame = new EntityFrame(worldObj, x + 0.5F, y + 0.5F, z + 0.5F, 1, 1, 1);
			worldObj.spawnEntityInWorld(frame);
		}
	}

	public void rpcCreateFrame (int x, int y, int z) {
		p2x = x;
		p2y = y;
		p2z = z;

		RPCHandler.rpcServer(this, "createFrame", x, y, z);
	}

	@RPC (RPCSide.SERVER)
	public void moveFrame (int x, int y, int z) {
		if (frame != null) {
			frame.xSize = x - frame.posX + 0.5F;
			frame.ySize = y - frame.posY + 0.5F;
			frame.zSize = z - frame.posZ + 0.5F;
			frame.updateData();
		}
	}

	public void rpcMoveFrame (int x, int y, int z) {
		if (p2x != x || p2y != y || p2z != z) {
			p2x = x;
			p2y = y;
			p2z = z;

			RPCHandler.rpcServer(this, "moveFrame", x, y, z);
		}
	}

	public class FrameTask {
		int nbOfTasks;
		EntityFrame frame;

		public void taskDone () {
			nbOfTasks--;

			if (nbOfTasks <= 0) {
				frame.setDead();
			}
		}
	}

	@RPC (RPCSide.SERVER)
	public void startFiller (String fillerTag, Box box) {
		BlueprintBuilder builder = FillerPattern.patterns.get(fillerTag).getBlueprint(box, worldObj);

		List <SchematicBuilder> schematics = builder.getBuilders();

		if (frame != null) {
			frame.setDead();
			frame = null;
		}

		EntityFrame newFrame = new EntityFrame(worldObj, box);
		newFrame.setKind(Kind.STRIPES);
		worldObj.spawnEntityInWorld(newFrame);

		FrameTask task = new FrameTask();
		task.frame = newFrame;

		for (SchematicBuilder b : schematics) {
			if (!b.isComplete()) {
				tasks.add(new UrbanistTaskBuildSchematic(this, b, task));
				task.nbOfTasks++;
			}
		}
	}

	public void rpcStartFiller (String fillerTag, Box box) {
		RPCHandler.rpcServer(this, "startFiller", fillerTag, box);
	}

	@RPC (RPCSide.SERVER)
	public void spawnRobot () {
		if (robots.size() == 0) {
			for (int i = 0; i < 10; ++i) {
				EntityRobotUrbanism robot = new EntityRobotUrbanism(worldObj);
				robot.setLocationAndAngles(xCoord, yCoord, zCoord, 0, 0);
				//robot.setDestination(xCoord, yCoord, zCoord);
				//robot.setDestinationAround(xCoord, yCoord, zCoord);

				worldObj.spawnEntityInWorld(robot);

				robots.add(robot);
			}
		}
	}

	public void destroyUrbanistEntity() {
		Minecraft.getMinecraft().renderViewEntity = player;
		Minecraft.getMinecraft().gameSettings.thirdPersonView = thirdPersonView;
		worldObj.removeEntity(urbanist);
		urbanist.setDead();
		urbanist = null;
	}

	@Override
	public int getSizeInventory() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getInventoryStackLimit() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer) {
		return true;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBuildingMaterial(int i) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getInventoryName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasCustomInventoryName() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void openInventory() {
		// TODO Auto-generated method stub

	}

	@Override
	public void closeInventory() {
		// TODO Auto-generated method stub

	}

}
