package co.sblock.events.listeners.inventory;

import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

import co.sblock.Sblock;
import co.sblock.events.listeners.SblockListener;
import co.sblock.utilities.InventoryUtils;

/**
 * Listener for FurnaceSmeltEvents.
 * 
 * @author Jikoo
 */
public class FurnaceSmeltListener extends SblockListener {

	public FurnaceSmeltListener(Sblock plugin) {
		super(plugin);
	}

	/**
	 * EventHandler for FurnaceSmeltEvents.
	 * 
	 * @param event the FurnaceSmeltEvent
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onFurnaceSmelt(FurnaceSmeltEvent event) {

		if (!canSalvage(event.getSource().getType())) {
			return;
		}

		ItemStack result = getMainItem(event.getSource().getType());

		int amount = (int) (result.getAmount()
				// This cast is actually necessary, prevents mid-calculation rounding.
				* ((double) (event.getSource().getType().getMaxDurability() - event.getSource()
						.getDurability())) / event.getSource().getType().getMaxDurability());

		if (amount < 1) {
			amount = 1;
			result = new ItemStack(Material.COAL, 1);
		}

		result.setAmount(amount);

		event.setCancelled(true);
		Furnace furnace = (Furnace) event.getBlock().getState();
		ItemStack furnaceResult = furnace.getInventory().getResult();
		if (furnaceResult == null || furnaceResult.isSimilar(result)) {
			if (furnaceResult != null) {
				amount = furnaceResult.getAmount() + result.getAmount();
				if (amount > result.getMaxStackSize()) {
					amount -= result.getMaxStackSize();
					result.setAmount(result.getMaxStackSize());
					furnace.getWorld().dropItem(furnace.getLocation(), new ItemStack(result.getType(), amount));
				} else {
					result.setAmount(amount);
				}
			}
			furnace.getInventory().setResult(result);
		} else {
			furnace.getWorld().dropItemNaturally(furnace.getLocation(), result);
		}
		furnace.getInventory().setSmelting(InventoryUtils.decrement(furnace.getInventory().getSmelting(), 1));
		furnace.update(true);
	}

	private ItemStack getMainItem(Material material) {
		switch (material) {
		case DIAMOND_SPADE:
			return new ItemStack(Material.DIAMOND, 1);
		case DIAMOND_HOE:
		case DIAMOND_SWORD:
			return new ItemStack(Material.DIAMOND, 2);
		case DIAMOND_AXE:
		case DIAMOND_PICKAXE:
			return new ItemStack(Material.DIAMOND, 3);
		case DIAMOND_BOOTS:
			return new ItemStack(Material.DIAMOND, 4);
		case DIAMOND_HELMET:
			return new ItemStack(Material.DIAMOND, 5);
		case DIAMOND_LEGGINGS:
			return new ItemStack(Material.DIAMOND, 7);
		case DIAMOND_CHESTPLATE:
			return new ItemStack(Material.DIAMOND, 8);
		case GOLD_SPADE:
			return new ItemStack(Material.GOLD_INGOT, 1);
		case GOLD_HOE:
		case GOLD_SWORD:
			return new ItemStack(Material.GOLD_INGOT, 2);
		case GOLD_AXE:
		case GOLD_PICKAXE:
			return new ItemStack(Material.GOLD_INGOT, 3);
		case GOLD_BOOTS:
			return new ItemStack(Material.GOLD_INGOT, 4);
		case GOLD_HELMET:
			return new ItemStack(Material.GOLD_INGOT, 5);
		case GOLD_LEGGINGS:
			return new ItemStack(Material.GOLD_INGOT, 7);
		case GOLD_CHESTPLATE:
			return new ItemStack(Material.GOLD_INGOT, 8);
		case IRON_SPADE:
			return new ItemStack(Material.IRON_INGOT, 1);
		case IRON_HOE:
		case IRON_SWORD:
		case SHEARS:
			return new ItemStack(Material.IRON_INGOT, 2);
		case IRON_AXE:
		case IRON_PICKAXE:
			return new ItemStack(Material.IRON_INGOT, 3);
		case IRON_BOOTS:
			return new ItemStack(Material.IRON_INGOT, 4);
		case IRON_HELMET:
			return new ItemStack(Material.IRON_INGOT, 5);
		case IRON_LEGGINGS:
			return new ItemStack(Material.IRON_INGOT, 7);
		case IRON_CHESTPLATE:
			return new ItemStack(Material.IRON_INGOT, 8);
		default:
			return new ItemStack(Material.COAL);
		}
	}

	private boolean canSalvage(Material material) {
		switch (material) {
		case DIAMOND_AXE:
		case DIAMOND_HOE:
		case DIAMOND_PICKAXE:
		case DIAMOND_SPADE:
		case DIAMOND_SWORD:
		case DIAMOND_BOOTS:
		case DIAMOND_CHESTPLATE:
		case DIAMOND_HELMET:
		case DIAMOND_LEGGINGS:
		case GOLD_AXE:
		case GOLD_HOE:
		case GOLD_PICKAXE:
		case GOLD_SPADE:
		case GOLD_SWORD:
		case GOLD_BOOTS:
		case GOLD_CHESTPLATE:
		case GOLD_HELMET:
		case GOLD_LEGGINGS:
		case IRON_AXE:
		case IRON_HOE:
		case IRON_SPADE:
		case IRON_SWORD:
		case IRON_PICKAXE:
		case IRON_BOOTS:
		case IRON_CHESTPLATE:
		case IRON_HELMET:
		case IRON_LEGGINGS:
		case SHEARS:
			return true;
		default:
			return false;
		}
	}
}
