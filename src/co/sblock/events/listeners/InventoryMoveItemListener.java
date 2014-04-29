package co.sblock.events.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;

import co.sblock.machines.type.Machine;

/**
 * Listener for InventoryMoveItemEvents.
 * 
 * @author Jikoo
 */
public class InventoryMoveItemListener implements Listener {

	/**
	 * EventHandler for when hoppers move items.
	 * 
	 * @param event the InventoryMoveItemEvent
	 */
	@EventHandler(ignoreCancelled = true)
	public void onHopperMoveItem(InventoryMoveItemEvent event) {
		InventoryHolder ih = event.getDestination().getHolder();
		if (ih == null || !(ih instanceof Machine)) {
			return;
		}
		Machine m = (Machine) ih;
		if (m != null) {
			event.setCancelled(m.handleHopper(event));
		}
	}
}