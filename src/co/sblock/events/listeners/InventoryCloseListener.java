package co.sblock.events.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import co.sblock.fx.FXManager;
import co.sblock.machines.MachineInventoryTracker;
import co.sblock.users.OnlineUser;
import co.sblock.users.UserManager;

/**
 * Listener for InventoryCloseEvents.
 * 
 * @author Jikoo
 */
public class InventoryCloseListener implements Listener {

	/**
	 * EventHandler for InventoryCloseEvents.
	 * 
	 * @param event the InventoryCloseEvent
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent event) {
		MachineInventoryTracker.getTracker().closeMachine(event);

		OnlineUser user = UserManager.getGuaranteedUser(event.getPlayer().getUniqueId()).getOnlineUser();
		user.removeAllEffects();
		FXManager.fullEffectsScan(user);
	}
}
