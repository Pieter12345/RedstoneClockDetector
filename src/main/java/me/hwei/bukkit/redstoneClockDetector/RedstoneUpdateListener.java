package me.hwei.bukkit.redstoneClockDetector;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;

public class RedstoneUpdateListener implements Listener {
	
	// Variables & Constants.
	private HashMap<Location, Integer> redstoneActivityTable = null;
	private Plugin plugin;
	
	// Constructor.
	public RedstoneUpdateListener(Plugin plugin) {
		this.plugin = plugin;
	}
	
	public void startListener() {
		this.redstoneActivityTable = new HashMap<Location, Integer>();
		this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
	}
	
	public void stopListener() {
		this.redstoneActivityTable = null;
		HandlerList.unregisterAll(this);
	}
	
	public HashMap<Location, Integer> getRedstoneActivityTable() {
		return this.redstoneActivityTable;
	}
	
	public void setRedstoneActivityTable(HashMap<Location, Integer> redstoneActivityTable) {
		this.redstoneActivityTable = redstoneActivityTable;
	}
	
	
	/*
	 * Listeners and related methods.
	 */
	
	// Listen to default redstone component changes.
	@EventHandler(priority = EventPriority.MONITOR)
	public void onRedstoneUpdate(BlockRedstoneEvent event) {
		if(event.getOldCurrent() == event.getNewCurrent()) {
			return;
		}
		this.addRedstoneActivity(event.getBlock().getLocation());
		
	}
	
	// Listen to comparator and observer changes.
	// Note: event.getChangedType() returns the type of the cause block, not the block it fires for. event.getBlock() returns the block it fires for.
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPhysicsUpdate(BlockPhysicsEvent event) {
		
		// Handle Observer blocks.
		if(event.getChangedType().equals(Material.OBSERVER)) {
			this.addRedstoneActivity(event.getBlock().getLocation());
			return;
		}
		
		// Return if the cause block isn't a block or if the cause block is air.
		if(!event.getChangedType().isBlock() || event.getChangedType().equals(Material.AIR)) {
			return;
		}
		
		// Check if the block that got updated by this event should be logged.
		switch(event.getBlock().getType()) {
		case REDSTONE_COMPARATOR: // This block does not actually exist anymore.
		case REDSTONE_COMPARATOR_ON: // This block does not actually exist anymore.
		case REDSTONE_COMPARATOR_OFF: { // All comparators are of this type.
//			byte rawData = event.getBlock().getData();
//			BlockFace frontInputFace = null;
//			switch(rawData & 3) {
//			case 0: {
//				frontInputFace = BlockFace.SOUTH;
//				break;
//			}
//			case 1: {
//				frontInputFace = BlockFace.WEST;
//				break;
//			}
//			case 2: {
//				frontInputFace = BlockFace.NORTH;
//				break;
//			}
//			case 3: {
//				frontInputFace = BlockFace.EAST;
//				break;
//			}
//			}
//			BlockFace outputFace = frontInputFace.getOppositeFace();
//			int frontInputPower = event.getBlock().getBlockPower(frontInputFace); // TODO - Not accurate when powered from redstone in a non-straight line.
//			int sideInputPower = 0;
//			for(BlockFace face : new BlockFace[] {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST}) {
//				if(!face.equals(frontInputFace) && !face.equals(outputFace)) {
//					int power = event.getBlock().getBlockPower(face);
//					sideInputPower = Math.max(sideInputPower, power);
//				}
//			}
////			int outputPower;
////			if((rawData & 4) == rawData) { // If in subtraction mode.
////				outputPower = Math.max(frontInputPower - sideInputPower, 0);
////			} else { // If in comparator mode.
////				outputPower = (frontInputPower >= sideInputPower ? frontInputPower : 0);
////			}
//			int expectedOutputPower = event.getBlock().getRelative(outputFace).getBlockPower(frontInputFace); // TODO - This returns 15 a lot..
////			if(expectedOutputPower != outputPower) { // If the output power has changed.
////				Bukkit.getServer().broadcastMessage("Comparator changed power from "
////						+ expectedOutputPower + " to " + outputPower);
////			}
//			
//			int nmsPower = getComparatorOutputPower(
//					event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ(), event.getBlock().getWorld());
//			
//			Bukkit.getServer().broadcastMessage("Comparator updated with nmsPower=" + nmsPower
//					+ ", side=" + sideInputPower + ", front=" + frontInputPower + ".");
			
			this.addRedstoneActivity(event.getBlock().getLocation());
			return;
		}
		default: {
			return;
		}
		}
	}
	
//	/**
//	 * getComparatorOutputPower method.
//	 * Gets the output power of a comparator at the given location through NMS code.
//	 * @param x
//	 * @param y
//	 * @param z
//	 * @param world
//	 * @return The output power of the comparator at the given location or -1 if there was no comparator or an error has occured in the NMS code.
//	 */
//	private static int getComparatorOutputPower(int x, int y, int z, World world) {
//		// This code is ran using Reflection below, it returns the power of a comparator at the given BlockPosition:
//		// org.bukkit.Bukkit.getWorld('redstone').getHandle().getTileEntity(net.minecraft.server.v1_8_R3.BlockPosition.newInstance(29, 21, 176)).b()
//		int nmsPower = -1;
//		ClassLoader classLoader = RCDPlugin.class.getClassLoader();
//		try {
//			Class<?> craftWorldClass = classLoader.loadClass("org.bukkit.craftbukkit.v1_8_R3.CraftWorld");
//			Method getHandleMethod = craftWorldClass.getDeclaredMethod("getHandle");
//			Object worldServerInstance = getHandleMethod.invoke(world);
//			
//			Class<?> worldServerClass = classLoader.loadClass("net.minecraft.server.v1_8_R3.WorldServer");
//			Class<?> blockPositionClass = classLoader.loadClass("net.minecraft.server.v1_8_R3.BlockPosition");
//			Constructor<?> blockPositionConstructor = blockPositionClass.getDeclaredConstructor(int.class, int.class, int.class);
//			Object blockPositionObject = blockPositionConstructor.newInstance(x, y, z);
//			Method getTileEntityMethod = worldServerClass.getDeclaredMethod("getTileEntity", blockPositionClass);
//			Object tileEntity = getTileEntityMethod.invoke(worldServerInstance, blockPositionObject);
//			
//			Class<?> tileEntityComparatorClass = classLoader.loadClass("net.minecraft.server.v1_8_R3.TileEntityComparator");
//			if(tileEntity != null && tileEntityComparatorClass.isAssignableFrom(tileEntity.getClass())) {
//				Method bMethod = tileEntityComparatorClass.getDeclaredMethod("b");
//				nmsPower = (Integer) bMethod.invoke(tileEntity);
//			}
//		} catch (Exception e) {
//			System.out.println("Something went wrong in the NMS code in RedstoneClockDetector. Here's the stacktrace:");
//			e.printStackTrace();
//		}
//		return nmsPower;
//	}
	
	// Listen to hopper, dropper, chest, dispenser, etc inventory move events.
	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryMoveItem(InventoryMoveItemEvent event) {
		if(event.getSource().getHolder() == null || event.isCancelled()) {
			return;
		}
		InventoryHolder holder = event.getSource().getHolder();
		InventoryHolder target = event.getDestination().getHolder();
		if(target instanceof Hopper) {
			if(((Hopper) target).getBlock().getBlockPower() != 0) {
				return; // Hopper is locked. This might never happen.
			}
		} else if(target instanceof BrewingStand || target instanceof Furnace) {
			return; // This is easier than checking which items are allowed in a BrewingStand/Furnace. For clock detection, this isn't nessesary either.
		}
		Location loc;
		if(holder instanceof BlockState) {
			loc = ((BlockState) holder).getLocation();
		} else if(holder instanceof Entity) { // This catches minecarts and probably more.
			loc = ((Entity) holder).getLocation();
		} else {
			return;
		}
		
		// Check if there is space for the item in the other inventory.
		Inventory dest = event.getDestination();
		ItemStack items = event.getItem();
		boolean moveCanHappen = false;
		if(dest.firstEmpty() == -1) { // If the inventory is full.
			for(ItemStack stack : dest.getContents()) {
				
				// Get the MaterialData (type and data) of the stacks.
				MaterialData stackMaterial = stack.getData();
				MaterialData itemsMaterial = items.getData();
				
				// Check if either the MaterialData matches, or if there is no MaterialData and the type matches.
				boolean materialsMatch = (stackMaterial != null && itemsMaterial != null && stackMaterial.equals(itemsMaterial))
						|| (stackMaterial == null && itemsMaterial == null && stack.getType().equals(items.getType()));
				if(materialsMatch && stack.getAmount() + items.getAmount() <= stack.getMaxStackSize()) {
					moveCanHappen = true; // The stack can be added to an existing stack.
					break;
				}
			}
		} else {
			moveCanHappen = true; // There is a free inventory space.
		}
		
		if(moveCanHappen) {
			this.addRedstoneActivity(loc);
		}
	}
	
	// Listen to piston events.
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		this.addRedstoneActivity(event.getBlock().getLocation());
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		this.addRedstoneActivity(event.getBlock().getLocation());
	}
	
	private void addRedstoneActivity(Location loc) {
		int count = 1;
		if(this.redstoneActivityTable.containsKey(loc)) {
			count += this.redstoneActivityTable.get(loc);
//			this.debug("Adding to existing location at: ("
//					+ loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ") " + count);
		}
//		else {
//			this.debug("Adding new location at: ("
//					+ loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
//		}
		this.redstoneActivityTable.put(loc, count);
	}
	
//	// TODO - Remove this temporary debug.
//	private void debug(String message) {
//		this.getServer().broadcastMessage("[DEBUG] " + message);
//	}
}
