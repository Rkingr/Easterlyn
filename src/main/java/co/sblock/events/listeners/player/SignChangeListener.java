package co.sblock.events.listeners.player;

import co.sblock.Sblock;
import co.sblock.chat.Chat;
import co.sblock.events.listeners.SblockListener;
import co.sblock.utilities.PermissionUtils;
import co.sblock.utilities.TextUtils;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;

import net.md_5.bungee.api.ChatColor;

/**
 * Listener for SignChangeEvents.
 * 
 * @author Jikoo
 */
public class SignChangeListener extends SblockListener {

	private final Chat chat;

	public SignChangeListener(Sblock plugin) {
		super(plugin);
		this.chat = plugin.getModule(Chat.class);

		PermissionUtils.addParent("sblock.sign.unlogged", "sblock.spam");
		PermissionUtils.addParent("sblock.sign.unlogged", "sblock.felt");
	}

	/**
	 * The event handler for SignChangeEvents.
	 * <p>
	 * Allows signs to be colored using &codes.
	 * 
	 * @param event the SignChangeEvent
	 */
	@EventHandler(ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {

		// Automatically flag players with bypass as posting non-empty signs to skip empty checks
		boolean empty = !event.getPlayer().hasPermission("sblock.sign.unlogged");

		for (int i = 0; i < event.getLines().length; i++) {
			event.setLine(i, ChatColor.translateAlternateColorCodes('&', event.getLine(i)));
			if (empty && !TextUtils.appearsEmpty(event.getLine(i))) {
				empty = false;
			}
		}

		if (empty || event.getPlayer().hasPermission("sblock.sign.unlogged")) {
			return;
		}

		Block block = event.getBlock();

		StringBuilder msg = new StringBuilder().append(ChatColor.GRAY)
				.append(block.getWorld().getName()).append(' ').append(block.getX()).append("x, ")
				.append(block.getY()).append("y, ").append(block.getZ()).append("z\n");
		for (String line : event.getLines()) {
			if (!TextUtils.appearsEmpty(line)) {
				msg.append(line).append(ChatColor.GRAY).append('\n');
			}
		}
		msg.delete(msg.length() - 3, msg.length());

		if (chat.testForMute(event.getPlayer(), msg.toString(), "#sign")) {
			event.setCancelled(true);
		}
	}
}
