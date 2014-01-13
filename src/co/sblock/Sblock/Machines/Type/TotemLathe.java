package co.sblock.Sblock.Machines.Type;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.libs.jline.internal.Log;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import co.sblock.Sblock.Utilities.Captcha.Captcha;
import co.sblock.Sblock.Utilities.Captcha.CruxiteDowel;

/**
 * 
 * @author Dublek
 */
public class TotemLathe extends Machine implements InventoryHolder	{

	public TotemLathe(Location l, String data, Direction d) {
		super(l, data, d);
		ItemStack is = new ItemStack(Material.QUARTZ_BLOCK);
		is.setDurability((short) 2);
		shape.addBlock(new Vector(0, 1, 0), is);
		shape.addBlock(new Vector(0, 2, 0), is);
		is = new ItemStack(Material.QUARTZ_STAIRS);
		is.setDurability(d.getRelativeDirection(Direction.WEST).getUpperStairByte());
		shape.addBlock(new Vector(1, 0, 0), is);
		is = new ItemStack(Material.STEP);
		is.setDurability((short) 7);
		shape.addBlock(new Vector(0, 3, 0), is);
		shape.addBlock(new Vector(1, 3, 0), is);
		shape.addBlock(new Vector(2, 3, 0), is);
		shape.addBlock(new Vector(3, 3, 0), is);
		is = new ItemStack(Material.STEP);
		is.setDurability((short) 15);
		shape.addBlock(new Vector(2, 0, 0), is);
		shape.addBlock(new Vector(3, 0, 0), is);
		is = new ItemStack(Material.FURNACE);
		is.setDurability(d.getRelativeDirection(Direction.WEST).getChestByte());
		shape.addBlock(new Vector(1, 1, 0), is);
		is = new ItemStack(Material.DAYLIGHT_DETECTOR);
		shape.addBlock(new Vector(2, 1, 0), is);
		is = new ItemStack(Material.ANVIL);
		is.setDurability((short) (d.getDirByte() % 2 + 2));
		shape.addBlock(new Vector(3, 1, 0), is);
		is = new ItemStack(Material.HOPPER);
		shape.addBlock(new Vector(3, 2, 0), is);
		blocks = shape.getBuildLocations(getFacingDirection());
	}

	@Override
	public MachineType getType() {
		return MachineType.TOTEM_LATHE;
	}

	@Override
	public boolean handleInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			Log.debug("Not a right click");
			return true;
		}
		Log.debug("Right click, time to create inventory");
		event.getPlayer().openInventory(getInventory());
		return true;
	}
	
	public boolean handleClick(InventoryClickEvent event)	{
		if (event.getCurrentItem() == null) {
			event.setResult(Result.DENY);
			return true;
		}
		FurnaceInventory fi = (FurnaceInventory) event.getInventory();
		if(CruxiteDowel.isDowel(fi.getFuel()) && Captcha.isPunchCard(fi.getSmelting()))	{
			//ADAM do the smelty thing
		}
		event.setResult(Result.DENY);
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void postAssemble() {
		this.l.getBlock().setType(Material.QUARTZ_BLOCK);
		this.l.getBlock().setData((byte) 2, false);		
	}

	@Override
	public Inventory getInventory() {
		Inventory i = Bukkit.createInventory(this, InventoryType.FURNACE);
		Log.debug("Created inventory");
		return i;
	}

}
