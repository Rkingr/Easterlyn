package co.sblock.Sblock.Machines.Type;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import co.sblock.Sblock.Machines.MachineModule;
import co.sblock.Sblock.UserData.SblockUser;

/**
 * @author Jikoo
 */
public class Computer extends Machine implements InventoryHolder {

	/**
	 * @see co.sblock.Sblock.Machines.Type.Machine#Machine(Location, String)
	 */
	public Computer(Location l, String data) {
		super(l, data);
	}

	/**
	 * @see co.sblock.Sblock.Machines.Type.Machine#assemble()
	 */
	public void assemble(BlockPlaceEvent event) {
		if (MachineModule.getInstance().getManager().hasComputer(event.getPlayer(), l)) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "You can only have one Computer!");
			event.getPlayer().setItemInHand(null);
			MachineModule.getInstance().getManager().removeMachineListing(l);
		}
	}

	/**
	 * @see co.sblock.Sblock.Machines.Type.Machine#meetsAdditionalBreakConditions(BlockBreakEvent)
	 */
	public boolean meetsAdditionalBreakConditions(BlockBreakEvent event) {
		return event.getPlayer().getName().equals(getData());
	}

	/**
	 * @see co.sblock.Sblock.Machines.Type.Machine#getLocations()
	 */
	public Set<Location> getLocations() {
		return new HashSet<Location>();
	}

	/**
	 * @see co.sblock.Sblock.Machines.Type.Machine#getType()
	 */
	public MachineType getType() {
		return MachineType.COMPUTER;
	}

	/**
	 * @see co.sblock.Sblock.Machines.Type.Machine#handleClick(InventoryClickEvent)
	 */
	public boolean handleClick(InventoryClickEvent event) {
		// Adam ensure upper inventory is clicked icon
		if (event.getCurrentItem() == null) {
			event.setResult(Result.DENY);
			return true;
		}
		for (Icon ico : Icon.values()) {
			if (event.getCurrentItem().equals(ico.getIcon())) {
				switch (ico) {
				case PESTERCHUM:
					break;
				case SBURBBETACLIENT:
					event.getWhoClicked().openInventory(getClientInventory());
					break;
				case SBURBBETASERVER:
					break;
				case BACK:
					event.getWhoClicked().openInventory(getInventory());
					break;
				default:
					break;
				}
				break;
			}
		}
		event.setResult(Result.DENY);
		return true;
	}

	/**
	 * @see co.sblock.Sblock.Machines.Type.Machine#handleInteract(PlayerInteractEvent)
	 */
	public boolean handleInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return true;
		}
		if (event.getMaterial().name().contains("RECORD")) {
			Icon ico = Icon.getIcon(event.getMaterial());
			if (ico != null) {
				event.setCancelled(true);
				event.getPlayer().setItemInHand(null);
				event.getPlayer().sendMessage(ChatColor.GREEN + "Installed " + ico + ChatColor.GREEN + "!");
				SblockUser u = SblockUser.getUser(event.getPlayer().getName());
				u.addProgram(ico.getProgramID());
				return true;
			} else {
				event.getPlayer().openInventory(getInventory());
			}
		}
		event.getPlayer().openInventory(getInventory());
		return true;
	}

	/**
	 * @see co.sblock.Sblock.Machines.Type.Machine#getFacingDirection()
	 */
	@Override
	public Direction getFacingDirection() {
		return Direction.NORTH;
	}

	/**
	 * @see org.bukkit.inventory.InventoryHolder#getInventory()
	 */
	@Override
	public Inventory getInventory() {
		SblockUser u = SblockUser.getUser(getData());
		Inventory i = Bukkit.createInventory(this, 9, getData() + "@sblock.co:~/");
		if (u == null) {
			return i;
		}
		for (int i1 : u.getPrograms()) {
			i.addItem(Icon.getIcon(i1).getIcon());
		}
		if (i.firstEmpty() == 9) {
			u.getPlayer().sendMessage(ChatColor.RED + "You do not have any programs installed!");
		}
		return i;
	}

	private Inventory getClientInventory() {
		Inventory i = Bukkit.createInventory(this, 18, "~/SburbClient");
		i.setItem(i.getSize() - 1, Icon.BACK.getIcon());
		return i;
	}
}
