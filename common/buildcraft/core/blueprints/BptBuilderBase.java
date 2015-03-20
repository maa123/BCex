/**
 * Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.core.blueprints;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.logging.log4j.Level;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.BlockEvent;

import buildcraft.BuildCraftCore;
import buildcraft.api.blueprints.BuilderAPI;
import buildcraft.api.blueprints.IBuilderContext;
import buildcraft.api.blueprints.MappingNotFoundException;
import buildcraft.api.blueprints.SchematicBlock;
import buildcraft.api.blueprints.SchematicBlockBase;
import buildcraft.api.core.BCLog;
import buildcraft.api.core.BlockIndex;
import buildcraft.api.core.IAreaProvider;
import buildcraft.api.core.Position;
import buildcraft.core.Box;
import buildcraft.core.builders.BuildingItem;
import buildcraft.core.builders.BuildingSlot;
import buildcraft.core.builders.BuildingSlotBlock;
import buildcraft.core.builders.IBuildingItemsProvider;
import buildcraft.core.builders.TileAbstractBuilder;
import buildcraft.core.proxy.CoreProxy;
import buildcraft.core.lib.utils.BlockUtils;

public abstract class BptBuilderBase implements IAreaProvider {

	public BlueprintBase blueprint;
	public BptContext context;
	protected boolean done;
	protected HashSet<BlockIndex> clearedLocations = new HashSet<BlockIndex>();
	protected HashSet<BlockIndex> builtLocations = new HashSet<BlockIndex>();
	protected int x, y, z;
	protected boolean initialized = false;

	private long nextBuildDate = 0;

	public BptBuilderBase(BlueprintBase bluePrint, World world, int x, int y, int z) {
		this.blueprint = bluePrint;
		this.x = x;
		this.y = y;
		this.z = z;
		done = false;

		Box box = new Box();
		box.initialize(this);

		context = bluePrint.getContext(world, box);
	}

	public void initialize() {
		if (!initialized) {
			internalInit();
			initialized = true;
		}
	}

	protected abstract void internalInit();

	protected abstract BuildingSlot reserveNextBlock(World world);

	protected abstract BuildingSlot getNextBlock(World world, TileAbstractBuilder inv);

	public boolean buildNextSlot(World world, TileAbstractBuilder builder, double x, double y, double z) {
		initialize();

		if (world.getTotalWorldTime() < nextBuildDate) {
			return false;
		}

		BuildingSlot slot = getNextBlock(world, builder);

		if (buildSlot(world, builder, slot, x + 0.5F, y + 0.5F, z + 0.5F)) {
			nextBuildDate = world.getTotalWorldTime() + slot.buildTime();
			return true;
		} else {
			return false;
		}
	}

	public boolean buildSlot(World world, IBuildingItemsProvider builder, BuildingSlot slot, double x, double y,
			double z) {
		initialize();

		if (slot != null) {
			slot.built = true;
			BuildingItem i = new BuildingItem();
			i.origin = new Position(x, y, z);
			i.destination = slot.getDestination();
			i.slotToBuild = slot;
			i.context = getContext();
			i.setStacksToDisplay(slot.getStacksToDisplay());
			builder.addAndLaunchBuildingItem(i);

			return true;
		}

		return false;
	}

	public BuildingSlot reserveNextSlot(World world) {
		initialize();

		return reserveNextBlock(world);
	}

	@Override
	public int xMin() {
		return x - blueprint.anchorX;
	}

	@Override
	public int yMin() {
		return y - blueprint.anchorY;
	}

	@Override
	public int zMin() {
		return z - blueprint.anchorZ;
	}

	@Override
	public int xMax() {
		return x + blueprint.sizeX - blueprint.anchorX - 1;
	}

	@Override
	public int yMax() {
		return y + blueprint.sizeY - blueprint.anchorY - 1;
	}

	@Override
	public int zMax() {
		return z + blueprint.sizeZ - blueprint.anchorZ - 1;
	}

	@Override
	public void removeFromWorld() {

	}

	public AxisAlignedBB getBoundingBox() {
		return AxisAlignedBB.getBoundingBox(xMin(), yMin(), zMin(), xMax(), yMax(), zMax());
	}

	public void postProcessing(World world) {

	}

	public BptContext getContext() {
		return context;
	}

	public void removeDoneBuilders (TileAbstractBuilder builder) {
		ArrayList<BuildingItem> items = builder.getBuilders();

		for (int i = items.size() - 1; i >= 0; --i) {
			if (items.get(i).isDone()) {
				items.remove(i);
			}
		}
	}

	public boolean isDone(IBuildingItemsProvider builder) {
		return done && builder.getBuilders().size() == 0;
	}

	private int getBlockBreakEnergy(BuildingSlotBlock slot) {
		return BlockUtils.computeBlockBreakEnergy(context.world(), slot.x, slot.y, slot.z);
	}

	protected final boolean canDestroy(TileAbstractBuilder builder, IBuilderContext context, BuildingSlotBlock slot) {
		LinkedList<ItemStack> result = new LinkedList<ItemStack>();

		return builder.energyAvailable() >= getBlockBreakEnergy(slot);
	}

	public void consumeEnergyToDestroy(TileAbstractBuilder builder, BuildingSlotBlock slot) {
		builder.consumeEnergy(getBlockBreakEnergy(slot));
	}

	public void createDestroyItems(BuildingSlotBlock slot) {
		int hardness = (int) Math.ceil(getBlockBreakEnergy(slot) / BuilderAPI.BREAK_ENERGY);

		for (int i = 0; i < hardness; ++i) {
			slot.addStackConsumed(new ItemStack(BuildCraftCore.buildToolBlock));
		}
	}

	public void useRequirements(IInventory inv, BuildingSlot slot) {

	}

	public void saveBuildStateToNBT(NBTTagCompound nbt, IBuildingItemsProvider builder) {
		NBTTagList clearList = new NBTTagList();

		for (BlockIndex loc : clearedLocations) {
			NBTTagCompound cpt = new NBTTagCompound();
			loc.writeTo(cpt);
			clearList.appendTag(cpt);
		}

		nbt.setTag("clearList", clearList);

		NBTTagList builtList = new NBTTagList();

		for (BlockIndex loc : builtLocations) {
			NBTTagCompound cpt = new NBTTagCompound();
			loc.writeTo(cpt);
			builtList.appendTag(cpt);
		}

		nbt.setTag("builtList", builtList);

		NBTTagList buildingList = new NBTTagList();

		for (BuildingItem item : builder.getBuilders()) {
			NBTTagCompound sub = new NBTTagCompound();
			item.writeToNBT(sub);
			buildingList.appendTag(sub);
		}

		nbt.setTag("buildersInAction", buildingList);
	}

	public void loadBuildStateToNBT(NBTTagCompound nbt, IBuildingItemsProvider builder) {
		NBTTagList clearList = nbt.getTagList("clearList", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < clearList.tagCount(); ++i) {
			NBTTagCompound cpt = clearList.getCompoundTagAt(i);

			clearedLocations.add (new BlockIndex(cpt));
		}

		NBTTagList builtList = nbt.getTagList("builtList", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < builtList.tagCount(); ++i) {
			NBTTagCompound cpt = builtList.getCompoundTagAt(i);

			builtLocations.add (new BlockIndex(cpt));
		}

		NBTTagList buildingList = nbt
				.getTagList("buildersInAction",
						Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < buildingList.tagCount(); ++i) {
			BuildingItem item = new BuildingItem();

			try {
				item.readFromNBT(buildingList.getCompoundTagAt(i));
				item.context = getContext();
				builder.getBuilders().add(item);
			} catch (MappingNotFoundException e) {
				BCLog.logger.log(Level.WARN, "can't load building item", e);
			}
		}
	}

	protected boolean isBlockBreakCanceled(World world, int x, int y, int z) {
		if (!world.isAirBlock(x, y, z)) {
			BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(x, y, z, world, world.getBlock(x, y, z),
					world.getBlockMetadata(x, y, z),
					CoreProxy.proxy.getBuildCraftPlayer((WorldServer) world).get());
			MinecraftForge.EVENT_BUS.post(breakEvent);
			return breakEvent.isCanceled();
		}
		return false;
	}

	protected boolean isBlockPlaceCanceled(World world, int x, int y, int z, SchematicBlockBase schematic) {
		Block block = schematic instanceof SchematicBlock ? ((SchematicBlock) schematic).block : Blocks.stone;
		int meta = schematic instanceof SchematicBlock ? ((SchematicBlock) schematic).meta : 0;

		BlockEvent.PlaceEvent placeEvent = new BlockEvent.PlaceEvent(
				new BlockSnapshot(world, x, y, z, block, meta),
				Blocks.air,
				CoreProxy.proxy.getBuildCraftPlayer((WorldServer) world, x, y, z).get()
		);

		MinecraftForge.EVENT_BUS.post(placeEvent);
		return placeEvent.isCanceled();
	}
}
