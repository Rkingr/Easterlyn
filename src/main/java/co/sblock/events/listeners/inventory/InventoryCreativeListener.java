package co.sblock.events.listeners.inventory;

import co.sblock.Sblock;
import co.sblock.events.Events;
import co.sblock.events.listeners.SblockListener;
import co.sblock.utilities.InventoryUtils;
import co.sblock.utilities.PermissionUtils;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for InventoryCreativeEvents. Used to clean input items from creative clients, preventing
 * server/client crashes.
 * 
 * @author Jikoo
 */
public class InventoryCreativeListener extends SblockListener {

	private final Events events;

	public InventoryCreativeListener(Sblock plugin) {
		super(plugin);
		this.events = plugin.getModule(Events.class);

		PermissionUtils.addParent("sblock.events.creative.unfiltered", "sblock.felt");
	}

	/**
	 * EventHandler for InventoryCreativeEvents. Triggered when a creative client spawns an item.
	 * <p>
	 * The click fired is always left, cursor is always the item being created/placed, current is
	 * always the item being replaced. This holds true even for item drops, pick block, etc.
	 * 
	 * @param event the InventoryCreativeEvent
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInventoryCreative(InventoryCreativeEvent event) {
		if (event.getWhoClicked().hasPermission("sblock.events.creative.unfiltered")) {
			return;
		}

		if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) {
			return;
		}

		// Blacklist
		if (events.getCreativeBlacklist().contains(event.getCursor().getType())) {
			event.setCancelled(true);
			return;
		}

		ItemStack cleanedItem = InventoryUtils.cleanNBT(event.getCursor());

		if (cleanedItem == event.getCursor()) {
			return;
		}

		// No invalid durabilities.
		if (cleanedItem.getDurability() < 0) {
			cleanedItem.setDurability((short) 0);
		}

		// No overstacking, no negative amounts
		if (cleanedItem.getAmount() > cleanedItem.getMaxStackSize()) {
			cleanedItem.setAmount(cleanedItem.getMaxStackSize());
		} else if (cleanedItem.getAmount() < 1) {
			cleanedItem.setAmount(1);
		}

		event.setCursor(cleanedItem);
	}
}
