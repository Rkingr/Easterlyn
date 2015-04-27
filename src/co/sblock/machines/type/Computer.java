package co.sblock.machines.type;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

import co.sblock.machines.Machines;
import co.sblock.machines.utilities.Icon;
import co.sblock.machines.utilities.MachineType;
import co.sblock.users.OfflineUser;
import co.sblock.users.OnlineUser;
import co.sblock.users.Users;

/**
 * Computers for players! Inventory-based selection system.
 * 
 * @author Jikoo
 */
public class Computer extends Machine implements InventoryHolder {

	/**
	 * Creates a Computer. If virtual is true, computer is not to actually be built.
	 * 
	 * @see co.sblock.machines.type.Machine#Machine(Location, String)
	 */
	public Computer(Location l, String owner, boolean virtual) {
		super(l, owner);
		if (!virtual) {
			shape.addBlock(new Vector(0, 0, 0), new MaterialData(Material.JUKEBOX));
			this.blocks = shape.getBuildLocations(direction);
		}
	}

	/**
	 * Players can only have one computer, and servers cannot place them for the client.
	 * 
	 * @see co.sblock.machines.type.Machine#assemble()
	 */
	@Override
	public void assemble(BlockPlaceEvent event) {
		if (Machines.getInstance().hasComputer(event.getPlayer(), key)) {
			if (event.getPlayer().hasPermission("sblock.horrorterror")) {
				event.getPlayer().sendMessage("Bypassing Computer cap. You devilish admin you.");
				return;
			}
			event.setCancelled(true);
			event.getBlock().setType(Material.AIR);
			event.getPlayer().sendMessage(ChatColor.RED + "You can only have one Computer placed!");
			this.assemblyFailed();
			return;
		}
		super.assemble(event);
	}

	/**
	 * @see co.sblock.machines.type.Machine#getType()
	 */
	@Override
	public MachineType getType() {
		return MachineType.COMPUTER;
	}

	/**
	 * @see co.sblock.machines.type.Machine#handleClick(InventoryClickEvent)
	 */
	@Override
	public boolean handleClick(InventoryClickEvent event) {
		if (!event.getWhoClicked().getUniqueId().toString().equals(this.owner)
				&& !event.getWhoClicked().hasPermission("sblock.denizen")) {
			event.setResult(Result.DENY);
			return true;
		}
		if (event.getCurrentItem() == null) {
			event.setResult(Result.DENY);
			return true;
		}
		event.setResult(Result.DENY);
		for (Icon ico : Icon.values()) {
			if (event.getCurrentItem().equals(ico.getIcon())) {
				switch (ico) {
				case BACK:
					event.getWhoClicked().openInventory(getInventory(Users.getGuaranteedUser(event.getWhoClicked().getUniqueId())));
					break;
				case BOONDOLLAR_SHOP:
					// Keiko, shop name is all you, set to LOHACSE for now
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bossshop open LOHACSE " + event.getWhoClicked().getName());
					break;
				case SBURBCLIENT:
					// if gamestate != none
				case PESTERCHUM:
					break;
				case SBURBSERVER:
					event.getWhoClicked().openInventory(getServerConfirmation());
					break;
				case CONFIRM:
					OfflineUser offUser = Users.getGuaranteedUser(event.getWhoClicked().getUniqueId());
					if (!(offUser instanceof OnlineUser)) {
						((Player) event.getWhoClicked()).sendMessage(
								ChatColor.RED + "Your data appears to not have loaded properly. Please relog.");
						break;
					}
					OnlineUser onUser = (OnlineUser) offUser;
					// All checks for starting server mode handled inside startServerMode()
					if (onUser.isServer()) {
						onUser.stopServerMode();
					} else {
						onUser.startServerMode();
					}
				default:
					break;
				}
				break;
			}
		}
		return true;
	}

	/**
	 * @see co.sblock.machines.type.Machine#handleInteract(PlayerInteractEvent)
	 */
	@Override
	public boolean handleInteract(PlayerInteractEvent event) {
		if (super.handleInteract(event)) {
			return true;
		}
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return true;
		}
		if (!event.getPlayer().getUniqueId().toString().equals(this.owner)) {
			if (event.getPlayer().hasPermission("sblock.denizen")) {
				event.getPlayer().sendMessage("Allowing admin override for interaction with Computer.");
			} else {
				return true;
			}
		}
		if (event.getMaterial().name().contains("RECORD")) { // prevent non-program Icons from being registered
			event.setCancelled(true);
			Icon ico = Icon.getIcon(event.getItem());
			if (ico != null) {
				// TODO Do not decrement in creative, decrement instead of setting null, do not allow reinstallation
				event.getPlayer().sendMessage(ChatColor.GREEN + "Installed "
						+ event.getItem().getItemMeta().getDisplayName() + ChatColor.GREEN + "!");
				event.getPlayer().setItemInHand(null);
				OfflineUser u = Users.getGuaranteedUser(event.getPlayer().getUniqueId());
				u.addProgram(ico.getProgramID());
				return true;
			}
		}
		if (event.getPlayer().isSneaking()) {
			return false;
		}
		event.getPlayer().openInventory(getInventory(Users.getGuaranteedUser(event.getPlayer().getUniqueId())));
		return true;
	}

	@Override
	public Inventory getInventory() {
		return null;
	}

	public Inventory getInventory(OfflineUser user) {
		Inventory i = Bukkit.createInventory(this, 9, user.getPlayerName() + "@sblock.co:~/");
		for (int i1 : user.getPrograms()) {
			i.addItem(Icon.getIcon(i1).getIcon());
		}
		if (i.firstEmpty() == 9) {
			user.getPlayer().sendMessage(ChatColor.RED + "You do not have any programs installed!");
		}
		return i;
	}

	/**
	 * Create a confirmation screen prior to entering server mode.
	 * 
	 * @return the Inventory created
	 */
	private Inventory getServerConfirmation() {
		Inventory i = Bukkit.createInventory(this, 9, "~/Verify?initialize=SburbServer");
		i.setItem(0, Icon.CONFIRM.getIcon());
		i.setItem(i.getSize() - 1, Icon.BACK.getIcon());
		return i;
	}
}
