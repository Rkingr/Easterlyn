package co.sblock.events.listeners.inventory;

import org.apache.commons.lang3.tuple.Pair;

import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;

import co.sblock.Sblock;
import co.sblock.events.listeners.SblockListener;
import co.sblock.machines.Machines;
import co.sblock.machines.type.Machine;

/**
 * Listener for InventoryMoveItemEvents.
 * 
 * @author Jikoo
 */
public class InventoryMoveItemListener extends SblockListener {

	private final Machines machines;

	public InventoryMoveItemListener(Sblock plugin) {
		super(plugin);
		this.machines = plugin.getModule(Machines.class);
	}

	/**
	 * EventHandler for when hoppers move items.
	 * 
	 * @param event the InventoryMoveItemEvent
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInventoryMoveItem(InventoryMoveItemEvent event) {
		InventoryHolder ih = event.getDestination().getHolder();
		// For now, sending inv is not checked, as no machines require it.
		if (ih != null && ih instanceof BlockState) {
			Pair<Machine, ConfigurationSection> pair = machines.getMachineByBlock(((BlockState) ih).getBlock());
			if (pair != null) {
				event.setCancelled(pair.getLeft().handleHopperMoveItem(event, pair.getRight()));
			}
		}
	}
}
