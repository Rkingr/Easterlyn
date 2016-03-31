package co.sblock.commands.utility;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableList;

import co.sblock.Sblock;
import co.sblock.chat.Language;
import co.sblock.commands.SblockCommand;
import co.sblock.users.User;
import co.sblock.users.Users;

/**
 * SblockCommand for manipulating commands sent on login.
 * 
 * @author Jikoo
 */
public class LoginCommandsCommand extends SblockCommand {

	private final Users users;

	public LoginCommandsCommand(Sblock plugin) {
		super(plugin, "onlogin");
		this.users = plugin.getModule(Users.class);
		this.addExtraPermission("more", "helper");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(getLang().getValue("command.general.noConsole"));
			return true;
		}
		if (args.length == 0) {
			sender.sendMessage(this.getUsage());
			return true;
		}

		Player player = (Player) sender;
		User user = users.getUser(player.getUniqueId());
		args[0] = args[0].toLowerCase();

		if (args[0].equals("list")) {
			List<String> commands = user.getLoginCommands();
			if (commands.isEmpty()) {
				sender.sendMessage(getLang().getValue("command.onlogin.error.list.noCommands"));
				return true;
			}
			for (int i = 0; i < commands.size(); i++) {
				sender.sendMessage(new StringBuilder().append(Language.getColor("emphasis.neutral")).append(i + 1)
						.append(": ").append(Language.getColor("neutral")).append(commands.get(i)).toString());
			}
			return true;
		}
		if (args[0].equals("delete")) {
			delete(player, user, args);
			return true;
		}
		if (args[0].equals("add")) {
			add(player, user, args);
			return true;
		}
		sender.sendMessage(this.getUsage());
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args)
			throws IllegalArgumentException {
		if (!(sender instanceof Player) || !sender.hasPermission(this.getPermission())
				|| args.length == 0) {
			return ImmutableList.of();
		}
		args[0] = args[0].toLowerCase();
		ArrayList<String> matches = new ArrayList<String>();
		if (args.length == 1) {
			if ("add".startsWith(args[0])) {
				matches.add("add");
			}
			if ("delete".startsWith(args[0])) {
				matches.add("delete");
			}
			if (matches.isEmpty() || "list".startsWith(args[0])) {
				matches.add("list");
			}
			return matches;
		}
		if (args.length > 2) {
			return matches;
		}
		if (args[1].equals("delete")) {
			matches.add("1");
		}
		return matches;
	}

	private void delete(Player player, User user, String[] args) {
		try {
			int line = Integer.parseInt(args[1]);
			ArrayList<String> commands = new ArrayList<>(user.getLoginCommands());
			if (line < 1 || commands.size() < line) {
				player.sendMessage(getLang().getValue("command.onlogin.delete.bounds")
						.replace("{COUNT}", String.valueOf(commands.size()))
						.replace("{PARAMETER}", args[1]));
				return;
			}
			String removed = commands.remove(line - 1);
			user.setLoginCommands(commands);
			player.sendMessage(getLang().getValue("command.onlogin.delete").replace("{PARAMETER}", removed));
			return;
		} catch (NumberFormatException e) {
			player.sendMessage(getLang().getValue("command.onlogin.delete.usage"));
			return;
		}
	}

	private void add(Player player, User user, String[] args) {
		ArrayList<String> commands = new ArrayList<String>(user.getLoginCommands());
		if (!player.hasPermission("sblock.command.onlogin.more") && commands.size() >= 2 || commands.size() > 9) {
			player.sendMessage(getLang().getValue("command.onlogin.error.add.maximum"));
			return;
		}
		if (args.length <= 1) {
			player.sendMessage(getLang().getValue("command.onlogin.error.add.usage"));
			return;
		}
		if (args[1].charAt(0) != '/' || args[1].equalsIgnoreCase("/me")) {
			player.sendMessage(getLang().getValue("command.onlogin.error.add.noChat"));
			return;
		}
		String command = StringUtils.join(args, ' ', 1, args.length);
		commands.add(command);
		user.setLoginCommands(commands);
		player.sendMessage(getLang().getValue("command.onlogin.add").replace("{PARAMETER}", command));
		return;
	}
}
