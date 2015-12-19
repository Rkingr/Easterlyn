package co.sblock.events.listeners.player;

import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import co.sblock.Sblock;
import co.sblock.chat.Chat;
import co.sblock.chat.Color;
import co.sblock.commands.utility.OopsCommand;
import co.sblock.discord.Discord;
import co.sblock.discord.DiscordPlayer;
import co.sblock.events.listeners.SblockListener;
import co.sblock.micromodules.AwayFromKeyboard;
import co.sblock.micromodules.Spectators;

/**
 * Listener for PlayerCommandPreprocessEvents.
 * 
 * @author Jikoo
 */
public class CommandPreprocessListener extends SblockListener {

	private final AwayFromKeyboard afk;
	private final Chat chat;
	private final Discord discord;
	private final Spectators spectators;
	private final SimpleCommandMap map;

	public CommandPreprocessListener(Sblock plugin) {
		super(plugin);
		this.afk = plugin.getModule(AwayFromKeyboard.class);
		this.chat = plugin.getModule(Chat.class);
		this.discord = plugin.getModule(Discord.class);
		this.spectators = plugin.getModule(Spectators.class);
		this.map = plugin.getCommandMap();
	}

	/**
	 * EventHandler for PlayerCommandPreprocessEvents.
	 * 
	 * @param event the PlayerCommandPreprocessEvent
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		afk.extendActivity(event.getPlayer());

		int colon = event.getMessage().indexOf(':');
		int space = event.getMessage().indexOf(' ');
		if (!event.getPlayer().hasPermission("sblock.denizen") && 0 < colon && (colon < space || space < 0)) {
			event.setMessage("/" + event.getMessage().substring(colon + 1));
		}

		String command = event.getMessage().substring(1, space > 0 ? space : event.getMessage().length()).toLowerCase();
		Command cmd = map.getCommand(command);

		if (!event.getPlayer().hasPermission("sblock.felt") && cmd != null
				&& !discord.getConfig().getStringList("discord.command-log-blacklist").contains(cmd.getName())) {
			discord.logMessage(event.getPlayer().getName() + " issued command: " + event.getMessage());
		}

		if (((OopsCommand) map.getCommand("oops"))
				.handleFailedCommand(event.getPlayer(), command, space > 0
						? event.getMessage().substring(space + 1) : null)) {
			event.setCancelled(true);
			return;
		}

		if (cmd == null) {
			return;
		}

		if (spectators.isSpectator(event.getPlayer().getUniqueId())
				&& cmd.getName().equals("sethome")) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(Color.BAD + "You hear a fizzling noise as your spell fails.");
			return;
		}

		if (cmd.getName().equals("mail")) {
			if (space > 0 && event.getMessage().substring(space + 1).toLowerCase().startsWith("send")
					&& chat.testForMute(event.getPlayer())) {
				event.setCancelled(true);
			}
		} else if (cmd.getName().equals("prism")) {
			if (space > 0 && event.getMessage().substring(space + 1).toLowerCase().startsWith("undo")) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(Color.BAD + "Restore, don't undo.");
			}
		} else if (cmd.getName().equals("fly")) {
			if ((event.getPlayer().hasPermission("essentials.fly")
					|| event.getPlayer().hasPermission("essentials.*"))) {
				event.getPlayer().setFallDistance(0);
			}
		} else if (cmd.getName().equals("gc")) {
			if (!event.getPlayer().hasPermission("essentials.gc")
					&& !event.getPlayer().hasPermission("essentials.*")) {
				event.setMessage("/tps");
				command = "tps";
			}
		}

		if (event.getPlayer() instanceof DiscordPlayer) {
			if (!discord.getConfig().getStringList("discord.command-whitelist").contains(cmd.getName())) {
				event.getPlayer().sendMessage('/' + cmd.getName() + " isn't allowed from Discord, sorry!");
				event.setCancelled(true);
			}
		}

	}
}
