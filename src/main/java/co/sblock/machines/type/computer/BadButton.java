package co.sblock.machines.type.computer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Wool;

import co.sblock.machines.Machines;
import co.sblock.machines.type.Elevator;
import co.sblock.machines.type.Machine;

/**
 * Counterpart to GoodButton.
 * 
 * @author Jikoo
 */
public class BadButton extends Program {

	private final ItemStack icon;

	public BadButton(Machines machines) {
		super(machines);

		Wool wool = new Wool(Material.STAINED_GLASS_PANE);
		wool.setColor(DyeColor.RED);
		icon = wool.toItemStack();
		icon.setAmount(1);
	}

	@Override
	protected void execute(Player player, ItemStack clicked, boolean verified) {
		if (!clicked.hasItemMeta()) {
			return;
		}

		ItemMeta clickedMeta = clicked.getItemMeta();
		InventoryView view = player.getOpenInventory();
		Inventory top = view.getTopInventory();

		if (!clickedMeta.hasLore()) {
			switch (ChatColor.stripColor(clickedMeta.getDisplayName())) {
			case "Decrease Boost":
				if (top.getLocation() == null) {
					break;
				}
				Pair<Machine, ConfigurationSection> machine = getMachines().getMachineByLocation(top.getLocation());
				if (!(machine.getLeft() instanceof Elevator)) {
					break;
				}
				Elevator elevator = (Elevator) machine.getLeft();
				int amount = elevator.adjustBlockBoost(machine.getRight(), -1);
				ItemStack gauge = top.getItem(4);
				if (gauge != null) {
					gauge.setAmount(amount);
					top.setItem(4, gauge);
				}
				break;
			default:
				break;
			}
			return;
		}

		List<String> lore = clickedMeta.getLore();
		if (lore.size() == 0) {
			return;
		}

		switch (ChatColor.stripColor(lore.get(0))) {
		default:
			break;
		}
	}

	@Override
	public ItemStack getIcon() {
		return icon;
	}

	public ItemStack getIconFor(String... data) {
		ItemStack localIcon = this.icon.clone();
		if (data.length == 0) {
			return localIcon;
		}

		ItemMeta localMeta = localIcon.getItemMeta();
		localMeta.setDisplayName(data[0]);

		if (data.length == 1) {
			localIcon.setItemMeta(localMeta);
			return localIcon;
		}

		List<String> lore = new ArrayList<>(data.length - 1);
		for (int i = 1; i < data.length; ++i) {
			lore.add(data[i]);
		}
		localMeta.setLore(lore);

		localIcon.setItemMeta(localMeta);
		return localIcon;
	}

	@Override
	public ItemStack getInstaller() {
		return null;
	}

}
